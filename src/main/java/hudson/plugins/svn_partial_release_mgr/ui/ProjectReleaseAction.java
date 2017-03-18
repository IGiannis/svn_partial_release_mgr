package hudson.plugins.svn_partial_release_mgr.ui;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ProminentProjectAction;
import hudson.model.StringParameterValue;
import hudson.model.TopLevelItem;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.model.AllIssueRevisionsInfo;
import hudson.scm.SubversionReleaseSCM;
import jenkins.model.Jenkins;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class ProjectReleaseAction implements ProminentProjectAction {

  private AbstractProject<?, ?> owner;
  private JobPropertyImpl property;

  public ProjectReleaseAction(AbstractProject<?, ?> owner,
                              JobPropertyImpl property) {
    this.owner = owner;
    this.property = property;
  }

  public String getUrlName() {
    return "releases";
  }

  private SubversionReleaseSCM.ModuleLocation getLocation() {
    return getSubversion().getLocation();
  }

  public AllIssueRevisionsInfo getAllIssueRevisionsInfo() throws IOException {
    // in this point I could not find how to get an non null workspace
    FilePath workspaceDir = Jenkins.getInstance()
        .getWorkspaceFor((TopLevelItem) owner.getRootProject());
    return getSubversion().getIssueRevisionsInfo(workspaceDir.getRemote());
  }

  public String getDisplayName() {
    return Constants.ACTION_NAME;
  }

  public String getIconFileName() {
    return "clipboard.gif";
  }

  public AbstractProject<?, ?> getOwner() {
    return owner;
  }

  public void setOwner(AbstractProject<?, ?> owner) {
    this.owner = owner;
  }

  public JobPropertyImpl getProperty() {
    return property;
  }

  public void setProperty(JobPropertyImpl property) {
    this.property = property;
  }

  public SubversionReleaseSCM getSubversion() {
    return (SubversionReleaseSCM) owner.getScm();
  }

  public boolean hasTagSource() throws IOException {
    // in this point I could not find how to get an non null workspace
    FilePath workspaceDir = Jenkins.getInstance()
        .getWorkspaceFor((TopLevelItem) owner.getRootProject());
    return getSubversion().hasTagBackupSource(workspaceDir.getRemote());
  }

  public void doBuild(StaplerRequest req,
                      StaplerResponse rsp) throws ServletException, IOException {
    String[] selectedRevisionIds = req.getParameterValues("includeInPatch");
    String toStringRevisionIds = StringUtils.join(selectedRevisionIds, ",");
    List<ParameterValue> buildParameters = new ArrayList<>();
    PluginUtil.setJobStarted();
    System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

    buildParameters.add(new StringParameterValue(
        Constants.ENV_PARAM_RELEASE_REVISIONS, toStringRevisionIds));
    buildParameters = getAdditionalParameters(buildParameters, req);

    owner.makeDisabled(false);
    owner.scheduleBuild(0, new Cause.UserCause(),
        new ParametersAction(buildParameters));
    rsp.sendRedirect(req.getContextPath());
  }

  protected List<ParameterValue> getAdditionalParameters(List<ParameterValue> buildParameters,
                                                         StaplerRequest req) {
    for (Map.Entry<String, String> entry : Constants.additionalUserInputParameters.entrySet()) {
      String parameterName = entry.getValue();
      String parameterValue = req.getParameter(parameterName);
      if (StringUtils.isBlank(parameterValue)) {
        continue;
      }
      String environmentVariable = entry.getKey();
      buildParameters.add(new StringParameterValue(environmentVariable, parameterValue));
    }
    return buildParameters;
  }
}
