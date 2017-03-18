package hudson.plugins.svn_partial_release_mgr.api.model;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;

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
   * Reads the stored xml file into a new pojo instance
   *
   * @param xmlFile the stored xml string to get the info from
   * @return the new instance of this pojo
   */
  public static TagDeploymentInfo readFromFile(File xmlFile) throws IOException {
    try {
      Document xmlDocument = PluginUtil.buildW3CDocumentFromFile(xmlFile,
          Constants.ENCODING_UTF8);
      Node rootNode = xmlDocument.getFirstChild();
      String tagName = PluginUtil.getValueFromChildElement(rootNode, "tagName");
      String deploymentDate = PluginUtil.getValueFromChildElement(rootNode, "deploymentDate");
      UserInput userInput = UserInput.fromXML(rootNode);
      return new TagDeploymentInfo(tagName, deploymentDate, userInput);

    } catch (Exception e) {
      throw new IOException("Error reading the xml file [" + xmlFile.getAbsolutePath() + "]!!" +
          ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Converts this pojo to an XML string in order to be stored into the disk
   *
   * @return the XMl document to be stored into a file
   */
  public Document toXml() {
    Document document = PluginUtil.buildNewW3CDocument();
    Element rootNode = document.createElement("deployment");
    document.appendChild(rootNode);
    PluginUtil.addNodeInNode(rootNode, "tagName", tagName);
    PluginUtil.addNodeInNode(rootNode, "deploymentDate", deploymentDate);
    Node userInputXML = userInput.toXML(document);
    rootNode.appendChild(userInputXML);
    return document;
  }
}
