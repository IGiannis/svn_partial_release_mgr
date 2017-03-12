package hudson.plugins.svn_partial_release_mgr.api.functions.build;

import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.File;
import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function3GetReleaseFileSources {

  /**
   * Checks out the selected files at their respective revision
   * and copies the source into the build directory
   *
   * @param ws                 the workspace folder
   * @param svnClient          the svn client
   * @param releaseDeployInput the release deploy input info wrapper
   * @param buildRootDir       the root build directory
   * @param listener           a place to send output
   */
  void checkoutAndCopyToBuildDirectoryTheFileSource(File ws,
                                                    AbstractBuild<?, ?> build,
                                                    SVNUpdateClient svnClient,
                                                    File buildRootDir,
                                                    ReleaseDeployInput releaseDeployInput,
                                                    TaskListener listener) throws IOException;
}
