package hudson.plugins.svn_partial_release_mgr.ui;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.scm.SubversionReleaseSCM;

/**
 * Job property for svn-release-mgr.
 */
public final class JobPropertyImpl extends JobProperty<AbstractProject<?, ?>> {

  @DataBoundConstructor
  public JobPropertyImpl() {
  }

  @Override
  public Collection<? extends Action> getJobActions(AbstractProject<?, ?> job) {
    // delegate to getJobAction (singular) for backward compatible behavior
    if (SubversionReleaseSCM.class.equals(job.getScm().getClass())) {
      checkDisableJob(job);
      return Collections.singletonList(new ProjectReleaseAction(job, this));
    }
    return super.getJobActions(job);
  }

  /**
   * Descriptor for Subversion Release Manager job property.
   */
  @Extension
  public static final class DescriptorImpl extends JobPropertyDescriptor {

    public DescriptorImpl() {
      super(JobPropertyImpl.class);
      load();
    }

    @Override
    public String getDisplayName() {
      return "Subversion Releases";
    }

  }

  protected void checkDisableJob(AbstractProject<?, ?> job) {
    boolean isStartedBuild = PluginUtil.isJobStarted();
    if (isStartedBuild) {
      return;
    }
    try {
      job.makeDisabled(true);
    } catch (IOException e) {
      System.out.println("Could not disable job....." + ExceptionUtils.getStackTrace(e));
    }
  }
}
