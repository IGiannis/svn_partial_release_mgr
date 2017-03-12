package hudson.plugins.svn_partial_release_mgr.api.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class IssueInfo {

  private final String number;
  private final String userName;
  private final String message;
  private Map<Long, Revision> revisions;

  public IssueInfo(String number,
                   String userName,
                   String message) {
    this.number = number;
    this.userName = userName;
    this.message = message;
  }

  public String getNumber() {
    return number;
  }

  public String getUserName() {
    return userName;
  }

  public String getMessage() {
    return message;
  }

  public Map<Long, Revision> getRevisions() {
    return revisions;
  }

  public Collection<Long> getRevisionIds() {
    if (revisions == null || revisions.isEmpty()) {
      return new ArrayList<>(0);
    }
    return revisions.keySet();
  }

  public boolean isAlreadyDeployed() {
    if (revisions == null || revisions.isEmpty()) {
      return false;
    }
    long maxDeployedRevision = 0;
    long maxUnDeployedRevision = 0;
    for (Revision revision : revisions.values()) {
      if (revision.isAlreadyDeployed()) {
        maxDeployedRevision = Math.max(maxDeployedRevision, revision.getRevision());
      } else {
        maxUnDeployedRevision = Math.max(maxUnDeployedRevision, revision.getRevision());
      }
    }
    return maxDeployedRevision > maxUnDeployedRevision;
  }

  public String getDeployedDate() {
    if (!isAlreadyDeployed()) {
      return "";
    }
    for (Revision revision : revisions.values()) {
      String deployedDate = revision.getDeployedDate();
      if (!StringUtils.isBlank(deployedDate)) {
        return deployedDate;
      }
    }
    return "";
  }

  public String getRevisionsArray() {
    return StringUtils.join(getRevisionIds(), ",");
  }

  public void addRevisionId(Revision revision) {
    if (revisions == null) {
      revisions = new LinkedHashMap<>();
    }
    revisions.put(revision.getLogEntry().getRevision(), revision);
  }

  public String getStyle() {
    return isAlreadyDeployed() ?
        "border-bottom:1px solid "+ Constants.UI_TABLE_COLOR_ROW_SEPARATOR+";"
            + "background:"+Constants.UI_TABLE_COLOR_ALREADY_DEPLOYED+"" :
        "border-bottom:1px solid "+ Constants.UI_TABLE_COLOR_ROW_SEPARATOR;
  }
}
