package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function4BackupReleaseFilesAsSrcPatches;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.model.UserInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function4BackupReleaseFilesAsSrcPatchesImpl implements
    Function4BackupReleaseFilesAsSrcPatches {

  /**
   * Creates the source patch directories into the job/builds/number directory
   *
   * @param ws                 the workspace folder
   * @param releaseDeployInput the release deploy input info wrapper
   * @param listener           a place to send output
   */
  @Override
  public void copyToBuildNumberDirectoryTheFileSources(File ws,
                                                       AbstractBuild<?, ?> build,
                                                       ReleaseDeployInput releaseDeployInput,
                                                       TaskListener listener) throws IOException {
    UserInput userInput = releaseDeployInput.getUserInput();
    if (!PluginUtil.isGeneratePartialPatch(userInput)) {
      return;
    }
    PluginUtil.log(listener, "CREATING THE SOURCE PATCHES ............");
    Map<String, Long> releaseFiles = releaseDeployInput.getReleaseFiles();
    // copy the files
    if (releaseFiles == null || releaseFiles.isEmpty()) {
      return;
    }
    // full patch sources
    File buildDeploymentDir = new File(build.getRootDir(), Constants.DIR_NAME_DEPLOYMENTS);
    boolean includePreviousDeploymentsFiles = PluginUtil.isIncludePreviousPatchSources(userInput);
    Map<String, Long> previousDeploymentFiles = includePreviousDeploymentsFiles ?
        releaseDeployInput.getFilesToReDeploy() : null;
    copyFullPatchSrc(ws, buildDeploymentDir, releaseFiles, previousDeploymentFiles);
    // per issue sources
    if (PluginUtil.isGeneratePatchForEveryIssue(userInput)) {
      copyPerIssuePatchSrc(ws, buildDeploymentDir, releaseDeployInput);
    }
  }

  /**
   * Creates a directory job/builds/1/deployments/full_patch/patch-src and copies all the sources
   *
   * @param ws                 the workspace folder
   * @param buildDeploymentDir the build number deployments folder (job/builds/1/deployments)
   * @param releaseFiles       all the files of the full patch
   */
  protected void copyFullPatchSrc(File ws,
                                  File buildDeploymentDir,
                                  Map<String, Long> releaseFiles,
                                  Map<String, Long> previousDeploymentFiles) throws IOException {
    // full patch
    File buildFullPatchDir = new File(buildDeploymentDir, Constants.PATCH_DIR_NAME_FULL);
    copyFilesIntoTheRootDir(ws, buildFullPatchDir, releaseFiles);
    copyFilesIntoTheRootDir(ws, buildFullPatchDir, previousDeploymentFiles);
  }

  /**
   * Creates a directory job/builds/1/deployments/issues_patch/ISSUE_NUMBER/patch-src and copies all the sources
   *
   * @param ws                 the workspace folder
   * @param buildDeploymentDir the build number deployments folder (job/builds/1/deployments)
   * @param releaseDeployInput the release deploy input info wrapper
   */
  protected void copyPerIssuePatchSrc(File ws,
                                      File buildDeploymentDir,
                                      ReleaseDeployInput releaseDeployInput) throws IOException {
    // per source patch
    File buildPerIssuePatchDir = new File(buildDeploymentDir, Constants.PATCH_DIR_NAME_PER_ISSUE);
    Map<String, Map<String, Long>> issueReleaseFiles = releaseDeployInput.getIssueReleaseFiles();
    if (issueReleaseFiles == null || issueReleaseFiles.isEmpty()) {
      return;
    }
    for (Map.Entry<String, Map<String, Long>> entry : issueReleaseFiles.entrySet()) {
      String issueNumber = entry.getKey();
      Map<String, Long> filesForIssue = entry.getValue();
      File issuePatchDir = new File(buildPerIssuePatchDir, issueNumber);
      copyFilesIntoTheRootDir(ws, issuePatchDir, filesForIssue);
    }
  }

  /**
   * Creates a directory /patch-src under the given root directory and copies all the input sources
   *
   * @param ws           the workspace folder
   * @param rootDir      the parent root directory
   * @param releaseFiles all the files to copy
   */
  protected void copyFilesIntoTheRootDir(File ws,
                                         File rootDir,
                                         Map<String, Long> releaseFiles) throws IOException {
    if (releaseFiles == null || releaseFiles.isEmpty()) {
      return;
    }
    File buildFullSrcPatchDir = new File(rootDir, Constants.PATCH_DIR_NAME_SRC);
    for (Map.Entry<String, Long> entry : releaseFiles.entrySet()) {
      String relativePath = entry.getKey();
      long revisionNumber = entry.getValue();
      // revision
      String localLocation = Constants.DIR_NAME_CHECKOUT + "/" +
          Constants.DIR_NAME_REVISIONS + "/" + revisionNumber;
      String relativeLocal = localLocation + relativePath;
      relativeLocal = FilenameUtils.separatorsToUnix(relativeLocal);
      File localFile = new File(ws, relativeLocal);
      if (localFile.isDirectory()) {
        continue;
      }
      // also copy into the deployment folder
      File targetFileDeployment = new File(buildFullSrcPatchDir, relativePath);
      FileUtils.copyFile(localFile, targetFileDeployment);
    }
  }
}
