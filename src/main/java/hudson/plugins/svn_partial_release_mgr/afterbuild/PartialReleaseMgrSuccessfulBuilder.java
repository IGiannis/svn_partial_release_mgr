package hudson.plugins.svn_partial_release_mgr.afterbuild;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.PluginFactory;
import hudson.plugins.svn_partial_release_mgr.api.PluginService;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

/**
 * Sample {@link Builder}.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link PartialReleaseMgrSuccessfulBuilder} is created.
 * It will parse the changelog and it will store the committed sources into patches and folders
 * according to the commit description
 * <p>
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class PartialReleaseMgrSuccessfulBuilder extends Builder implements SimpleBuildStep {

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public PartialReleaseMgrSuccessfulBuilder() {
  }

  /**
   * Run this step.
   *
   * @param build     a build this is running as a part of
   * @param workspace a workspace to use for any file operations
   * @param launcher  a way to start processes
   * @param listener  a place to send output
   */
  @Override
  public void perform(Run<?, ?> build,
                      FilePath workspace,
                      Launcher launcher,
                      TaskListener listener) throws InterruptedException, IOException {
    try {

      PluginService deployService = PluginFactory.getBean(PluginService.class);
      deployService.doAfterSuccessfullBuild(build, workspace, launcher, listener);

    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  // Overridden for better type safety.
  // If your plugin doesn't really define any property on Descriptor,
  // you don't have to do this.
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * Descriptor for {@link PartialReleaseMgrSuccessfulBuilder}. Used as a singleton.
   * The class is marked as public so that it can be accessed from views.
   * <p>
   * <p>
   * See {@code src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly}
   * for the actual HTML fragment for the configuration screen.
   */
  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    /**
     * In order to load the persisted global configuration, you have to
     * call load() in the constructor.
     */
    public DescriptorImpl() {
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
      return "Svn-Partial Release Manager (After build)";
    }

  }
}

