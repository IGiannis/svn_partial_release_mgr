package hudson.plugins.svn_partial_release_mgr.api.functions.initview;

import java.io.IOException;

import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.redeploy.TagPreviousDeploymentsInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function3PrevDeploymentsCollector {

  /**
   * Loads all the already deployed revisions
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @return a wrapper object for all the already deployed revisions
   */
  TagPreviousDeploymentsInfo resolveTagExistingDeploymentsInfo(JobConfigurationUserInput releaseInput,
                                                               String workspaceRootPath)
      throws IOException;
}
