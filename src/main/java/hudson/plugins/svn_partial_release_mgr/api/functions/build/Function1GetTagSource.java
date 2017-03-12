package hudson.plugins.svn_partial_release_mgr.api.functions.build;

import org.tmatesoft.svn.core.wc.SVNClientManager;
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
public interface Function1GetTagSource {

  /**
   * Gets the source of the tag into the build directory either from SVN or from a backup copy
   *
   * @param ws                 the workspace folder
   * @param build              the build info object
   * @param manager            the svn client manager to be used
   * @param svnClient          the svn client
   * @param listener           a place to send output
   * @param releaseDeployInput the release deploy input that holds the tag name and other info
   */
  void getTheTagSourceIntoTheBuildDirectory(File ws,
                                            AbstractBuild<?, ?> build,
                                            SVNClientManager manager,
                                            SVNUpdateClient svnClient,
                                            TaskListener listener,
                                            ReleaseDeployInput releaseDeployInput)
      throws IOException;
}
