package hudson.plugins.svn_partial_release_mgr.impl;

import hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild.Function1StoreTagDeploymentInfoFile;
import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.PluginFactory;
import hudson.plugins.svn_partial_release_mgr.api.PluginService;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild.Function2PartialPatchCreator;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function0GetReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function1GetTagSource;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function2GetPrevDeploymentsFileSources;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function3GetReleaseFileSources;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function4BackupReleaseFilesAsSrcPatches;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function5BackupDeploymentInfoFile;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function1TagRevisionResolver;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function2RevisionsAfterTagCollector;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function3PrevDeploymentsCollector;
import hudson.plugins.svn_partial_release_mgr.api.model.AllIssueRevisionsInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.Revision;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.redeploy.TagPreviousDeploymentsInfo;
import hudson.plugins.svn_partial_release_mgr.impl.task.ReleaseDeploymentTask;
import hudson.scm.SubversionReleaseSCM;


/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class ReleaseDeploymentService implements PluginService {

  /**
   * Will be called from the PartialReleaseMgrSuccessfulBuilder after a successful build
   * (if configured into the job) in order to mark the files as deployed
   *
   * @param build     a build this is running as a part of
   * @param workspace a workspace to use for any file operations
   * @param launcher  a way to start processes
   * @param listener  a place to send output
   */
  @Override
  public void doAfterSuccessfullBuild(Run<?, ?> build,
                                      FilePath workspace,
                                      Launcher launcher,
                                      TaskListener listener) throws IOException {
    // we have a success build so we need to move the deployment folder
    TagDeploymentInfo tagDeploymentInfo =
        PluginFactory.getBean(Function1StoreTagDeploymentInfoFile.class)
            .moveTheDeploymentInfoFileToTagDeployments(build, workspace, listener);
    // extract a partial patch
    PluginFactory.getBean(Function2PartialPatchCreator.class)
        .createThePartialPatch(build, tagDeploymentInfo, workspace, listener);
    PluginUtil.log(listener, "END OF RELEASE TOOL ACTION ========================");
  }

  /**
   * Returns the object that holds all the revisions from the tag and to the latest source
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  @Override
  public AllIssueRevisionsInfo getAllRevisionsInfo(JobConfigurationUserInput releaseInput,
                                                   String workspaceRootPath) throws IOException {
    long latestTagRevision = PluginFactory.getBean(Function1TagRevisionResolver.class)
        .resolveTagNameRevision(releaseInput);
    return getIssueRevisionsResolver(releaseInput, workspaceRootPath, latestTagRevision);
  }

  /**
   * The main action of the job. It will checkout the tag ,
   * add the release files into the selected revisions and build the result
   *
   * @param releaseInput          the input of the service to get the tag name from
   * @param allIssueRevisionsInfo the pojo wrapper holding all revisions info
   * @param build                 a build this is running as a part of
   * @param launcher              a way to start processes
   * @param workspace             a workspace to use for any file operations
   * @param listener              a place to send output
   * @param changelogFile         the parent changelogFile
   * @param LOGGER                the parent logger
   */
  @Override
  public void checkout(JobConfigurationUserInput releaseInput,
                       AllIssueRevisionsInfo allIssueRevisionsInfo,
                       AbstractBuild<?, ?> build,
                       Launcher launcher,
                       FilePath workspace,
                       BuildListener listener,
                       File changelogFile,
                       Logger LOGGER) throws IOException, InterruptedException {
    try {
      listener.getLogger().println("STARTING THE " + Constants.TITLE + " ACTIONS............");
      ReleaseDeployInput releaseDeployInput =
          PluginFactory.getBean(Function0GetReleaseDeployInput.class)
              .toReleaseDeployInput(build, listener, releaseInput, allIssueRevisionsInfo);

      ReleaseDeploymentTask releaseDeploymentTask = new ReleaseDeploymentTask(build, this,
          releaseDeployInput, workspace, listener, LOGGER);
      workspace.act(releaseDeploymentTask);
    } finally {
      PluginUtil.setJobEnded();
    }
  }

  /**
   * This method will be called after the release button has been pressed
   * and it will start executing the job logic
   *
   * @param ws                 a build this is running as a part of
   * @param releaseDeployInput the deploy input as resolved after the user input
   * @param workspace          a workspace to use for any file operations
   * @param listener           a place to send output
   * @param LOGGER             the parent logger
   */
  @Override
  public void executeTheAsynchJobLogic(File ws,
                                       AbstractBuild<?, ?> build,
                                       ReleaseDeployInput releaseDeployInput,
                                       FilePath workspace,
                                       TaskListener listener,
                                       Logger LOGGER) throws IOException {
    SubversionReleaseSCM.ModuleLocation location = releaseDeployInput
        .getReleaseInput().getLocation();
    String tagName = releaseDeployInput.getReleaseInput().getTagName();

    ISVNAuthenticationProvider authProvider = SubversionReleaseSCM.DescriptorImpl.DESCRIPTOR.
        createAuthenticationProvider();
    // check out the tag as a starting point
    final SVNClientManager manager = SubversionReleaseSCM.createSvnClientManager(authProvider);
    try {
      final SVNUpdateClient svnClient = manager.getUpdateClient();

      // first clean the build directory
      File buildRootDir = new File(ws, location.local);

      if (!PluginUtil.isFastBuild(releaseDeployInput.getUserInput())) {
        cleanBuildDirectory(buildRootDir, listener);

        // check out the tag as a starting point
        PluginUtil.log(listener, "GETTING THE TAG [" + tagName + "] SOURCE ............");
        Function1GetTagSource function1GetTagSource =
            PluginFactory.getBean(Function1GetTagSource.class);
        function1GetTagSource.getTheTagSourceIntoTheBuildDirectory(ws, build,
            manager, svnClient, listener, releaseDeployInput);
      }
      // checkout and copy the already deployed sources into the build directory
      PluginFactory.getBean(Function2GetPrevDeploymentsFileSources.class)
          .checkoutAndCopyToBuildDirectoryTheFileSource(ws, build, svnClient, buildRootDir,
              releaseDeployInput, listener);

      // checkout and copy from svn the release source files into the build directory
      PluginFactory.getBean(Function3GetReleaseFileSources.class)
          .checkoutAndCopyToBuildDirectoryTheFileSource(ws, build, svnClient, buildRootDir,
              releaseDeployInput, listener);

      // create the patch directories with the release files into the build number
      PluginFactory.getBean(Function4BackupReleaseFilesAsSrcPatches.class)
          .copyToBuildNumberDirectoryTheFileSources(ws, build, releaseDeployInput, listener);

      // store the deployment info file
      PluginFactory.getBean(Function5BackupDeploymentInfoFile.class)
          .storeTheDeploymentInfoToBuildNumberDirectory(build, listener, releaseDeployInput);

      PluginUtil.log(listener, "END - WILL START REGULAR MAVEN BUILD NOW.....");
    } finally {
      manager.dispose();
    }
  }

  // ############################# HELP ########################################################


  /**
   * Returns the object that holds all the revisions from the tag and to the latest source
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @param latestTagRevision the tag revision number
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  protected AllIssueRevisionsInfo getIssueRevisionsResolver(JobConfigurationUserInput releaseInput,
                                                            String workspaceRootPath,
                                                            long latestTagRevision)
      throws IOException {
    Collection<Revision> revisions = getRevisions(releaseInput, latestTagRevision);
    TagPreviousDeploymentsInfo existingDeployments =
        resolveTagExistingDeploymentsInfo(releaseInput, workspaceRootPath);
    return new AllIssueRevisionsInfo(releaseInput.getIssuePrefixes(),
        latestTagRevision, existingDeployments, revisions);
  }

  /**
   * Loads all the already deployed revisions
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @return a wrapper object for all the already deployed revisions
   */
  protected TagPreviousDeploymentsInfo resolveTagExistingDeploymentsInfo(JobConfigurationUserInput releaseInput,
                                                                         String workspaceRootPath)
      throws IOException {
    return PluginFactory.getBean(Function3PrevDeploymentsCollector.class)
        .resolveTagExistingDeploymentsInfo(releaseInput, workspaceRootPath);
  }

  /**
   * Cleans the build directory in order to get a fresh copy
   *
   * @param buildRootDir the root build directory to clean
   */
  protected void cleanBuildDirectory(File buildRootDir,
                                     TaskListener listener) throws IOException {
    if (buildRootDir.exists() && buildRootDir.isDirectory()) {
      PluginUtil.log(listener, "CLEANING THE BUILD DIRECTORY [" + buildRootDir + "] ............");
      FileUtils.cleanDirectory(buildRootDir);
    }
  }


  /**
   * Gets all the revisions from the SVN that are greater than the tag revision
   *
   * @param releaseInput the input of the service to get the tag name from
   * @param end          the end revision to get the logs to
   * @return all the revisions from the SVN that are greater than the tag revision
   */
  protected Collection<Revision> getRevisions(JobConfigurationUserInput releaseInput,
                                              long end) throws IOException {
    return PluginFactory.getBean(Function2RevisionsAfterTagCollector.class)
        .getRevisions(releaseInput, end);
  }


}
