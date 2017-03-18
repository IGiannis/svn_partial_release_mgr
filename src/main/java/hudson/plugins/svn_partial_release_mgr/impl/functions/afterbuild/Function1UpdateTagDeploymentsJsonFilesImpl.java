package hudson.plugins.svn_partial_release_mgr.impl.functions.afterbuild;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild.Function1UpdateTagDeploymentsJsonFiles;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function1UpdateTagDeploymentsJsonFilesImpl
    implements Function1UpdateTagDeploymentsJsonFiles {


  /**
   * Copies the saved deployment info json file from the build number directory of the job
   * into the deployments/tag folder
   *
   * @param workspace the workspace path of the job
   * @param listener  a place to send output
   */
  @Override
  public TagDeploymentInfo moveTheJsonDeploymentInfoToTagDeployments(Run<?, ?> build,
                                                                     FilePath workspace,
                                                                     TaskListener listener)
      throws IOException {
    String jsonPath = build.getRootDir() +
        "/" + Constants.DIR_NAME_DEPLOYMENTS +
        "/" + Constants.DEPLOYMENT_INFO_XML_FILE_NAME;
    File jsonDeploymentInfoFile = new File(FilenameUtils.separatorsToUnix(jsonPath));
    if (!jsonDeploymentInfoFile.exists()) {
      return null;
    }
    TagDeploymentInfo tagDeploymentInfo = TagDeploymentInfo.readFromFile(jsonDeploymentInfoFile);
    if (PluginUtil.isTestBuild(tagDeploymentInfo.getUserInput())) {
      return tagDeploymentInfo;
    }

    // do copy the files from already check out source
    File destDir = new File(PluginUtil.getWorkspaceTagDeploymentDatePath(workspace,
        tagDeploymentInfo));
    if (!destDir.exists()) {
      FileUtils.forceMkdir(destDir);
    }

    PluginUtil.log(listener, "COPY THE JSON DEPLOYMENT INFO FILE TO "
        + "[" + destDir.getAbsolutePath() + "]..........");
    FileUtils.copyFileToDirectory(jsonDeploymentInfoFile, destDir);
    return tagDeploymentInfo;
  }


}
