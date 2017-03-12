package hudson.plugins.svn_partial_release_mgr.api.model;

import java.util.Map;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class ReleaseDeployInput {
  private final JobConfigurationUserInput releaseInput;
  private final UserInput userInput;
  private final Map<String, Map<String, Long>> issueReleaseFiles;
  private final Map<String, Long> releaseFiles;
  private final Map<String, Long> filesToReDeploy;
  private final String conflictWarnings;

  public ReleaseDeployInput(JobConfigurationUserInput releaseInput,
                            UserInput userInput,
                            Map<String, Map<String, Long>> issueReleaseFiles,
                            Map<String, Long> releaseFiles,
                            Map<String, Long> filesToReDeploy,
                            String conflictWarnings) {
    this.releaseInput = releaseInput;
    this.userInput = userInput;
    this.issueReleaseFiles = issueReleaseFiles;
    this.releaseFiles = releaseFiles;
    this.filesToReDeploy = filesToReDeploy;
    this.conflictWarnings = conflictWarnings;
  }

  public JobConfigurationUserInput getReleaseInput() {
    return releaseInput;
  }

  public Map<String, Long> getReleaseFiles() {
    return releaseFiles;
  }

  public Map<String, Map<String, Long>> getIssueReleaseFiles() {
    return issueReleaseFiles;
  }

  public UserInput getUserInput() {
    return userInput;
  }

  public Map<String, Long> getFilesToReDeploy() {
    return filesToReDeploy;
  }

  public String getConflictWarnings() {
    return conflictWarnings;
  }
}
