package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.File;
import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function2GetPrevDeploymentsFileSources;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function2GetPrevDeploymentsFileSourcesImpl
    extends Function3GetReleaseFileSourcesImpl implements Function2GetPrevDeploymentsFileSources {

  /**
   * Checks out the selected files at their respective revision
   * and copies the source into the build directory
   *
   * @param ws                 the workspace folder
   * @param svnClient          the svn client
   * @param buildRootDir       the root build directory
   * @param releaseDeployInput the release deploy input info wrapper
   * @param listener           a place to send output
   */
  @Override
  public void checkoutAndCopyToBuildDirectoryTheFileSource(File ws,
                                                           AbstractBuild<?, ?> build,
                                                           SVNUpdateClient svnClient,
                                                           File buildRootDir,
                                                           ReleaseDeployInput releaseDeployInput,
                                                           TaskListener listener)
      throws IOException {
    checkoutAndCopyToBuildDirectoryTheFileSource(ws, build, svnClient, buildRootDir,
        releaseDeployInput, listener, releaseDeployInput.getFilesToReDeploy(), "RE-DEPLOY");
  }


}
