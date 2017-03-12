package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Date;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function1GetTagSource;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.scm.SubversionEventHandlerImpl;
import hudson.scm.SubversionReleaseSCM;
import hudson.util.IOException2;
import hudson.util.StreamCopyThread;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function1GetTagSourceImpl implements Function1GetTagSource {

  /**
   * Gets the source of the tag into the build directory either from SVN or from a backup copy
   *
   * @param ws                 the workspace folder
   * @param build              the build info object
   * @param manager            the svn client manager to be used
   * @param svnClient          the svn client
   * @param listener           a place to send output
   * @param releaseDeployInput the release deploy input that holds the tag name and other info
   */
  @Override
  public void getTheTagSourceIntoTheBuildDirectory(File ws,
                                                   AbstractBuild<?, ?> build,
                                                   SVNClientManager manager,
                                                   SVNUpdateClient svnClient,
                                                   TaskListener listener,
                                                   ReleaseDeployInput releaseDeployInput)
      throws IOException {
    SubversionReleaseSCM.ModuleLocation location =
        releaseDeployInput.getReleaseInput().getLocation();
    String tagName = releaseDeployInput.getReleaseInput().getTagName();

    File buildDir = new File(ws, location.local);
    // do copy the files from already check out source
    String backupTagSourceLocation = PluginUtil.getFullPathToTagBackupSource(ws.getAbsolutePath(),
        tagName);
    File backupDir = new File(backupTagSourceLocation);
    if (getBackupFiles(buildDir, backupDir, listener)) {
      return;
    }
    doCheckoutTheTag(ws, build, manager, svnClient, listener, releaseDeployInput);
    doBackUpTheTagSources(buildDir, backupDir, listener);
  }

  /**
   * Checks if there is a backup copy of the tag source and gets it
   *
   * @param buildDir  the directory were the maven build will take place
   * @param backupDir the directory were the checkout source directory have been backeded up
   * @param listener  a place to send output
   * @return true if a backup copy folder exists
   */
  protected boolean getBackupFiles(File buildDir,
                                   File backupDir,
                                   TaskListener listener) throws IOException {

    if (!backupDir.exists()) {
      return false;
    }
    long t1 = System.currentTimeMillis();
    try {
      PluginUtil.log(listener, "Will copy the files from"
          + " already checked out folder [" + backupDir.getAbsolutePath() + "]..........");
      copyTheBackupSource(backupDir, buildDir, listener);
    } finally {
      long t2 = System.currentTimeMillis();
      PluginUtil.log(listener, "Copy took [" + (t2 - t1) + "ms] ################### ");
    }
    return true;
  }

  /**
   * Copies the backed up sources from latest checkout (in order to avoid already performed checkout)
   * into the build directory
   *
   * @param backupDir the directory were the checkout source directory will be backed up
   * @param buildDir  the directory were the maven build will take place
   */
  protected void copyTheBackupSource(File backupDir,
                                     File buildDir,
                                     TaskListener listener) throws IOException {
    // INSTEAD OF GETTING ALL THE FILES GET SOME OF THEM
    FileUtils.copyDirectory(backupDir, buildDir);
  }

  /**
   * Copies the checked out directory from the svn into a backup directory
   *
   * @param svnCheckoutDir the directory that was checkout from the svn
   * @param backupDir      the directory were the checkout source directory will be backed up
   */
  protected void doBackUpTheTagSources(File svnCheckoutDir,
                                       File backupDir,
                                       TaskListener listener) throws IOException {
    FileUtils.forceMkdir(backupDir);
    long t1 = System.currentTimeMillis();
    try {
      PluginUtil.log(listener, "Will backup the tag source files into "
          + "[" + backupDir.getAbsolutePath() + "]..........");
      FileUtils.copyDirectory(svnCheckoutDir, backupDir);
    } finally {
      long t2 = System.currentTimeMillis();
      PluginUtil.log(listener, "Backup copy took [" + (t2 - t1) + "ms] ################### ");
    }
  }

  /**
   * Returns the tag location into the svn
   *
   * @param releaseDeployInput the release deploy input that holds the tag name and other info
   * @return the tag location into the svn
   */
  protected SubversionReleaseSCM.ModuleLocation toTagLocation(ReleaseDeployInput releaseDeployInput) {
    String remote;
    String tagName = releaseDeployInput.getReleaseInput().getTagName();
    SubversionReleaseSCM.ModuleLocation location =
        releaseDeployInput.getReleaseInput().getLocation();
    try {
      remote = PluginUtil.pathToTag(location.getSVNURL(), tagName);
    } catch (SVNException e) {
      remote = PluginUtil.pathToTagWithSvnUrl(location.getURL(), tagName);
    }
    return new SubversionReleaseSCM.ModuleLocation(Util.removeTrailingSlash(remote),
        StringUtils.trimToNull(location.local));
  }

  /**
   * Checks out the tag from the input root location
   *
   * @param ws        the workspace folder
   * @param manager   the svn client manager to be used
   * @param svnClient the svn client
   */
  protected void doCheckoutTheTag(File ws,
                                  AbstractBuild<?, ?> build,
                                  SVNClientManager manager,
                                  SVNUpdateClient svnClient,
                                  TaskListener listener,
                                  ReleaseDeployInput releaseDeployInput)
      throws IOException {
    long t1 = System.currentTimeMillis();
    String tagName = releaseDeployInput.getReleaseInput().getTagName();
    PluginUtil.log(listener, "Will check out the tag [" + tagName + "]..........");
    Date timestamp = build.getTimestamp().getTime();
    try {
      SubversionReleaseSCM.ModuleLocation tagLocation = toTagLocation(releaseDeployInput);
      SVNRevision revision = SVNRevision.create(timestamp);
      doCheckoutTheTag(ws, revision, manager, svnClient, listener, tagLocation);
    } finally {
      long t2 = System.currentTimeMillis();
      PluginUtil.log(listener, "Check out took [" + (t2 - t1) + "ms] ################### ");
    }
  }

  /**
   * Checks out the tag from the input root location
   *
   * @param ws           the workspace folder
   * @param revision     the revision of the tag
   * @param manager      the svn client manager to be used
   * @param svnClient    the svn client
   * @param rootLocation the svn location for the tag
   */
  protected void doCheckoutTheTag(File ws,
                                  SVNRevision revision,
                                  SVNClientManager manager,
                                  SVNUpdateClient svnClient,
                                  TaskListener listener,
                                  SubversionReleaseSCM.ModuleLocation rootLocation)
      throws IOException {

    File local = new File(ws, rootLocation.local);
    // buffer the output by a separate thread so that the update operation
    // won't be blocked by the remoting of the data
    PipedOutputStream pos = new PipedOutputStream();
    StreamCopyThread sct = new StreamCopyThread("svn log copier", new PipedInputStream(pos),
        listener.getLogger());
    sct.start();

    try {
      PluginUtil.log(listener, "Checking out [" + rootLocation.remote + "] "
          + "to [" + rootLocation.local + "]");

      svnClient.setEventHandler(new SubversionEventHandlerImpl(new PrintStream(pos), local));
      svnClient.doCheckout(rootLocation.getSVNURL(), local.getCanonicalFile(),
          rootLocation.getRevision(revision), rootLocation.getRevision(revision), true);
    } catch (SVNException e) {
      e.printStackTrace(listener.error("Failed to check out " + rootLocation.remote));
      return;
    }

    pos.close();
    try {
      sct.join(); // wait for all data to be piped.
    } catch (InterruptedException e) {
      throw new IOException2("interrupted", e);
    }

    try {

      SVNDirEntry dir = manager.createRepository(rootLocation.getSVNURL(), true).info("/", -1);
      if (dir != null) {// I don't think this can ever be null, but be defensive
        if (dir.getDate().after(new Date())) {
          listener.getLogger().println(
              hudson.scm.subversion.Messages.SubversionSCM_ClockOutOfSync());
        }
      }

    } catch (SVNException e) {
      listener.getLogger().println("Failed to estimate "
          + "the remote time stamp" + ExceptionUtils.getStackTrace(e));
    }
  }
}
