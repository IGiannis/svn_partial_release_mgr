package hudson.plugins.svn_partial_release_mgr.impl.functions.initview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function3PrevDeploymentsCollector;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.redeploy.TagPreviousDeploymentsInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function3PrevDeploymentsCollectorImpl implements
    Function3PrevDeploymentsCollector {

  /**
   * Loads all the already deployed revisions
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @return a wrapper object for all the already deployed revisions
   */
  @Override
  public TagPreviousDeploymentsInfo resolveTagExistingDeploymentsInfo(JobConfigurationUserInput releaseInput,
                                                                      String workspaceRootPath)
      throws IOException {
    // do copy the files from already check out source
    List<TagDeploymentInfo> existingDeployments = resolveTagExistingDeployments(releaseInput,
        workspaceRootPath);
    return new TagPreviousDeploymentsInfo(existingDeployments);
  }

  /**
   * Loads all the already deployed revisions
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param workspaceRootPath the job workspace path
   * @return a wrapper object for all the already deployed revisions
   */
  protected List<TagDeploymentInfo> resolveTagExistingDeployments(JobConfigurationUserInput releaseInput,
                                                                  String workspaceRootPath)
      throws IOException {
    // do copy the files from already check out source
    String deploymentsDirPath = PluginUtil.getWorkspaceDeploymentPath(workspaceRootPath,
        releaseInput.getTagName());
    File deploymentsDir = new File(deploymentsDirPath);
    if (!deploymentsDir.exists()) {
      return null;
    }
    List<File> allJsonFiles = PluginUtil.getAllFilesInAllSubDirectories(deploymentsDir,
        new Constants.JsonFileFilter());
    if (allJsonFiles == null || allJsonFiles.isEmpty()) {
      return null;
    }
    List<TagDeploymentInfo> existingDeployments = new ArrayList<>(allJsonFiles.size());
    for (File jsonFile : allJsonFiles) {
      TagDeploymentInfo tagDeploymentInfo = TagDeploymentInfo.readFromFile(jsonFile);
      existingDeployments.add(tagDeploymentInfo);
    }
    return existingDeployments;
  }
}
