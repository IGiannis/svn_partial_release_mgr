package hudson.plugins.svn_partial_release_mgr.impl.functions.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function3GetReleaseFileSources;
import hudson.plugins.svn_partial_release_mgr.api.model.ReleaseDeployInput;
import hudson.scm.SubversionReleaseSCM;
import hudson.util.IOException2;
import hudson.util.StreamCopyThread;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function3GetReleaseFileSourcesImpl implements Function3GetReleaseFileSources {

  /**
   * Checks out the selected files at their respective revision
   * and copies the source into the build directory
   *
   * @param ws                 the workspace folder
   * @param svnClient          the svn client
   * @param buildRootDir       the root build directory
   * @param releaseDeployInput the release deploy input info wrapper
   * @param listener           a place to send output
   */
  @Override
  public void checkoutAndCopyToBuildDirectoryTheFileSource(File ws,
                                                           AbstractBuild<?, ?> build,
                                                           SVNUpdateClient svnClient,
                                                           File buildRootDir,
                                                           ReleaseDeployInput releaseDeployInput,
                                                           TaskListener listener)
      throws IOException {
    checkoutAndCopyToBuildDirectoryTheFileSource(ws, build, svnClient, buildRootDir,
        releaseDeployInput, listener, releaseDeployInput.getReleaseFiles(), "RELEASE");
  }

  /**
   * Checks out the selected files at their respective revision
   * and copies the source into the build directory
   *
   * @param ws                 the workspace folder
   * @param svnClient          the svn client
   * @param buildRootDir       the root build directory
   * @param releaseDeployInput the release deploy input info wrapper
   * @param filesToUpdate      the files relative path and their revision number
   * @param listener           a place to send output
   * @param message            the message to send to the logger
   */
  protected void checkoutAndCopyToBuildDirectoryTheFileSource(File ws,
                                                              AbstractBuild<?, ?> build,
                                                              SVNUpdateClient svnClient,
                                                              File buildRootDir,
                                                              ReleaseDeployInput releaseDeployInput,
                                                              TaskListener listener,
                                                              Map<String, Long> filesToUpdate,
                                                              String message) throws IOException {
    // get the release files from SVN
    PluginUtil.log(listener, "GETTING THE " + message + " "
        + "FILE SOURCES ( SVN or BACKUP )............");
    checkoutTheReleaseFilesSources(ws, releaseDeployInput,
        listener, svnClient, filesToUpdate);

    // copy the extracted from svn source files into the build directory
    PluginUtil.log(listener, "COPYING THE " + message + " "
        + "FILES INTO THE BUILD DIRECTORY............");
    copyTheSourceIntoTheBuildDirectory(buildRootDir, ws,
        listener, filesToUpdate);
  }

  /**
   * Checks out from the SVN the revisions files that will be included into the release
   *
   * @param ws        the workspace folder
   * @param svnClient the svn client
   */
  protected void checkoutTheReleaseFilesSources(File ws,
                                                ReleaseDeployInput releaseDeployInput,
                                                TaskListener listener,
                                                SVNUpdateClient svnClient,
                                                Map<String, Long> filesToUpdate)
      throws IOException {
    try {
      // revert
      doGetTheFilesInTheMaxRevisionInSvn(ws, releaseDeployInput,
          listener, svnClient, filesToUpdate);
    } catch (SVNException e) {
      PluginUtil.log(listener, "Could not revert the files!!" + ExceptionUtils.getStackTrace(e));
      throw new IOException("Could not revert the files!!" + ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * For all files that we need to get the source for we first
   * check if we already have checked out the source and if not we check out
   *
   * @param ws                 the workspace folder
   * @param releaseDeployInput the release deploy input info wrapper
   * @param listener           a place to send output
   * @param svnClient          the svn client
   * @param filesToUpdate      the files relative path and their revision number
   */
  protected void doGetTheFilesInTheMaxRevisionInSvn(File ws,
                                                    ReleaseDeployInput releaseDeployInput,
                                                    TaskListener listener,
                                                    SVNUpdateClient svnClient,
                                                    Map<String, Long> filesToUpdate)
      throws IOException, SVNException {
    if (filesToUpdate == null || filesToUpdate.isEmpty()) {
      return;
    }
    for (Map.Entry<String, Long> entry : filesToUpdate.entrySet()) {
      String relativePath = entry.getKey();
      long revisionNumber = entry.getValue();
      // revision
      String localLocation = Constants.DIR_NAME_CHECKOUT + "/" +
          Constants.DIR_NAME_REVISIONS + "/" + revisionNumber;

      String relativeLocal = localLocation + relativePath;
      relativeLocal = FilenameUtils.separatorsToUnix(relativeLocal);
      File localFile = new File(ws, relativeLocal);
      if (localFile.exists()) {
        PluginUtil.log(listener, "File [" + relativePath + "] [r="+revisionNumber+"] "
            + "exists in backup.....");
        continue;
      }
      PluginUtil.log(listener, "Will update file ......[" + relativePath + "] "
          + "to revision [" + revisionNumber + "] at local path [" + relativeLocal + "]");

      String svnRemoteLocation = releaseDeployInput.getReleaseInput().getLocation().remote;
      String remoteForFile = FilenameUtils.separatorsToUnix(svnRemoteLocation + relativePath);
      SubversionReleaseSCM.ModuleLocation fileLocation =
          new SubversionReleaseSCM.ModuleLocation(remoteForFile, relativeLocal);
      SVNRevision revision = SVNRevision.create(revisionNumber);
      doExportTheFile(ws, listener, revision, svnClient, fileLocation);
    }
  }

  /**
   * Gets the source file from the svn for the input file location
   *
   * @param ws                 the workspace folder
   * @param listener           a place to send output
   * @param fileRevisionNumber the revision of the file to get the source for
   * @param svnClient          the svn client
   * @param fileLocation       the file remote and local location for the svn
   */
  protected void doExportTheFile(File ws,
                                 TaskListener listener,
                                 SVNRevision fileRevisionNumber,
                                 SVNUpdateClient svnClient,
                                 SubversionReleaseSCM.ModuleLocation fileLocation)
      throws IOException {

    File local = new File(ws, fileLocation.local);
    // buffer the output by a separate thread so that the update operation
    // won't be blocked by the remoting of the data
    PipedOutputStream pos = new PipedOutputStream();
    StreamCopyThread sct = new StreamCopyThread("svn log copier", new PipedInputStream(pos),
        listener.getLogger());
    sct.start();

    try {
      PluginUtil.log(listener, "Checking out[" + fileLocation.remote + "] "
          + "to [" + fileLocation.local + "]");
      svnClient.doExport(fileLocation.getSVNURL(), local.getCanonicalFile(),
          fileLocation.getRevision(fileRevisionNumber),
          fileLocation.getRevision(fileRevisionNumber), null, true,
          SVNDepth.INFINITY);
    } catch (SVNException e) {
      e.printStackTrace(listener.error("Failed to check out " + fileLocation.remote));
    }

    pos.close();
    try {
      sct.join(); // wait for all data to be piped.
    } catch (InterruptedException e) {
      throw new IOException2("interrupted", e);
    }
  }

  /**
   * Copies the source of all the files that will be included into the release into the build directory
   *
   * @param buildRootDir  the root build directory
   * @param ws            the workspace folder
   * @param listener      a place to send output
   * @param filesToUpdate the files relative path and their revision number
   */
  protected void copyTheSourceIntoTheBuildDirectory(File buildRootDir,
                                                    File ws,
                                                    TaskListener listener,
                                                    Map<String, Long> filesToUpdate)
      throws IOException {
    // copy the files
    if (filesToUpdate == null || filesToUpdate.isEmpty()) {
      return;
    }

    for (Map.Entry<String, Long> entry : filesToUpdate.entrySet()) {
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
      File targetFile = new File(buildRootDir, relativePath);
      PluginUtil.log(listener, "Copying the file [" + localFile + "] "
          + "to [" + targetFile + "] ..........");
      // this might be read-only
      if (targetFile.exists()) {
        targetFile.delete();
      }
      FileUtils.copyFile(localFile, targetFile);
    }
  }
}
