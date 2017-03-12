package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function5BackupDeploymentInfoJson;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function5BackupDeploymentInfoJsonImpl implements Function5BackupDeploymentInfoJson {

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
    String path = buildDeploymentDir.getAbsolutePath() + "/" +
        Constants.DEPLOYMENT_INFO_JSON_FILE_NAME;
    path = FilenameUtils.separatorsToUnix(path);
    PluginUtil.log(listener, "STORING DEPLOYMENT INFO JSON FILE [" + path + "]..........");
    FileWriter fileWriter = null;
    JsonWriter writer = null;
    try {
      fileWriter = new FileWriter(path);
      writer = Json.createWriterFactory(
          Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true)
      ).createWriter(fileWriter);
      writer.writeObject(tagDeploymentInfo.toJson());
      writer.close();
    } finally {
      if (writer != null) {
        writer.close();
      }
      if (fileWriter != null) {
        fileWriter.close();
      }
    }
  }


}
