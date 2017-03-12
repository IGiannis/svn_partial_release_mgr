package hudson.scm;

import com.trilead.ssh2.crypto.Base64;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.putty.PuTTYKey;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.dav.http.DefaultHTTPConnectionFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.PluginFactory;
import hudson.plugins.svn_partial_release_mgr.api.PluginService;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.model.AllIssueRevisionsInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.util.EditDistance;
import hudson.util.FormFieldValidator;
import hudson.util.IOException2;
import hudson.util.MultipartFormDataParser;
import hudson.util.Scrambler;
import hudson.util.Secret;
import jenkins.security.Roles;

/**
 * Subversion SCM.
 *
 * <h2>Plugin Developer Notes</h2>
 * <p>
 * Plugins that interact with Subversion can use {@link DescriptorImpl#createAuthenticationProvider()}
 * so that it can use the credentials (username, password, etc.) that the user entered for Hudson.
 * See the javadoc of this method for the precautions you need to take if you run Subversion operations
 * remotely on slaves.
 *
 * <h2>Implementation Notes</h2>
 * <p>
 * Because this instance refers to some other classes that are not necessarily
 * Java serializable (like {@link #browser}), remotable {@link FileCallable}s all
 * need to be declared as static inner classes.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionReleaseSCM extends SCM
    implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(SubversionReleaseSCM.class.getName());
  private static final Logger LOGGER = Logger.getLogger(SubversionReleaseSCM.class.getName());

  static {
    new Initializer();
  }

  private final JobConfigurationUserInput releaseInput;
  private final SubversionRepositoryBrowser browser;

  /**
   * the locations field is used to store all configured SVN locations (with
   * their local and remote part). Direct access to this filed should be
   * avoided and the getLocations() method should be used instead. This is
   * needed to make importing of old hudson-configurations possible as
   * getLocations() will check if the modules field has been set and import
   * the data.
   *
   * @since 1.91
   */
  private ModuleLocation location;

  public SubversionReleaseSCM(JobConfigurationUserInput releaseInput,
                              SubversionRepositoryBrowser browser) {
    this.releaseInput = releaseInput;
    this.location = releaseInput.getLocation();
    this.browser = browser;
  }

  /**
   * Creates {@link SVNClientManager}.
   *
   * <p>
   * This method must be executed on the slave where svn operations are performed.
   *
   * @param authProvider The value obtained from {@link DescriptorImpl#createAuthenticationProvider()}.
   *                     If the operation runs on slaves,
   *                     (and properly remoted, if the svn operations run on slaves.)
   */
  public static SVNClientManager createSvnClientManager(ISVNAuthenticationProvider authProvider) {
    ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
    sam.setAuthenticationProvider(authProvider);
    return SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), sam);
  }

  /**
   * Creates {@link SVNClientManager} for code running on the master.
   * <p>
   * CAUTION: this code only works when invoked on master. On slaves, use
   * {@link #createSvnClientManager(ISVNAuthenticationProvider)} and get {@link ISVNAuthenticationProvider}
   * from the master via remoting.
   */
  public static SVNClientManager createSvnClientManager() {
    return createSvnClientManager(DescriptorImpl.DESCRIPTOR.createAuthenticationProvider());
  }

  private static String getLastPathComponent(String s) {
    String[] tokens = s.split("/");
    return tokens[tokens.length - 1]; // return the last token
  }

  // #################### GETTERS Used in SubversionReleaseSCM\config.jelly ######################
  public String getTagName() {
    return releaseInput.getTagName();
  }

  /**
   * Returns the object that holds all the revisions from the tag and to the latest source
   *
   * @param workspaceRootPath the job workspace path
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  public AllIssueRevisionsInfo getIssueRevisionsInfo(String workspaceRootPath) throws IOException {
    return newPluginService().getAllRevisionsInfo(releaseInput, workspaceRootPath);
  }

  /**
   * Returns true if the tag has already been checked out
   *
   * @param workspaceRootPath the job workspace path
   * @return true if the tag has already been checked out
   */
  public boolean hasTagBackupSource(String workspaceRootPath) throws IOException {
    String backupTagSourceLocation = PluginUtil.getFullPathToTagBackupSource(
        workspaceRootPath,releaseInput.getTagName());
    return new File(backupTagSourceLocation).exists();
  }

  @Override
  public SubversionRepositoryBrowser getBrowser() {
    return browser;
  }

  public String getIssuePrefixes() {
    return StringUtils.defaultIfBlank(releaseInput.getIssuePrefixes(),
        Constants.DEFAULT_ISSUE_PREFIXES);
  }

  /**
   * Polling can happen on the master and does not require a workspace.
   */
  @Override
  public boolean requiresWorkspaceForPolling() {
    return false;
  }

  /**
   * list of all configured svn locations
   *
   * @since 1.91
   */
  public ModuleLocation getLocation() {
    return location;
  }

  @Override
  public boolean checkout(AbstractBuild<?, ?> build,
                          Launcher launcher,
                          FilePath workspace,
                          final BuildListener listener,
                          File changelogFile) throws IOException, InterruptedException {
    PluginService pluginService = newPluginService();
    AllIssueRevisionsInfo allIssueRevisionsInfo = getIssueRevisionsInfo(workspace.getRemote());
    pluginService.checkout(releaseInput, allIssueRevisionsInfo, build, launcher,
        workspace, listener, changelogFile, LOGGER);
    return true;
  }

  /**
   * New instance of the plugin implementation class
   *
   * @return the instance of the plugin implementation class
   */
  protected PluginService newPluginService() {
    return PluginFactory.getBean(PluginService.class);
  }

  public boolean pollChanges(AbstractProject project,
                             Launcher launcher,
                             FilePath workspace,
                             TaskListener listener) throws IOException,
      InterruptedException {
    AbstractBuild lastBuild = (AbstractBuild) project.getLastBuild();
    if (lastBuild == null) {
      listener.getLogger().println(
          "No existing build. Starting a new one");
      return true;
    }

    try {

      if (!repositoryLocationsExist(lastBuild, listener)) {
        // Disable this project, see issue #763

        listener.getLogger().println(
            "One or more repository locations do not exist anymore for "
                + project + ", project will be disabled.");
        project.makeDisabled(true);
        return false;
      }
    } catch (SVNException e) {
      e.printStackTrace(listener.error(e.getMessage()));
      return false;
    }

    Thread.sleep(60 * 1000);  // debug

    return false; // no change
  }

  public ChangeLogParser createChangeLogParser() {
    return new SubversionChangeLogParser();
  }

  public DescriptorImpl getDescriptor() {
    return DescriptorImpl.DESCRIPTOR;
  }

  public FilePath getModuleRoot(FilePath workspace) {
    if (location != null) {
      return workspace.child(location.local);
    }
    return workspace;
  }

  public FilePath[] getModuleRoots(FilePath workspace) {
    final ModuleLocation moduleLocation = getLocation();
    if (moduleLocation != null) {
      FilePath[] moduleRoots = new FilePath[1];
      moduleRoots[0] = workspace.child(moduleLocation.local);
      return moduleRoots;
    }
    return new FilePath[]{getModuleRoot(workspace)};
  }

  public boolean repositoryLocationsExist(AbstractBuild<?, ?> build,
                                          TaskListener listener) throws SVNException {

    PrintStream out = listener.getLogger();
    ModuleLocation l = getLocation();

    if (getDescriptor().checkRepositoryPath(l.getSVNURL()) == SVNNodeKind.NONE) {
      out.println("Location '" + l.remote + "' does not exist");

      ParametersAction params = build
          .getAction(ParametersAction.class);
      if (params != null) {
        out.println("Location could be expanded on build '" + build
            + "' parameters values:");

        for (ParameterValue paramValue : params) {
          out.println("  " + paramValue);
        }
      }
      return false;
    }

    return true;
  }

  @Override
  public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
                                                 Launcher launcher,
                                                 TaskListener listener) throws IOException,
      InterruptedException {
    return null; // We don't care about calculating revision, as it's input.
  }

  @Override
  protected PollingResult compareRemoteRevisionWith(
      AbstractProject<?, ?> project,
      Launcher launcher,
      FilePath workspace,
      TaskListener listener,
      SCMRevisionState baseline)
      throws IOException, InterruptedException {
    return null; // We don't care about polling SCM.
  }

  public static class DescriptorImpl extends SCMDescriptor<SubversionReleaseSCM> {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    static {
      new Initializer();
    }

    /**
     * SVN authentication realm to its associated credentials.
     */
    private final Map<String, Credential> credentials = new Hashtable<String, Credential>();
    /**
     * There's no point in exporting multiple {@link RemotableSVNAuthenticationProviderImpl} instances,
     * so let's just use one instance.
     */
    private transient final RemotableSVNAuthenticationProviderImpl
        remotableProvider = new RemotableSVNAuthenticationProviderImpl();

    private DescriptorImpl() {
      super(SubversionReleaseSCM.class, SubversionRepositoryBrowser.class);
      load();
    }

    protected DescriptorImpl(Class clazz,
                             Class<? extends RepositoryBrowser> repositoryBrowser) {
      super(clazz, repositoryBrowser);
    }

    public static String getRelativePath(SVNURL repoURL,
                                         SVNRepository repository) throws SVNException {
      String
          repoPath =
          repoURL.getPath().substring(repository.getRepositoryRoot(false).getPath().length());
      if (!repoPath.startsWith("/")) {
        repoPath = "/" + repoPath;
      }
      return repoPath;
    }

    public String getDisplayName() {
      return Constants.ACTION_NAME;
    }

    //TODO JSP MODIFIED to never use update
    @Override
    public SCM newInstance(StaplerRequest req,
                           JSONObject formData) throws FormException {
      JobConfigurationUserInput
          releaseInput =
          new JobConfigurationUserInput(req.getParameter("svn-r.location_remote"),
              req.getParameter("tagName"),
              req.getParameter("issuePrefixes"));
      return new SubversionReleaseSCM(releaseInput,
          RepositoryBrowsers
              .createInstance(SubversionRepositoryBrowser.class, req, formData, "browser"));
    }

    /**
     * Creates {@link ISVNAuthenticationProvider} backed by {@link #credentials}.
     * This method must be invoked on the master, but the returned object is remotable.
     *
     * <p>
     * Therefore, to access {@link ISVNAuthenticationProvider}, you need to call this method
     * on the master, then pass the object to the slave side, then call
     * {@link SubversionReleaseSCM#createSvnClientManager(ISVNAuthenticationProvider)} on the slave.
     *
     * @see SubversionReleaseSCM#createSvnClientManager(ISVNAuthenticationProvider)
     */
    public ISVNAuthenticationProvider createAuthenticationProvider() {
      return new SVNAuthenticationProviderImpl(remotableProvider);
    }

    /**
     * Submits the authentication info.
     *
     * This code is fairly ugly because of the way SVNKit handles credentials.
     */
    // TODO: stapler should do multipart/form-data handling
    public void doPostCredential(StaplerRequest req,
                                 StaplerResponse rsp) throws IOException, ServletException {
      Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

      MultipartFormDataParser parser = new MultipartFormDataParser(req);

      String url = parser.get("url");

      String kind = parser.get("kind");
      int idx = Arrays.asList("", "password", "publickey", "certificate").indexOf(kind);

      final String username = parser.get("username" + idx);
      final String password = parser.get("password" + idx);

      // SVNKit wants a key in a file
      final File keyFile;
      FileItem item = null;
      if (idx <= 1) {
        keyFile = null;
      } else {
        item = parser.getFileItem(kind.equals("publickey") ? "privateKey" : "certificate");
        keyFile = File.createTempFile("hudson", "key");
        if (item != null) {
          try {
            item.write(keyFile);
          } catch (Exception e) {
            throw new IOException2(e);
          }
          if (PuTTYKey.isPuTTYKeyFile(keyFile)) {
            // TODO: we need a passphrase support
            LOGGER.info("Converting " + keyFile + " from PuTTY format to OpenSSH format");
            new PuTTYKey(keyFile, null).toOpenSSH(keyFile);
          }
        }
      }

      // we'll record what credential we are trying here.
      StringWriter log = new StringWriter();
      final PrintWriter logWriter = new PrintWriter(log);
      final boolean[] authenticationAttemped = new boolean[1];
      final boolean[] authenticationAcknowled = new boolean[1];

      SVNRepository repository = null;
      try {
        // the way it works with SVNKit is that
        // 1) svnkit calls AuthenticationManager asking for a credential.
        //    this is when we can see the 'realm', which identifies the user domain.
        // 2) DefaultSVNAuthenticationManager returns the username and password we set below
        // 3) if the authentication is successful, svnkit calls back acknowledgeAuthentication
        //    (so we store the password info here)
        repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
        repository.setAuthenticationManager(
            new DefaultSVNAuthenticationManager(SVNWCUtil.getDefaultConfigurationDirectory(), true,
                username, password, keyFile, password) {
              Credential cred = null;

              @Override
              public SVNAuthentication getFirstAuthentication(String kind,
                                                              String realm,
                                                              SVNURL url) throws SVNException {
                authenticationAttemped[0] = true;
                if (kind.equals(ISVNAuthenticationManager.USERNAME))
                // when using svn+ssh, svnkit first asks for ISVNAuthenticationManager.SSH
                // authentication to connect via SSH, then calls this method one more time
                // to get the user name. Perhaps svn takes user name on its own, separate
                // from OS user name? In any case, we need to return the same user name.
                // I don't set the cred field here, so that the 1st credential for ssh
                // won't get clobbered.
                {
                  return new SVNUserNameAuthentication(username, false);
                }
                if (kind.equals(ISVNAuthenticationManager.PASSWORD)) {
                  logWriter.println("Passing user name " + username + " and password you entered");
                  cred = new PasswordCredential(username, password);
                }
                if (kind.equals(ISVNAuthenticationManager.SSH)) {
                  if (keyFile == null) {
                    logWriter.println(
                        "Passing user name " + username + " and password you entered to SSH");
                    cred = new PasswordCredential(username, password);
                  } else {
                    logWriter.println(
                        "Attempting a public key authentication with username " + username);
                    cred = new SshPublicKeyCredential(username, password, keyFile);
                  }
                }
                if (kind.equals(ISVNAuthenticationManager.SSL)) {
                  logWriter.println("Attempting an SSL client certificate authentcation");
                  cred = new SslClientCertificateCredential(keyFile, password);
                }

                if (cred == null) {
                  logWriter.println("Unknown authentication method: " + kind);
                  return null;
                }
                return cred.createSVNAuthentication(kind);
              }

              /**
               * Getting here means the authentication tried in {@link #getFirstAuthentication(String, String, SVNURL)}
               * didn't work.
               */
              @Override
              public SVNAuthentication getNextAuthentication(String kind,
                                                             String realm,
                                                             SVNURL url) throws SVNException {
                SVNErrorManager.authenticationFailed("Authentication failed for " + url, null);
                return null;
              }

              @Override
              public void acknowledgeAuthentication(boolean accepted,
                                                    String kind,
                                                    String realm,
                                                    SVNErrorMessage errorMessage,
                                                    SVNAuthentication authentication)
                  throws SVNException {
                authenticationAcknowled[0] = true;
                if (accepted) {
                  assert cred != null;
                  credentials.put(realm, cred);
                  save();
                } else {
                  logWriter.println("Failed to authenticate: " + errorMessage);
                  if (errorMessage.getCause() != null) {
                    errorMessage.getCause().printStackTrace(logWriter);
                  }
                }
                super
                    .acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
              }
            });
        repository.testConnection();

        if (!authenticationAttemped[0]) {
          logWriter.println("No authentication was attemped.");
          throw new SVNCancelException();
        }
        if (!authenticationAcknowled[0]) {
          logWriter.println("Authentication was not acknowledged.");
          throw new SVNCancelException();
        }

        rsp.sendRedirect("credentialOK");
      } catch (SVNException e) {
        logWriter.println("FAILED: " + e.getErrorMessage());
        req.setAttribute("message", log.toString());
        req.setAttribute("pre", true);
        req.setAttribute("exception", e);
        rsp.forward(Hudson.getInstance(), "error", req);
      } finally {
        if (keyFile != null) {
          keyFile.delete();
        }
        if (item != null) {
          item.delete();
        }
        if (repository != null) {
          repository.closeSession();
        }
      }
    }

    /**
     * validate the value for a remote (repository) location.
     */
    public void doSvnRemoteLocationCheck(final StaplerRequest req,
                                         StaplerResponse rsp) throws IOException, ServletException {
      // false==No permisison needed for basic check
      new FormFieldValidator(req, rsp, false) {
        protected void check() throws IOException, ServletException {
          // syntax check first
          String url = Util.nullify(request.getParameter("value"));
          if (url == null) {
            ok(); // not entered yet
            return;
          }

          // remove unneeded whitespaces
          url = url.trim();
          if (!Constants.URL_PATTERN.matcher(url).matches()) {
            errorWithMarkup("Invalid URL syntax. See "
                + "<a href=\"http://svnbook.red-bean.com/en/1.2/svn-book.html#svn.basic.in-action.wc.tbl-1\">this</a> "
                + "for information about valid URLs.");
            return;
          }

          // Test the connection only if we have admin permission
          if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
            ok();
          } else {
            try {
              SVNURL repoURL = SVNURL.parseURIDecoded(url);
              if (checkRepositoryPath(repoURL) == SVNNodeKind.NONE) {
                SVNRepository repository = null;
                try {
                  repository = getRepository(repoURL);
                  long rev = repository.getLatestRevision();
                  // now go back the tree and find if there's anything that exists
                  String repoPath = getRelativePath(repoURL, repository);
                  String p = repoPath;
                  while (p.length() > 0) {
                    p = SVNPathUtil.removeTail(p);
                    if (repository.checkPath(p, rev) == SVNNodeKind.DIR) {
                      // found a matching path
                      List<SVNDirEntry> entries = new ArrayList<SVNDirEntry>();
                      repository.getDir(p, rev, false, entries);

                      // build up the name list
                      List<String> paths = new ArrayList<String>();
                      for (SVNDirEntry e : entries) {
                        if (e.getKind() == SVNNodeKind.DIR) {
                          paths.add(e.getName());
                        }
                      }

                      String
                          head =
                          SVNPathUtil.head(repoPath.substring(p.length() + 1));
                      String candidate = EditDistance.findNearest(head, paths);

                      error(
                          "'%1$s/%2$s' doesn't exist in the repository. Maybe you meant '%1$s/%3$s'?",
                          p, head, candidate);
                      return;
                    }
                  }

                  error(repoPath + " doesn't exist in the repository");
                } finally {
                  if (repository != null) {
                    repository.closeSession();
                  }
                }
              } else {
                ok();
              }
            } catch (SVNException e) {
              String message = "";
              message +=
                  "Unable to access " + Util.escape(url) + " : " + Util
                      .escape(e.getErrorMessage().getFullMessage());
              message += " <a href='#' id=svnerrorlink onclick='javascript:" +
                  "document.getElementById(\"svnerror\").style.display=\"block\";" +
                  "document.getElementById(\"svnerrorlink\").style.display=\"none\";" +
                  "return false;'>(show details)</a>";
              message +=
                  "<pre id=svnerror style='display:none'>" + Functions.printThrowable(e)
                      + "</pre>";
              message +=
                  " (Maybe you need to <a target='_new' href='" + req.getContextPath()
                      + "/scm/SubversionReleaseSCM/enterCredential?" + url
                      + "'>enter credential</a>?)";
              message += "<br>";
              logger.log(Level.INFO, "Failed to access subversion repository " + url, e);
              errorWithMarkup(message);
            }
          }
        }
      }.process();
    }

    public SVNNodeKind checkRepositoryPath(SVNURL repoURL) throws SVNException {
      SVNRepository repository = null;

      try {
        repository = getRepository(repoURL);
        repository.testConnection();

        long rev = repository.getLatestRevision();
        String repoPath = getRelativePath(repoURL, repository);
        return repository.checkPath(repoPath, rev);
      } finally {
        if (repository != null) {
          repository.closeSession();
        }
      }
    }

    protected SVNRepository getRepository(SVNURL repoURL) throws SVNException {
      SVNRepository repository = SVNRepositoryFactory.create(repoURL);

      ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
      sam.setAuthenticationProvider(createAuthenticationProvider());
      repository.setAuthenticationManager(sam);

      return repository;
    }

    /**
     * validate the value for a local location (local checkout directory).
     */
    public void doSvnLocalLocationCheck(StaplerRequest req,
                                        StaplerResponse rsp) throws IOException, ServletException {
      // false==No permission needed for this syntax check
      new FormFieldValidator(req, rsp, false) {
        protected void check() throws IOException, ServletException {
          String v = Util.nullify(request.getParameter("value"));
          if (v == null) {
            // local directory is optional so this is ok
            ok();
            return;
          }

          v = v.trim();

          // check if a absolute path has been supplied
          // (the last check with the regex will match windows drives)
          if (v.startsWith("/") || v.startsWith("\\") || v.startsWith("..") || v
              .matches("^[A-Za-z]:")) {
            error("absolute path is not allowed");
          }

          // all tests passed so far
          ok();
        }
      }.process();
    }

    /**
     * Remoting interface that allows remote {@link ISVNAuthenticationProvider}
     * to read from local {@link DescriptorImpl#credentials}.
     */
    private interface RemotableSVNAuthenticationProvider {
      Credential getCredential(String realm);
    }

    /**
     * Stores {@link SVNAuthentication} for a single realm.
     *
     * <p>
     * {@link Credential} holds data in a persistence-friendly way,
     * and it's capable of creating {@link SVNAuthentication} object,
     * to be passed to SVNKit.
     */
    private static abstract class Credential implements Serializable {
      /**
       * @param kind One of the constants defined in {@link ISVNAuthenticationManager},
       *             indicating what subype of {@link SVNAuthentication} is expected.
       */
      abstract SVNAuthentication createSVNAuthentication(String kind) throws SVNException;
    }

    /**
     * Username/password based authentication.
     */
    private static final class PasswordCredential extends Credential {
      private final String userName;
      private final String password; // scrambled by base64

      public PasswordCredential(String userName,
                                String password) {
        this.userName = userName;
        this.password = Scrambler.scramble(password);
      }

      @Override
      SVNAuthentication createSVNAuthentication(String kind) {
        if (kind.equals(ISVNAuthenticationManager.SSH)) {
          return new SVNSSHAuthentication(userName, Scrambler.descramble(password), -1, false);
        } else {
          return new SVNPasswordAuthentication(userName, Scrambler.descramble(password), false);
        }
      }
    }

    /**
     * Publickey authentication for Subversion over SSH.
     */
    private static final class SshPublicKeyCredential extends Credential {
      private final String userName;
      private final String passphrase; // scrambled by base64
      private final String id;

      /**
       * @param keyFile stores SSH private key. The file will be copied.
       */
      public SshPublicKeyCredential(String userName,
                                    String passphrase,
                                    File keyFile) throws SVNException {
        this.userName = userName;
        this.passphrase = Scrambler.scramble(passphrase);

        Random r = new Random();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 16; i++) {
          buf.append(Integer.toHexString(r.nextInt(16)));
        }
        this.id = buf.toString();

        try {
          FileUtils.copyFile(keyFile, getKeyFile());
        } catch (IOException e) {
          throw new SVNException(SVNErrorMessage
              .create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "Unable to save private key"), e);
        }
      }

      /**
       * Gets the location where the private key will be permanently stored.
       */
      private File getKeyFile() {
        File dir = new File(Hudson.getInstance().getRootDir(), "subversion-credentials");
        if (dir.mkdirs()) {
          // make sure the directory exists. if we created it, try to set the permission to 600
          // since this is sensitive information
          try {
            Chmod chmod = new Chmod();
            chmod.setProject(new Project());
            chmod.setFile(dir);
            chmod.setPerm("600");
            chmod.execute();
          } catch (Throwable e) {
            // if we failed to set the permission, that's fine.
            LOGGER.log(Level.WARNING, "Failed to set directory permission of " + dir, e);
          }
        }
        return new File(dir, id);
      }

      @Override
      SVNSSHAuthentication createSVNAuthentication(String kind) throws SVNException {
        if (kind.equals(ISVNAuthenticationManager.SSH)) {
          try {
            Channel channel = Channel.current();
            String privateKey;
            if (channel != null) {
              // remote
              privateKey = channel.call(new Callable<String, IOException>() {
                public String call() throws IOException {
                  return FileUtils.readFileToString(getKeyFile(), "iso-8859-1");
                }

                @Override
                public void checkRoles(RoleChecker roleChecker) throws SecurityException {
                  roleChecker.check(this, Roles.SLAVE);
                }
              });
            } else {
              privateKey = FileUtils.readFileToString(getKeyFile(), "iso-8859-1");
            }
            return new SVNSSHAuthentication(userName, privateKey.toCharArray(),
                Scrambler.descramble(passphrase), -1, false);
          } catch (IOException e) {
            throw new SVNException(SVNErrorMessage
                .create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "Unable to load private key"),
                e);
          } catch (InterruptedException e) {
            throw new SVNException(SVNErrorMessage
                .create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "Unable to load private key"),
                e);
          }
        } else {
          return null; // unknown
        }
      }
    }

    /**
     * SSL client certificate based authentication.
     */
    private static final class SslClientCertificateCredential extends Credential {
      private final String password; // scrambled by base64
      private final File certificate; // scrambled by base64

      public SslClientCertificateCredential(File certificate,
                                            String password) {
        this.certificate = certificate;
        this.password = Scrambler.scramble(password);
      }

      @Override
      SVNAuthentication createSVNAuthentication(String kind) {
        if (kind.equals(ISVNAuthenticationManager.SSL)) {
          try {
            Secret certificateSecret = Secret.fromString(
                new String(Base64.encode(FileUtils.readFileToByteArray(certificate))));
            SVNSSLAuthentication authentication = SVNSSLAuthentication.newInstance(
                com.trilead.ssh2.crypto.Base64
                    .decode(certificateSecret.getPlainText().toCharArray()),
                Scrambler.descramble(password).toCharArray(),
                false, null, false);
            return authentication;
          } catch (IOException e) {
            throw new Error(e); // can't happen
          }
        } else {
          return null; // unexpected authentication type
        }
      }
    }

    /**
     * See {@link DescriptorImpl#createAuthenticationProvider()}.
     */
    private static final class SVNAuthenticationProviderImpl
        implements ISVNAuthenticationProvider, Serializable {
      private static final long serialVersionUID = 1L;
      private final RemotableSVNAuthenticationProvider source;

      public SVNAuthenticationProviderImpl(RemotableSVNAuthenticationProvider source) {
        this.source = source;
      }

      public SVNAuthentication requestClientAuthentication(String kind,
                                                           SVNURL url,
                                                           String realm,
                                                           SVNErrorMessage errorMessage,
                                                           SVNAuthentication previousAuth,
                                                           boolean authMayBeStored) {
        Credential cred = source.getCredential(realm);
        LOGGER.fine(
            String.format("requestClientAuthentication(%s,%s,%s)=>%s", kind, url, realm, cred));

        try {
          SVNAuthentication auth = null;
          if (cred != null) {
            auth = cred.createSVNAuthentication(kind);
          }

          if (auth == null && ISVNAuthenticationManager.USERNAME.equals(kind)) {
            // this happens with file:// URL and svn+ssh (in this case this method gets invoked twice.)
            // The base class does this, too.
            // user auth shouldn't be null.
            return new SVNUserNameAuthentication("", false);
          }

          return auth;
        } catch (SVNException e) {
          logger.log(Level.SEVERE, "Failed to authorize", e);
          throw new RuntimeException("Failed to authorize", e);
        }
      }

      public int acceptServerAuthentication(SVNURL url,
                                            String realm,
                                            Object certificate,
                                            boolean resultMayBeStored) {
        return ACCEPTED_TEMPORARY;
      }
    }

    private final class RemotableSVNAuthenticationProviderImpl
        implements RemotableSVNAuthenticationProvider, Serializable {
      public Credential getCredential(String realm) {
        LOGGER.fine(String.format("getCredential(%s)=>%s", realm, credentials.get(realm)));
        return credentials.get(realm);
      }

      /**
       * When sent to the remote node, send a proxy.
       */
      private Object writeReplace() {
        return Channel.current().export(RemotableSVNAuthenticationProvider.class, this);
      }
    }
  }

  private static final class Initializer {
    static {
      if (Boolean.getBoolean("hudson.spool-svn")) {
        DAVRepositoryFactory.setup(new DefaultHTTPConnectionFactory(null, true, null));
      } else {
        DAVRepositoryFactory.setup();   // http, https
      }
      SVNRepositoryFactoryImpl.setup();   // svn, svn+xxx
      FSRepositoryFactory.setup();    // file

      // work around for http://www.nabble.com/Slow-SVN-Checkout-tf4486786.html
      if (System.getProperty("svnkit.symlinks") == null) {
        System.setProperty("svnkit.symlinks", "false");
      }

      // disable the connection pooling, which causes problems like
      // http://www.nabble.com/SSH-connection-problems-p12028339.html
      if (System.getProperty("svnkit.ssh2.persistent") == null) {
        System.setProperty("svnkit.ssh2.persistent", "false");
      }

      // use SVN1.4 compatible workspace by default.
      SVNAdminAreaFactory.setSelector(new SubversionWorkspaceSelector());
    }
  }

  /**
   * small structure to store local and remote (repository) location
   * information of the repository. As a addition it holds the invalid field
   * to make failure messages when doing a checkout possible
   */
  public static final class ModuleLocation implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Subversion URL to check out.
     *
     * This may include "@NNN" at the end to indicate a fixed revision.
     */
    public final String remote;
    /**
     * Local directory to place the file to.
     * Relative to the workspace root.
     */
    public final String local;

    public ModuleLocation(String remote,
                          String local) {
      if (local == null) {
        local = getLastPathComponent(remote);
      }

      this.remote = remote.trim();
      this.local = local.trim();
    }

    /**
     * Returns the pure URL portion of {@link #remote} by removing
     * possible "@NNN" suffix.
     */
    public String getURL() {
      int idx = remote.lastIndexOf('@');
      if (idx > 0) {
        try {
          String n = remote.substring(idx + 1);
          Long.parseLong(n);
          return remote.substring(0, idx);
        } catch (NumberFormatException e) {
          // not a revision number
        }
      }
      return remote;
    }

    /**
     * Gets {@link #remote} as {@link SVNURL}.
     */
    public SVNURL getSVNURL() throws SVNException {
      return SVNURL.parseURIEncoded(getURL());
    }

    /**
     * Figures out which revision to check out.
     *
     * If {@link #remote} is {@code url@rev}, then this method
     * returns that specific revision.
     *
     * @param defaultValue If "@NNN" portion is not in the URL, this value will be returned.
     *                     Normally, this is the SVN revision timestamped at the build date.
     */
    public SVNRevision getRevision(SVNRevision defaultValue) {
      int idx = remote.lastIndexOf('@');
      if (idx > 0) {
        try {
          String n = remote.substring(idx + 1);
          return SVNRevision.create(Long.parseLong(n));
        } catch (NumberFormatException e) {
          // not a revision number
        }
      }
      return defaultValue;
    }

    private String getExpandedRemote(AbstractBuild<?, ?> build) {
      String outRemote = remote;

      ParametersAction parameters = build.getAction(ParametersAction.class);
      if (parameters != null) {
        outRemote = parameters.substitute(build, remote);
      }

      return outRemote;
    }

    /**
     * Expand location value based on Build parametric execution.
     *
     * @param build Build instance for expanding parameters into their values
     * @return Output ModuleLocation expanded according to Build parameters
     * values.
     */
    public ModuleLocation getExpandedLocation(AbstractBuild<?, ?> build) {
      return new ModuleLocation(getExpandedRemote(build), local);
    }

    public String toString() {
      return remote;
    }
  }

  // TODO G.ILIADIS code =============================================================


}
