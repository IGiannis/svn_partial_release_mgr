package hudson.plugins.svn_partial_release_mgr.api.functions.build;

import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function5BackupDeploymentInfoJson {

  /**
   * Stores the deployed revisions and other info of the releaseDeployInput into a json file
   * into the build number directory in order to get it after the successful build
   *
   * @param build              the job build info object
   * @param listener           a place to send output
   * @param releaseDeployInput the release deploy input info wrapper
   */
  void storeTheDeploymentInfoToBuildNumberDirectory(AbstractBuild<?, ?> build,
                                                    TaskListener listener,
                                                    ReleaseDeployInput releaseDeployInput)
      throws IOException;

}
