package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.tmatesoft.svn.core.SVNLogEntryPath;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function0GetReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.AllIssueRevisionsInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.IssueInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.Revision;
import hudson.plugins.svn_partial_release_mgr.api.model.UserInput;
import hudson.scm.SubversionReleaseSCM;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function0GetReleaseDeployInputImpl implements Function0GetReleaseDeployInput {

  /**
   * Resolves the final ReleaseDeployInput pojo object to pass around the build functions
   *
   * @param build                 the build info object
   * @param listener              a place to send output
   * @param releaseInput          the user input build original configuration ( tagName , svn location etc )
   * @param allIssueRevisionsInfo the resolved info of all revision after the tag
   * @return the final ReleaseDeployInput pojo object to pass around the build functions
   */
  @Override
  public ReleaseDeployInput toReleaseDeployInput(AbstractBuild<?, ?> build,
                                                 TaskListener listener,
                                                 JobConfigurationUserInput releaseInput,
                                                 AllIssueRevisionsInfo allIssueRevisionsInfo)
      throws IOException, InterruptedException {
    UserInput userInput = createUserInputFromEnvParameters(build, listener);
    long tagRevision = allIssueRevisionsInfo.getTagRevisionNumber();
    SubversionReleaseSCM.ModuleLocation location = releaseInput.getLocation();
    Set<Long> includeRevisionsIntoTheRelease = userInput.getIncludedRevisionsInRelease();

    // get the files/revisions to check out for the release
    Map<String, FileRevisionsInfo> fileRevisionsInfoMap = markAllFileRevisions(location,
        allIssueRevisionsInfo, includeRevisionsIntoTheRelease);
    Map<String, Long> releaseFiles = resolveIncludedFileRevisions(fileRevisionsInfoMap,
        tagRevision);

    Map<String, Map<String, Long>> issueReleaseFiles =
        getIssueReleaseFiles(allIssueRevisionsInfo, releaseFiles);

    // get the files/revisions to check out for the previous deployments
    Map<String, Long> filesToReDeploy = null;
    Set<Long> tagPreviouslyDeployedRevisions =
        allIssueRevisionsInfo.getTagPreviouslyDeployedRevisions();
    if (tagPreviouslyDeployedRevisions != null && !tagPreviouslyDeployedRevisions.isEmpty()) {
      filesToReDeploy = resolveIncludedFilesRevisionNumber(location,
          tagRevision, allIssueRevisionsInfo, tagPreviouslyDeployedRevisions);
    }

    String conflictWarnings = resolveConflictsWarnings(fileRevisionsInfoMap,
        allIssueRevisionsInfo, releaseFiles,
        includeRevisionsIntoTheRelease, tagPreviouslyDeployedRevisions);
    printConflicts(listener, conflictWarnings);

    return new ReleaseDeployInput(releaseInput, userInput,
        issueReleaseFiles, releaseFiles, filesToReDeploy, conflictWarnings);
  }

  //####################### HELP ########################################################

  protected void printConflicts(TaskListener listener,
                                String conflictWarnings) {
    // print conflicts
    if (!StringUtils.isBlank(conflictWarnings)) {
      PluginUtil.log(listener, "###################### CONFLICT WARNINGS START #################");
      listener.getLogger().println();
      listener.getLogger().println(conflictWarnings);
      listener.getLogger().println();
      PluginUtil.log(listener, "###################### CONFLICT WARNINGS END   #################");
    } else {
      PluginUtil.log(listener, " ###################### NO CONFLICTS ####################");
    }
  }

  /**
   * Convert the user input parameters into a pojo
   *
   * @param build    the build info object
   * @param listener a place to send output
   * @return a pojo wrapper for the http parameters
   */
  protected UserInput createUserInputFromEnvParameters(AbstractBuild<?, ?> build,
                                                       TaskListener listener)
      throws IOException, InterruptedException {
    String releaseVersions = build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_RELEASE_REVISIONS);

    Set<Long> includeRevisionsIntoTheRelease = toRevisionIdsSet(releaseVersions);
    if (includeRevisionsIntoTheRelease == null || includeRevisionsIntoTheRelease.isEmpty()) {
      throw new IOException("No revisions have been checked for deployment!!!");
    }
    boolean generatePartialPatch = NumberUtils.toInt(build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_GENERATE_PARTIAL_PATCH)) == 1;
    boolean generateSourcePartialPatch = NumberUtils.toInt(build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_GENERATE_SRC_PARTIAL_PATCH)) == 1;
    boolean generatePatchForEveryIssue = NumberUtils.toInt(build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_GENERATE_PATCH_FOR_EVERY_ISSUE)) == 1;
    boolean isFastBuild = NumberUtils.toInt(build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_IS_FAST_BUILD)) == 1;
    boolean isTestBuild = NumberUtils.toInt(build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_IS_TEST_BUILD)) == 1;
    boolean includePreviousPatchSources = NumberUtils.toInt(build.getEnvironment(listener)
        .get(Constants.ENV_PARAM_INCLUDE_PREV_PATCH_SOURCES)) == 1;
    return new UserInput(includeRevisionsIntoTheRelease, generatePartialPatch,
        generateSourcePartialPatch, generatePatchForEveryIssue,
        isFastBuild, isTestBuild, includePreviousPatchSources);
  }

  /**
   * Convert the input from the html to a set of included revision ids
   *
   * @param revisionIdsAsString the comma delimited included revision ids
   * @return the set oof the included revision ids
   */
  protected Set<Long> toRevisionIdsSet(String revisionIdsAsString) {
    String[] revisionIdsArray = StringUtils.split(revisionIdsAsString, ",");
    Set<Long> revisionIds = new TreeSet<>();
    for (String revisionId : revisionIdsArray) {
      long id = NumberUtils.toLong(revisionId);
      if (id > 0) {
        revisionIds.add(id);
      }
    }
    return revisionIds;
  }

  /**
   * From the input set of included revision ids extracts the higher revision number for each file
   * and returns a map of file path and revision number as key/value pair respectively
   *
   * @param location                   the svn module location
   * @param latestTagRevision          the tag revision number to compare the file revisions against
   * @param allIssueRevisionsInfo      the wrapper pojo object of all the revisions
   * @param includedRevisionsInRelease the input set of revisions to get the files for
   * @return a map of the files relative path and their revision number to get
   */
  protected Map<String, Long> resolveIncludedFilesRevisionNumber(SubversionReleaseSCM.ModuleLocation location,
                                                                 long latestTagRevision,
                                                                 AllIssueRevisionsInfo allIssueRevisionsInfo,
                                                                 Set<Long> includedRevisionsInRelease)
      throws IOException {
    Map<String, FileRevisionsInfo> fileRevisionsInfoMap = markAllFileRevisions(location,
        allIssueRevisionsInfo, includedRevisionsInRelease);
    return resolveIncludedFileRevisions(fileRevisionsInfoMap, latestTagRevision);
  }

  /**
   * Resolves the map of the issue number and the released files of the issue with their respective max revision
   *
   * @param allIssueRevisionsInfo  the wrapper pojo object of all the revisions
   * @param includedFilesInRelease the map of included file revisions
   * @return the map of the issue number and the released files of the issue with their respective max revision
   */
  protected Map<String, Map<String, Long>> getIssueReleaseFiles(AllIssueRevisionsInfo allIssueRevisionsInfo,
                                                                Map<String, Long> includedFilesInRelease)
      throws IOException {
    Collection<IssueInfo> issueInfoCollection = allIssueRevisionsInfo.getIssues();
    if (issueInfoCollection == null || issueInfoCollection.isEmpty()) {
      return null;
    }
    Map<Long, Set<String>> mapByRevisions = reverseMap(includedFilesInRelease);
    if (mapByRevisions == null || mapByRevisions.isEmpty()) {
      return null;
    }
    Map<String, Map<String, Long>> issueFiles = null;
    for (IssueInfo issueInfo : issueInfoCollection) {
      issueFiles = updateFilesForIssue(issueFiles, mapByRevisions, issueInfo);
    }
    return issueFiles;
  }


  protected Map<String, Map<String, Long>> updateFilesForIssue(Map<String, Map<String, Long>> issueFiles,
                                                               Map<Long, Set<String>> mapByRevisions,
                                                               IssueInfo issueInfo) {

    Collection<Long> issueRevisionIds = issueInfo.getRevisionIds();
    for (Long issueRevisionId : issueRevisionIds) {
      Set<String> revisionFiles = mapByRevisions.get(issueRevisionId);
      if (revisionFiles == null || revisionFiles.isEmpty()) {
        continue;
      }
      String issueNumber = issueInfo.getNumber();
      for (String revisionFile : revisionFiles) {
        issueFiles = PluginUtil.putCheckedObjectInInnerMap(issueFiles,
            issueNumber, revisionFile, issueRevisionId);
      }
    }
    return issueFiles;
  }

  protected Map<Long, Set<String>> reverseMap(Map<String, Long> includedFilesInRelease) {
    if (includedFilesInRelease == null || includedFilesInRelease.isEmpty()) {
      return null;
    }
    Map<Long, Set<String>> mapByRevisions = new HashMap<>(includedFilesInRelease.size());
    for (Map.Entry<String, Long> entry : includedFilesInRelease.entrySet()) {
      String filePath = entry.getKey();
      Long revisionId = entry.getValue();
      mapByRevisions = PluginUtil.putCheckedObjectInInnerSet(mapByRevisions, revisionId, filePath);
    }
    return mapByRevisions;
  }

  protected Map<String, FileRevisionsInfo> markAllFileRevisions(SubversionReleaseSCM.ModuleLocation location,
                                                                AllIssueRevisionsInfo allIssueRevisionsInfo,
                                                                Set<Long> includedRevisionsInRelease) {
    Map<String, FileRevisionsInfo> fileRevisions = new LinkedHashMap<>();
    // already in reverse order
    Collection<Revision> revisions = allIssueRevisionsInfo.getRevisions();
    for (Revision revision : revisions) {
      boolean isRevisionIncluded = includedRevisionsInRelease != null
          && includedRevisionsInRelease.contains(revision.getRevision());
      fileRevisions = handleRevision(fileRevisions,
          location, revision, isRevisionIncluded);
    }
    return fileRevisions;
  }

  protected Map<String, FileRevisionsInfo> handleRevision(Map<String, FileRevisionsInfo> fileRevisions,
                                                          SubversionReleaseSCM.ModuleLocation location,
                                                          Revision revision,
                                                          boolean isIncluded) {
    long revisionID = revision.getLogEntry().getRevision();

    Map<String, SVNLogEntryPath> logEntryPathMap = revision.getLogEntry().getChangedPaths();
    if (logEntryPathMap == null || logEntryPathMap.isEmpty()) {
      return fileRevisions;
    }
    for (SVNLogEntryPath logEntryPath : logEntryPathMap.values()) {
      char type = logEntryPath.getType();
      if ('D' == type) {
        continue;
      }
      String fileRelativePath = toRelativePath(location.getURL(), logEntryPath.getPath());
      FileRevisionsInfo fileRevisionsInfo = fileRevisions.get(fileRelativePath);
      if (fileRevisionsInfo == null) {
        fileRevisionsInfo = new FileRevisionsInfo(fileRelativePath, logEntryPath.getPath());
      }
      fileRevisionsInfo.addRevision(revisionID, isIncluded);
      fileRevisions.put(fileRelativePath, fileRevisionsInfo);
    }
    return fileRevisions;
  }

  /**
   * Finds the files that need to be included into the patch and their final revision
   * in which they should be updated
   *
   * @param fileRevisions     the map of all file revisions after the tag
   * @param latestTagRevision the tag revision number to compare the file revisions against
   * @return the file map of the patch and their final revision
   */
  protected Map<String, Long> resolveIncludedFileRevisions(Map<String, FileRevisionsInfo> fileRevisions,
                                                           long latestTagRevision) {
    Map<String, Long> filesToUpdate = new LinkedHashMap<>();
    for (FileRevisionsInfo fileRevisionsInfo : fileRevisions.values()) {
      long updateRevision = fileRevisionsInfo.getMaxIncludedRevision();
      if (updateRevision <= 0) {
        continue;
      }
      if (updateRevision <= latestTagRevision) {
        continue;
      }
      filesToUpdate.put(fileRevisionsInfo.getFilePath(), updateRevision);
    }
    return filesToUpdate;
  }

  /**
   * Finds the conflicts for all file revisions into the release
   *
   * @param fileRevisions                  the map of all file revisions after the tag
   * @param allIssueRevisionsInfo          the wrapper pojo object of all the revisions
   * @param releaseFiles                   the file that are included into the release
   * @param includedRevisionsInRelease     the input set of revisions to get the files for
   * @param tagPreviouslyDeployedRevisions the set of revisions included into previous releases
   */
  protected String resolveConflictsWarnings(Map<String, FileRevisionsInfo> fileRevisions,
                                            AllIssueRevisionsInfo allIssueRevisionsInfo,
                                            Map<String, Long> releaseFiles,
                                            Set<Long> includedRevisionsInRelease,
                                            Set<Long> tagPreviouslyDeployedRevisions) {
    StringBuilder sb = null;
    for (Map.Entry<String, Long> entry : releaseFiles.entrySet()) {
      String filePath = entry.getKey();
      Long releaseRevision = entry.getValue();
      FileRevisionsInfo fileRevisionsInfo = fileRevisions.get(filePath);
      if (fileRevisionsInfo == null) {
        continue;
      }
      Set<Long> revisionsInConflict = getConflictRevisionsForFile(fileRevisionsInfo,
          releaseRevision, includedRevisionsInRelease, tagPreviouslyDeployedRevisions);
      if (revisionsInConflict == null || revisionsInConflict.isEmpty()) {
        continue;
      }
      if (sb == null) {
        sb = new StringBuilder();
      } else {
        sb.append(Constants.LINE_SEPARATOR);
      }
      sb.append(toConflictMessage(fileRevisionsInfo, allIssueRevisionsInfo,
          releaseRevision, revisionsInConflict));
    }
    return sb != null ? sb.toString() : null;
  }

  /**
   * Finds the revisions of the file that are not included into the release
   *
   * @param fileRevisionsInfo              the file revision info object
   * @param releaseRevision                the revision in which the file will be included into the release
   * @param includedRevisionsInRelease     the set of revisions included into the release
   * @param tagPreviouslyDeployedRevisions the set of revisions included into previous releases
   * @return the revisions of the file that are in conflict
   */
  protected Set<Long> getConflictRevisionsForFile(FileRevisionsInfo fileRevisionsInfo,
                                                  Long releaseRevision,
                                                  Set<Long> includedRevisionsInRelease,
                                                  Set<Long> tagPreviouslyDeployedRevisions) {
    Set<Long> conflictRevisionIds = null;
    Set<Long> allFileRevisions = fileRevisionsInfo.getAllRevisions();
    for (Long fileRevision : allFileRevisions) {
      if (fileRevision >= releaseRevision) {
        continue;
      }
      boolean isRevisionIncluded = includedRevisionsInRelease != null
          && includedRevisionsInRelease.contains(fileRevision);
      if (isRevisionIncluded) {
        continue;
      }
      isRevisionIncluded = tagPreviouslyDeployedRevisions != null
          && tagPreviouslyDeployedRevisions.contains(fileRevision);
      if (isRevisionIncluded) {
        continue;
      }
      conflictRevisionIds = PluginUtil.addCheckedObjectInSet(conflictRevisionIds, fileRevision);
    }
    return conflictRevisionIds;
  }

  /**
   * Generates a log block to display the conflicts for the input file
   *
   * @param fileRevisionsInfo     the file revision info object
   * @param allIssueRevisionsInfo the wrapper pojo object of all the revisions
   * @param releaseRevision       the revision in which the file will be included into the release
   * @param revisionsInConflict   the set of revisions that are not to be released
   * @return a log block to display the conflicts for the input file
   */
  protected String toConflictMessage(FileRevisionsInfo fileRevisionsInfo,
                                     AllIssueRevisionsInfo allIssueRevisionsInfo,
                                     Long releaseRevision,
                                     Set<Long> revisionsInConflict) {
    StringBuilder sb = new StringBuilder();
    String msg = "#### CONFLICT FILE ########### [" + fileRevisionsInfo.getFilePath() + "] "
        + "################################";
    sb.append(msg).append(Constants.LINE_SEPARATOR);
    IssueInfo issueInfo = allIssueRevisionsInfo.getIssueInfoForRevision(releaseRevision);
    sb.append("Revision [" + releaseRevision + "] (" + issueInfo.getUserName() + ") "
        + "will be deployed [ For issue " + issueInfo.getMessage() + "]")
        .append(Constants.LINE_SEPARATOR);
    sb.append("---------------------------------------------------------------------------------")
        .append(Constants.LINE_SEPARATOR);
    sb.append("Conflict revisions that are not included into the release")
        .append(Constants.LINE_SEPARATOR);
    for (Long revisionId : revisionsInConflict) {
      Revision revision = allIssueRevisionsInfo.getRevision(revisionId);
      String date = DateFormatUtils.format(revision.getLogEntry().getDate(),
          Constants.DEPLOY_DATE_FORMAT);
      IssueInfo issue = allIssueRevisionsInfo.getIssueInfoForRevision(revisionId);
      sb.append("Revision [" + revisionId + "] (" + revision.getLogEntry().getAuthor() + " , " +
          date + ") "
          + " [ For issue " + issue.getMessage() + "]");
      sb.append(Constants.LINE_SEPARATOR);
    }
    sb.append(StringUtils.repeat("#", msg.length()));
    return sb.toString();
  }

  protected String toRelativePath(String baseSVNURL,
                                  String filePathInSvn) {
    if (filePathInSvn.startsWith("/")) {
      filePathInSvn = filePathInSvn.substring(1, filePathInSvn.length());
    }
    String[] parts = StringUtils.split(filePathInSvn, "/");
    for (int i = 1; i < parts.length; i++) {
      String[] prefixArray = Arrays.copyOfRange(parts, 0, i);
      String searchPart = StringUtils.join(prefixArray, "/");
      if (baseSVNURL.endsWith(searchPart)) {
        String[] suffixArray = Arrays.copyOfRange(parts, i, parts.length);
        return "/" + StringUtils.join(suffixArray, "/");
      }
    }
    return filePathInSvn;
  }

  public static class FileRevisionsInfo {
    private final String filePath;
    private final String fullFilePath;
    private Set<Long> allRevisions = new LinkedHashSet<>();
    private long maxIncludedRevision;

    public FileRevisionsInfo(String filePath,
                             String fullFilePath) {
      this.fullFilePath = fullFilePath;
      this.filePath = filePath;
    }

    public void addRevision(long revision,
                            boolean isIncluded) {
      allRevisions.add(revision);
      if (isIncluded) {
        maxIncludedRevision = Math.max(maxIncludedRevision, revision);
      }
    }

    public String getFilePath() {
      return filePath;
    }

    public String getFullFilePath() {
      return fullFilePath;
    }

    public long getMaxIncludedRevision() {
      return maxIncludedRevision;
    }

    public Set<Long> getAllRevisions() {
      return allRevisions;
    }

  }
}
