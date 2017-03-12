package hudson.plugins.svn_partial_release_mgr.api.functions.build;

import java.io.File;
import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function4BackupReleaseFilesAsSrcPatches {

  /**
   * Creates the source patch directories into the job/builds/number directory
   *
   * @param ws                 the workspace folder
   * @param releaseDeployInput the release deploy input info wrapper
   * @param listener           a place to send output
   */
  void copyToBuildNumberDirectoryTheFileSources(File ws,
                                                AbstractBuild<?, ?> build,
                                                ReleaseDeployInput releaseDeployInput,
                                                TaskListener listener) throws IOException;
}
