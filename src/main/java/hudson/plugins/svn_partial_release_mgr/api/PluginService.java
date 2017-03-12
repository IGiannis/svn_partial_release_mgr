package hudson.plugins.svn_partial_release_mgr.api;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.AllIssueRevisionsInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface PluginService {

  /**
   * Returns the object that holds all the revisions from the tag and to the latest source
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  AllIssueRevisionsInfo getAllRevisionsInfo(JobConfigurationUserInput releaseInput,
                                            String workspaceRootPath) throws IOException;

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
  void checkout(JobConfigurationUserInput releaseInput,
                AllIssueRevisionsInfo allIssueRevisionsInfo,
                AbstractBuild<?, ?> build,
                Launcher launcher,
                FilePath workspace,
                final BuildListener listener,
                File changelogFile,
                Logger LOGGER) throws IOException, InterruptedException;

  /**
   * This method will be called after the release button has been pressed
   * and it will start executing the job logic
   *
   * @param ws                 a build this is running as a part of
   * @param build              a build this is running as a part of
   * @param releaseDeployInput the deploy input as resolved after the user input
   * @param workspace          a workspace to use for any file operations
   * @param listener           a place to send output
   * @param LOGGER             the parent logger
   */
  void executeTheAsynchJobLogic(File ws,
                                AbstractBuild<?, ?> build,
                                ReleaseDeployInput releaseDeployInput,
                                FilePath workspace,
                                TaskListener listener,
                                Logger LOGGER) throws IOException;

  /**
   * Will be called from the PartialReleaseMgrSuccessfulBuilder after a successful build
   * (if configured into the job) in order to mark the files as deployed
   *
   * @param build     a build this is running as a part of
   * @param workspace a workspace to use for any file operations
   * @param launcher  a way to start processes
   * @param listener  a place to send output
   */
  void doAfterSuccessfullBuild(Run<?, ?> build,
                               FilePath workspace,
                               Launcher launcher,
                               TaskListener listener) throws IOException;

}
