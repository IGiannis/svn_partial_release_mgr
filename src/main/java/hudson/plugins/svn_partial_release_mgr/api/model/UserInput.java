package hudson.plugins.svn_partial_release_mgr.api.model;

import org.apache.commons.lang.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class UserInput {

  private final Set<Long> includedRevisionsInRelease;
  private final Map<String, String> additionalParameters;

  public UserInput(Set<Long> includedRevisionsInRelease,
                   Map<String, String> additionalParameters) {
    this.includedRevisionsInRelease = includedRevisionsInRelease;
    this.additionalParameters = additionalParameters;
  }

  public Set<Long> getIncludedRevisionsInRelease() {
    return includedRevisionsInRelease;
  }

  public Map<String, String> getAdditionalParameters() {
    return additionalParameters;
  }

  public String getAdditionalParameterValue(String parameterName) {
    return additionalParameters != null ? additionalParameters.get(parameterName) : null;
  }

  public Node toXML(Document document) {
    Element userInputNode = document.createElement("userInput");
    for (Map.Entry<String, String> entry : additionalParameters.entrySet()) {
      String parameterName = entry.getKey();
      String parameterValue = entry.getValue();
      PluginUtil.addNodeInNode(userInputNode, parameterName, parameterValue);
    }
    Element deploymentRevisionsElement = document.createElement("deploymentRevisionIds");
    userInputNode.appendChild(deploymentRevisionsElement);
    for (Long revisionId : includedRevisionsInRelease) {
      PluginUtil.addNodeInNode(deploymentRevisionsElement, "revision", String.valueOf(revisionId));
    }
    return userInputNode;
  }

  public static UserInput fromXML(Node rootXmlNode) {
    Node userInputNode = PluginUtil.findNextNode(rootXmlNode, "userInput");
    Map<String, String> additionalParameters = PluginUtil.getChildNodeValues(userInputNode);
    Node deploymentRevisionsElement = PluginUtil.findNextNode(rootXmlNode, "deploymentRevisionIds");
    List<Node> revisionNodesList =
        PluginUtil.getNodesInNode(deploymentRevisionsElement, "revision");
    Set<Long> deploymentRevisionIds = new HashSet<>();
    for (Node revisionNode : revisionNodesList) {
      String revisionIDValue = PluginUtil.getValueFromTextNode(revisionNode);
      deploymentRevisionIds.add(NumberUtils.toLong(revisionIDValue));
    }
    return new UserInput(deploymentRevisionIds, additionalParameters);
  }
}
