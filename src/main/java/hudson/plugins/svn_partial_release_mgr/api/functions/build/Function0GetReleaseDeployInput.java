package hudson.plugins.svn_partial_release_mgr.api.functions.build;

import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.AllIssueRevisionsInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function0GetReleaseDeployInput {

  /**
   * Resolves the final ReleaseDeployInput pojo object to pass around the build functions
   *
   * @param build                 the build info object
   * @param listener              a place to send output
   * @param releaseInput          the user input build original configuration ( tagName , svn location etc )
   * @param allIssueRevisionsInfo the resolved info of all revision after the tag
   * @return the final ReleaseDeployInput pojo object to pass around the build functions
   */
  ReleaseDeployInput toReleaseDeployInput(AbstractBuild<?, ?> build,
                                          TaskListener listener,
                                          JobConfigurationUserInput releaseInput,
                                          AllIssueRevisionsInfo allIssueRevisionsInfo)
      throws IOException,InterruptedException;
}
