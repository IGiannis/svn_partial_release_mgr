package hudson.plugins.svn_partial_release_mgr.impl.task;

import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.PluginService;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.remoting.VirtualChannel;
import jenkins.security.Roles;

/**
 * Either run "svn co" or "svn up" equivalent.
 */
public class ReleaseDeploymentTask
    implements FilePath.FileCallable<Boolean> {
  private static final long serialVersionUID = 1L;

  protected final Logger LOGGER;
  // true to "svn update", false to "svn checkout".
  protected final AbstractBuild<?, ?> build;
  protected final PluginService pluginService;
  protected final ReleaseDeployInput releaseDeployInput;
  protected final FilePath workspace;
  protected final TaskListener listener;

  public ReleaseDeploymentTask(AbstractBuild<?, ?> build,
                               PluginService pluginService,
                               ReleaseDeployInput releaseDeployInput,
                               FilePath workspace,
                               TaskListener listener,
                               Logger LOGGER) {
    this.build = build;
    this.pluginService = pluginService;
    this.releaseDeployInput = releaseDeployInput;
    this.workspace = workspace;
    this.listener = listener;
    this.LOGGER = LOGGER;

  }

  @Override
  public void checkRoles(RoleChecker checker) throws SecurityException {
    checker.check(this, Roles.SLAVE);
  }

  @Override
  public Boolean invoke(File ws,
                        VirtualChannel channel) throws IOException {
    try {
      pluginService.executeTheAsynchJobLogic(ws, build,
          releaseDeployInput, workspace, listener, LOGGER);
      return true;
    } finally {
      PluginUtil.setJobEnded();
    }
  }


}
