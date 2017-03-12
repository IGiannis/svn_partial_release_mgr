package hudson.plugins.svn_partial_release_mgr.api.model.redeploy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;

/**
 * Pojo that holds into a list the info of all the previous deployments for the specific tag
 *
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class TagPreviousDeploymentsInfo {
  private final List<TagDeploymentInfo> existingDeployments;

  public TagPreviousDeploymentsInfo(List<TagDeploymentInfo> existingDeployments) {
    this.existingDeployments = existingDeployments;
  }

  public List<TagDeploymentInfo> getExistingDeployments() {
    return existingDeployments;
  }

  public Set<Long> getAllTagPreviouslyDeployedRevisions() {
    if (existingDeployments == null || existingDeployments.isEmpty()) {
      return null;
    }
    Set<Long> set = new HashSet<>();
    for (TagDeploymentInfo existingDeployment : existingDeployments) {
      set.addAll(existingDeployment.getDeploymentRevisionIds());
    }
    return set;
  }

  public boolean isRevisionAlreadyDeployed(long revisionId) {
    if (existingDeployments == null || existingDeployments.isEmpty()) {
      return false;
    }
    for (TagDeploymentInfo existingDeployment : existingDeployments) {
      if (existingDeployment.isRevisionAlreadyDeployed(revisionId)) {
        return true;
      }
    }
    return false;
  }

  public String getRevisionDeployedDate(long revisionId) {
    if (existingDeployments == null || existingDeployments.isEmpty()) {
      return null;
    }
    for (TagDeploymentInfo existingDeployment : existingDeployments) {
      if (existingDeployment.isRevisionAlreadyDeployed(revisionId)) {
        return existingDeployment.getDeploymentDate();
      }
    }
    return null;
  }
}
