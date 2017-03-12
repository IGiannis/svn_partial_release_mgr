package hudson.plugins.svn_partial_release_mgr.api.model;

import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import hudson.plugins.svn_partial_release_mgr.api.model.redeploy.TagPreviousDeploymentsInfo;

/**
 * Pojo class to hold the info for all the issues and all the revisions that were committed for an issue
 *
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class AllIssueRevisionsInfo {
  public static final String COMMIT_NO_ISSUE = "COMMIT_NO_ISSUE";

  private final long tagRevisionNumber;
  private final Map<Long, Revision> revisions;
  private final Map<String, IssueInfo> issues;
  private final Map<Long, String> revisionToIssueMap;
  private final String[] issuePrefixesArr;
  private final Set<Long> tagPreviouslyDeployedRevisions;

  public AllIssueRevisionsInfo(String issuePrefixes,
                               long tagRevisionNumber,
                               TagPreviousDeploymentsInfo existingDeployments,
                               Collection<Revision> revisions) {
    this.issuePrefixesArr = StringUtils.split(issuePrefixes, ",");
    this.tagRevisionNumber = tagRevisionNumber;
    this.revisions = toRevisionList(revisions, existingDeployments);
    this.tagPreviouslyDeployedRevisions =
        getAllTagPreviouslyDeployedRevisions(existingDeployments);
    this.issues = toIssueMap();
    revisionToIssueMap = toRevisionIssueMap();
  }

  // ################### GETTERS ########################################

  public long getTagRevisionNumber() {
    return tagRevisionNumber;
  }

  public Collection<IssueInfo> getIssues() {
    return issues.values();
  }

  public Collection<Revision> getRevisions() {
    return revisions != null ? revisions.values() : null;
  }

  public String getIssueNumberForRevision(long revisionID) {
    return revisionToIssueMap.get(revisionID);
  }

  public Revision getRevision(long revisionID) {
    return revisions != null ? revisions.get(revisionID) : null;
  }

  public String getIssueMessageForRevision(long revisionID) {
    String issueNumber = getIssueNumberForRevision(revisionID);
    IssueInfo issueInfo = getIssueInfo(issueNumber);
    return issueInfo != null ? issueInfo.getMessage() : null;
  }

  public IssueInfo getIssueInfo(String issueNumber) {
    return issues != null ? issues.get(issueNumber) : null;
  }

  public IssueInfo getIssueInfoForRevision(long revisionID) {
    String issueNumber = getIssueNumberForRevision(revisionID);
    if (StringUtils.isBlank(issueNumber)) {
      return null;
    }
    return getIssueInfo(issueNumber);
  }

  public boolean hasPreviousDeployments(){
    return tagPreviouslyDeployedRevisions!=null&&!tagPreviouslyDeployedRevisions.isEmpty();
  }

  public Set<Long> getTagPreviouslyDeployedRevisions() {
    return tagPreviouslyDeployedRevisions;
  }

  // ################### LOADER METHODS ########################################
  protected Set<Long> getAllTagPreviouslyDeployedRevisions(TagPreviousDeploymentsInfo existingDeployments) {
    if (existingDeployments == null) {
      return null;
    }
    return existingDeployments.getAllTagPreviouslyDeployedRevisions();
  }

  protected Map<Long, Revision> toRevisionList(Collection<Revision> revisions,
                                               TagPreviousDeploymentsInfo existingDeployments) {
    if (revisions == null || revisions.isEmpty()) {
      return null;
    }
    Map<Long, Revision> revisionList = new LinkedHashMap<>(revisions.size());
    for (Revision revision : revisions) {
      Revision revisionForList = checkRevisionAlreadyDeployed(revision, existingDeployments);
      revisionList.put(revisionForList.getRevision(), revisionForList);
    }
    return revisionList;
  }

  protected Revision checkRevisionAlreadyDeployed(Revision revision,
                                                  TagPreviousDeploymentsInfo existingDeployments) {
    if (existingDeployments == null) {
      return revision;
    }
    String dateDeployed = existingDeployments.getRevisionDeployedDate(revision.getRevision());
    if (!StringUtils.isBlank(dateDeployed)) {
      return revision.asAlreadyDeployed(dateDeployed);
    }
    return revision;
  }

  protected Map<Long, String> toRevisionIssueMap() {
    Map<Long, String> revisionToIssueMap = new HashMap<>();
    for (IssueInfo issueInfo : issues.values()) {
      Collection<Long> revisionIds = issueInfo.getRevisionIds();
      for (Long revisionId : revisionIds) {
        revisionToIssueMap.put(revisionId, issueInfo.getNumber());
      }
    }
    return revisionToIssueMap;
  }

  protected Map<String, IssueInfo> toIssueMap() {
    Map<String, IssueInfo> issues = new LinkedHashMap<>();
    if (revisions == null || revisions.isEmpty()) {
      return issues;
    }
    for (Revision revision : revisions.values()) {
      String msg = revision.getLogEntry().getMessage();
      String issueNumber = StringUtils.defaultIfBlank(resolveIssueNumber(msg), COMMIT_NO_ISSUE);
      IssueInfo issueInfo = issues.get(issueNumber);
      if (issueInfo == null) {
        issueInfo = new IssueInfo(issueNumber, revision.getLogEntry().getAuthor(), msg);
      }
      issueInfo.addRevisionId(revision);
      issues.put(issueNumber, issueInfo);
    }
    return issues;
  }

  protected String resolveIssueNumber(String msg) {
    for (String prefix : issuePrefixesArr) {
      if (!msg.contains(prefix)) {
        continue;
      }
      String issueNumber = resolveIssueFromPrefix(msg, prefix);
      if (!StringUtils.isBlank(issueNumber)) {
        return issueNumber;
      }
    }
    return COMMIT_NO_ISSUE;
  }

  protected String resolveIssueFromPrefix(String msg,
                                          String prefix) {
    int index = msg.indexOf(prefix);
    if (index < 0) {
      return null;
    }
    String rightPart = msg.substring(index + prefix.length(), msg.length());
    StringBuilder sb = new StringBuilder(prefix);
    sb.append(getNumbersPart(rightPart));
    return sb.toString();
  }

  protected String getNumbersPart(String str) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (!Character.isDigit(c)) {
        return sb.toString();
      }
      sb.append(c);
    }
    return sb.toString();
  }

}
