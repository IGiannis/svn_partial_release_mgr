package hudson.plugins.svn_partial_release_mgr.api.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class TagDeploymentInfo {

  private final String tagName;
  private final String deploymentDate;
  private final UserInput userInput;

  public TagDeploymentInfo(String tagName,
                           String deploymentDate,
                           UserInput userInput) {
    this.tagName = tagName;
    this.deploymentDate = deploymentDate;
    this.userInput = userInput;
  }

  public String getTagName() {
    return tagName;
  }

  public String getDeploymentDate() {
    return deploymentDate;
  }

  public Set<Long> getDeploymentRevisionIds() {
    return userInput.getIncludedRevisionsInRelease();
  }

  public UserInput getUserInput() {
    return userInput;
  }

  public boolean isRevisionAlreadyDeployed(long revisionId) {
    Set<Long> deploymentRevisionIds = getDeploymentRevisionIds();
    return deploymentRevisionIds != null && deploymentRevisionIds.contains(revisionId);
  }

  /**
   * Reads the stored json file into a new pojo instance
   *
   * @param jsonFile the stored json string to get the info from
   * @return the new instance of this pojo
   */
  public static TagDeploymentInfo readFromFile(File jsonFile) throws IOException {
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(jsonFile);

      //create JsonReader object
      JsonReader jsonReader = Json.createReader(fileInputStream);
      //get JsonObject from JsonReader
      JsonObject jsonObject = jsonReader.readObject();
      String tagName = jsonObject.getString("tagName");
      String deploymentDate = jsonObject.getString("deploymentDate");

      JsonObject userInputJsonObject = jsonObject.getJsonObject("userInput");
      UserInput userInput = UserInput.fromJson(userInputJsonObject);
      return new TagDeploymentInfo(tagName, deploymentDate, userInput);
    } finally {
      if (fileInputStream != null) {
        fileInputStream.close();
      }
    }

  }

  /**
   * Converts this pojo to a json string in order to be stored into the disk
   *
   * @return the JsonObject to be stored into a file
   */
  public JsonObject toJson() {
    JsonObject userInputJsonObject = userInput.toJson();
    return Json.createObjectBuilder()
        .add("tagName", tagName)
        .add("deploymentDate", deploymentDate)
        .add("userInput", userInputJsonObject)
        .build();
  }
}
