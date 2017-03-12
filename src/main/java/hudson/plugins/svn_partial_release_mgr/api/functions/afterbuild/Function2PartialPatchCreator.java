package hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild;

import java.io.IOException;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function2PartialPatchCreator {

  /**
   * This method will create a patch folder inside the deployment directory
   *
   * @param build             the job build info object
   * @param tagDeploymentInfo the json file info for the deployment
   * @param workspace         the workspace path of the job
   * @param listener          a place to send output
   */
  void createThePartialPatch(Run<?, ?> build,
                             TagDeploymentInfo tagDeploymentInfo,
                             FilePath workspace,
                             TaskListener listener) throws IOException;
}
