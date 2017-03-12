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
public interface Function1UpdateTagDeploymentsJsonFiles {


  /**
   * Copies the saved deployment info json file from the build number directory of the job
   * into the deployments/tag folder
   *
   * @param workspace the workspace path of the job
   * @param listener  a place to send output
   */
  TagDeploymentInfo moveTheJsonDeploymentInfoToTagDeployments(Run<?, ?> build,
                                                              FilePath workspace,
                                                              TaskListener listener)
      throws IOException;


}
