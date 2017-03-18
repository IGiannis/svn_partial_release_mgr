package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function5BackupDeploymentInfoFile;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function5BackupDeploymentInfoFileImpl implements Function5BackupDeploymentInfoFile {

  /**
   * Stores the deployed revisions and other info of the releaseDeployInput into a json file
   * into the build number directory in order to get it after the successful build
   *
   * @param listener           a place to send output
   * @param releaseDeployInput the release deploy input info wrapper
   */
  @Override
  public void storeTheDeploymentInfoToBuildNumberDirectory(AbstractBuild<?, ?> build,
                                                           TaskListener listener,
                                                           ReleaseDeployInput releaseDeployInput)
      throws IOException {
    String tagName = releaseDeployInput.getReleaseInput().getTagName();
    String deploymentDate = DateFormatUtils.format(build.getTime(), Constants.DEPLOY_DATE_FORMAT);
    TagDeploymentInfo tagDeploymentInfo = new TagDeploymentInfo(tagName,
        deploymentDate, releaseDeployInput.getUserInput());

    File buildDeploymentDir = new File(build.getRootDir(), Constants.DIR_NAME_DEPLOYMENTS);
    FileUtils.forceMkdir(buildDeploymentDir);
    String path = buildDeploymentDir.getAbsolutePath() + "/" +
        Constants.DEPLOYMENT_INFO_XML_FILE_NAME;
    path = FilenameUtils.separatorsToUnix(path);
    PluginUtil.log(listener, "STORING DEPLOYMENT INFO XML FILE [" + path + "]..........");

    try {
      Document xmlDocument = tagDeploymentInfo.toXml();
      PluginUtil.toFile(xmlDocument, new File(path));
    } catch (Exception e) {
      throw new IOException("Error saving the XML file!!" + ExceptionUtils.getStackTrace(e));
    }

  }


}
