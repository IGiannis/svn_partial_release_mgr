package hudson.plugins.svn_partial_release_mgr.impl.functions.afterbuild;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild.Function2PartialPatchCreator;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;
import hudson.plugins.svn_partial_release_mgr.api.model.UserInput;
import hudson.plugins.svn_partial_release_mgr.impl.help.PomComparator;

/**
 * This implementation supposes that the application is a webapp with a specific directory tree
 *
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function2PartialPatchCreatorImpl implements Function2PartialPatchCreator {

  /**
   * This method will create a patch folder inside the deployment directory
   *
   * @param build             the job build info object
   * @param tagDeploymentInfo the json file info for the deployment
   * @param workspace         the workspace path of the job
   * @param listener          a place to send output
   */
  @Override
  public void createThePartialPatch(Run<?, ?> build,
                                    TagDeploymentInfo tagDeploymentInfo,
                                    FilePath workspace,
                                    TaskListener listener) throws IOException {
    UserInput userInput = tagDeploymentInfo.getUserInput();
    if (!userInput.isGeneratePartialPatch()) {
      return;
    }
    if (userInput.isTestBuild()) {
      return;
    }

    File buildNumberDeploymentDir = new File(
        build.getRootDir() + "/" + Constants.DIR_NAME_DEPLOYMENTS);
    if (!buildNumberDeploymentDir.exists()) {
      return;
    }

    // do copy the files from already check out source
    File workspaceDeploymentFolder = new File(
        PluginUtil.getWorkspaceTagDeploymentDatePath(workspace, tagDeploymentInfo));
    if (!workspaceDeploymentFolder.exists()) {
      FileUtils.forceMkdir(workspaceDeploymentFolder);
    }

    // do a full copy
    if (userInput.isGenerateSourcePartialPatch()) {
      copySrcDirectories(buildNumberDeploymentDir, workspaceDeploymentFolder, listener);
    }

    // get the target directory to get the classes from
    File buildTargetAppDirectory = resolveBuildTargetAppDirectory(tagDeploymentInfo, workspace);
    String tagName = tagDeploymentInfo.getTagName();

    // full partial patch
    createFullPatchPartial(workspace, tagName, buildNumberDeploymentDir,
        workspaceDeploymentFolder, buildTargetAppDirectory, listener);

    if (userInput.isGeneratePatchForEveryIssue()) {
      createPerIssuePatch(workspace, tagName, buildNumberDeploymentDir,
          workspaceDeploymentFolder, buildTargetAppDirectory, listener);
    }
  }

  /**
   * Returns the directory file for the target/webapp_name in order to find the compile resources into
   *
   * @param tagDeploymentInfo the json file info for the deployment
   * @param workspace         the workspace path of the job
   * @return the directory file for the target/webapp_name
   */
  protected File resolveBuildTargetAppDirectory(TagDeploymentInfo tagDeploymentInfo,
                                                FilePath workspace) throws IOException {
    String buildRootDirectory = PluginUtil.getWorkspaceTagBuildRootDirectory(workspace,
        tagDeploymentInfo);
    String buildTargetDirectory = buildRootDirectory + "/" + Constants.DIR_NAME_TARGET;
    File targetDir = new File(buildTargetDirectory);
    if (!targetDir.exists()) {
      throw new IOException("Target directory [" + buildTargetDirectory + "] does not exist!!");
    }
    String warName = getWarName(targetDir);
    return new File(targetDir, warName);
  }

  protected String getWarName(File targetDir) {
    File[] warFiles = targetDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir,
                            String name) {
        return name.endsWith(".war");
      }
    });
    if (warFiles == null || warFiles.length <= 0) {
      return null;
    }
    return FilenameUtils.removeExtension(warFiles[0].getName());
  }

  /**
   * Copies the source patches into the deployment directory
   *
   * @param buildNumberDeploymentDir  the directory to get the files from
   * @param workspaceDeploymentFolder the directory to save the files to
   * @param listener                  a place to send output
   */
  protected void copySrcDirectories(File buildNumberDeploymentDir,
                                    File workspaceDeploymentFolder,
                                    TaskListener listener) throws IOException {
    PluginUtil.log(listener, "Copying the directory "
        + "[" + buildNumberDeploymentDir.getAbsolutePath() + "] "
        + "to [" + workspaceDeploymentFolder + "].............");
    FileUtils.copyDirectory(buildNumberDeploymentDir, workspaceDeploymentFolder, new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    }, true);
  }

  /**
   * Creates a directory /deployments/date/full_patch/patch and creates the partial patch into it
   * (ready to deploy)
   *
   * @param workspace                 the workspace path of the job
   * @param tagName                   the name of the tag we are building the patch for
   * @param buildNumberDeploymentDir  the build number deployment folder to get the files from
   * @param workspaceDeploymentFolder the deployments folder
   * @param buildTargetAppDirectory   the target webapp folder to get the classes from
   * @param listener                  a place to send output
   */
  protected void createFullPatchPartial(FilePath workspace,
                                        String tagName,
                                        File buildNumberDeploymentDir,
                                        File workspaceDeploymentFolder,
                                        File buildTargetAppDirectory,
                                        TaskListener listener) throws IOException {
    // full patch
    File originPatchParentDir = new File(buildNumberDeploymentDir, Constants.PATCH_DIR_NAME_FULL);
    File targetPatchDir = new File(workspaceDeploymentFolder, Constants.PATCH_DIR_NAME_FULL);
    createPartialPatch(workspace, tagName, originPatchParentDir,
        buildTargetAppDirectory, targetPatchDir, listener);
  }

  /**
   * Creates a directory /deployments/date/issues_patch/NUMBER/patch and creates the partial patch into it
   * (ready to deploy)
   *
   * @param workspace                 the workspace path of the job
   * @param tagName                   the name of the tag we are building the patch for
   * @param buildNumberDeploymentDir  the build number deployment folder to get the files from
   * @param workspaceDeploymentFolder the directory to save the files to
   * @param buildTargetAppDirectory   the target webapp folder to get the classes from
   * @param listener                  a place to send output
   */
  protected void createPerIssuePatch(FilePath workspace,
                                     String tagName,
                                     File buildNumberDeploymentDir,
                                     File workspaceDeploymentFolder,
                                     File buildTargetAppDirectory,
                                     TaskListener listener) throws IOException {
    // per source patch
    File buildPerIssuePatchDir = new File(buildNumberDeploymentDir,
        Constants.PATCH_DIR_NAME_PER_ISSUE);
    if (!buildPerIssuePatchDir.exists()) {
      return;
    }
    File[] issueDirectories = buildPerIssuePatchDir.listFiles();
    if (issueDirectories == null || issueDirectories.length <= 0) {
      return;
    }
    for (File issuePatchDirectory : issueDirectories) {
      if (!issuePatchDirectory.isDirectory()) {
        continue;
      }
      String targetRelPath = Constants.PATCH_DIR_NAME_PER_ISSUE +
          "/" + issuePatchDirectory.getName();
      File targetPatchDir = new File(workspaceDeploymentFolder, targetRelPath);
      FileUtils.forceMkdir(targetPatchDir);
      createPartialPatch(workspace, tagName, issuePatchDirectory,
          buildTargetAppDirectory, targetPatchDir, listener);
    }
  }

  /**
   * Copies the resources into the target classes directory
   * and the respective classes for the input java source files into the target classes directory
   *
   * @param workspace               the workspace path of the job
   * @param tagName                 the name of the tag we are building the patch for
   * @param originPatchParentDir    the parent patch directory
   * @param buildTargetAppDirectory the target webapp folder to get the classes from
   * @param targetParentPatchDir    the target directory were the patch will be saved to
   * @param listener                a place to send output
   */
  protected void createPartialPatch(FilePath workspace,
                                    String tagName,
                                    File originPatchParentDir,
                                    File buildTargetAppDirectory,
                                    File targetParentPatchDir,
                                    TaskListener listener) throws IOException {
    File originPatchSrcDir = new File(originPatchParentDir, Constants.PATCH_DIR_NAME_SRC);
    File originMainDir = new File(originPatchSrcDir.getAbsolutePath() +
        "/" + Constants.DIR_NAME_SRC +
        "/" + Constants.DIR_NAME_MAIN);

    File patchDir = new File(targetParentPatchDir, Constants.PATCH_DIR_NAME);
    File webappTargetDir = copyOrBuildWebappDirectory(originMainDir, patchDir);
    copyResourcesIntoWebInfClasses(originMainDir, webappTargetDir);
    copyClassesToWebInf(originMainDir, buildTargetAppDirectory,
        webappTargetDir, listener);
    copyUpdatedJars(workspace, tagName, originPatchSrcDir,
        buildTargetAppDirectory, webappTargetDir, listener);
  }

  /**
   * Checks if a webapp folder exists into the origin directory and copies it into the target
   * or creates a new empty folder
   *
   * @param originMainDir the origin directory to get files from
   * @param targetDir     the target directory to copy file to
   * @return the webapp file object
   */
  protected File copyOrBuildWebappDirectory(File originMainDir,
                                            File targetDir) throws IOException {
    File webappDir = new File(targetDir, Constants.DIR_NAME_WEBAPP);
    if (!webappDir.exists()) {
      FileUtils.forceMkdir(webappDir);
    }
    File webappSourceDir = new File(originMainDir, Constants.DIR_NAME_WEBAPP);
    if (!webappSourceDir.exists()) {
      return webappDir;
    }
    FileUtils.copyDirectory(webappSourceDir, webappDir);
    return webappDir;
  }

  /**
   * Moves all the files from the resources into the web-inf classes
   *
   * @param originMainDir   the origin directory to get files from
   * @param webappTargetDir the target directory to copy file to
   */
  protected void copyResourcesIntoWebInfClasses(File originMainDir,
                                                File webappTargetDir) throws IOException {
    File resourcesDir = new File(originMainDir, Constants.DIR_NAME_RESOURCES);
    if (!resourcesDir.exists()) {
      return;
    }
    File classesDir = createOutputClassesDir(webappTargetDir);
    FileUtils.copyDirectory(resourcesDir, classesDir);
  }

  /**
   * Creates a new WEB-INF/classes directory to copy the classes and resources into
   *
   * @param rootDir the target directory to copy file to
   * @return the newly created directory
   */
  protected File createOutputClassesDir(File rootDir) throws IOException {
    String classesDirPath = toOutputClassesDirPath(rootDir);
    File classesDir = new File(classesDirPath);
    FileUtils.forceMkdir(classesDir);
    return classesDir;
  }

  /**
   * Creates a new WEB-INF/classes directory to copy the classes and resources into
   *
   * @param rootDir the target directory to copy file to
   * @return the newly created directory
   */
  protected File createOutputLibDir(File rootDir) throws IOException {
    String libDirPath = toOutputLibDirPath(rootDir);
    File libDir = new File(libDirPath);
    FileUtils.forceMkdir(libDir);
    return libDir;
  }

  /**
   * Creates a new WEB-INF/classes directory to copy the classes and resources into
   *
   * @param rootDir the target directory to copy file to
   * @return the newly created directory
   */
  protected String toOutputClassesDirPath(File rootDir) throws IOException {
    return FilenameUtils.separatorsToUnix(rootDir.getAbsolutePath() +
        "/" + Constants.DIR_NAME_WEBINF + "/" + Constants.DIR_NAME_CLASSES);
  }

  /**
   * Creates a new WEB-INF/lib directory to copy the jars into
   *
   * @param rootDir the target directory to copy file to
   * @return the newly created directory
   */
  protected String toOutputLibDirPath(File rootDir) throws IOException {
    return FilenameUtils.separatorsToUnix(rootDir.getAbsolutePath() +
        "/" + Constants.DIR_NAME_WEBINF + "/" + Constants.DIR_NAME_LIB);
  }

  /**
   * Checks if the patch has a pom file, compares it to the tags pom and updates the jars
   *
   * @param workspace               the workspace path of the job
   * @param tagName                 the name of the tag we are building the patch for
   * @param originPatchParentDir    the origin directory to get files from
   * @param buildTargetAppDirectory the target webapp folder to get the classes from
   * @param webappTargetDir         the target directory to copy file to
   * @param listener                a place to send output
   */
  protected void copyUpdatedJars(FilePath workspace,
                                 String tagName,
                                 File originPatchParentDir,
                                 File buildTargetAppDirectory,
                                 File webappTargetDir,
                                 TaskListener listener) throws IOException {
    File patchPomFile = new File(originPatchParentDir, Constants.POM_XML_FILE_NAME);
    if (!patchPomFile.exists()) {
      return;
    }
    PluginUtil.log(listener, "Will try to update the library jars.........");
    String tagPomFilePath = PluginUtil.getFullPathToTagBackupSource(workspace.getRemote(),
        tagName) + "/" + Constants.POM_XML_FILE_NAME;
    File tagPomFile = new File(tagPomFilePath);
    if (!tagPomFile.exists()) {
      throw new IOException("Pom file not found in loaction [" + tagPomFilePath + "]");
    }
    PomComparator pomComparator = new PomComparator(tagPomFile, patchPomFile);
    List<String> updatedJarNames = pomComparator.resolveUpdatedDependencies();
    if (updatedJarNames == null || updatedJarNames.isEmpty()) {
      PluginUtil.log(listener, "No jars found to update .........");
      return;
    }

    for (String jarFileName : updatedJarNames) {
      String buildLibDirPath = toOutputLibDirPath(buildTargetAppDirectory);
      String inJarFilePath = FilenameUtils.separatorsToUnix(buildLibDirPath +
          "/" + jarFileName);
      File jarFile = new File(inJarFilePath);
      if (!jarFile.exists()) {
        PluginUtil
            .log(listener, "WARNING!!!! Jar [" + inJarFilePath + "] file does not exist.");
      } else {
        File libOutputDir = createOutputLibDir(webappTargetDir);
        String outJarFilePath = FilenameUtils.separatorsToUnix(libOutputDir +
            "/" + jarFileName);
        PluginUtil.log(listener, "Copying file from [" + inJarFilePath + "] "
            + "to [" + outJarFilePath + "]");
        File outputJarFile = new File(outJarFilePath);
        FileUtils.copyFile(jarFile, outputJarFile);
      }
    }
  }


  /**
   * Gets all the files into the java directory copies the relative class
   * from the maven build directory ( and the sub-classes ) and moves them into the target directory
   *
   * @param originMainDir           the origin directory to get files from
   * @param buildTargetAppDirectory the target webapp folder to get the classes from
   * @param webappTargetDir         the target directory to copy file to
   * @param listener                a place to send output
   */
  protected void copyClassesToWebInf(File originMainDir,
                                     File buildTargetAppDirectory,
                                     File webappTargetDir,
                                     TaskListener listener) throws IOException {
    if (originMainDir == null || !originMainDir.exists()) {
      return;
    }
    String originJavaDir = FilenameUtils.separatorsToUnix(originMainDir.getAbsolutePath() +
        "/" + Constants.DIR_NAME_JAVA);
    File originJavaDirFile = new File(originJavaDir);
    if (!originJavaDirFile.exists()) {
      return;
    }
    List<String> javaFiles = getJavaFileRelativePaths(originJavaDir, originJavaDirFile, null);
    if (javaFiles == null || javaFiles.isEmpty()) {
      return;
    }
    for (String javaFilePath : javaFiles) {
      String fileClassPath = javaFilePath.substring(0, javaFilePath.lastIndexOf("."))
          .concat(Constants.CLASS_EXTENSION);
      String buildClassesDirPath = toOutputClassesDirPath(buildTargetAppDirectory);
      String inClassFilePath = FilenameUtils.separatorsToUnix(buildClassesDirPath +
          "/" + fileClassPath);
      File fileClass = new File(inClassFilePath);
      if (!fileClass.exists()) {
        PluginUtil
            .log(listener, "WARNING!!!! Class [" + inClassFilePath + "] file does not exist.");
      } else {
        File patchClassesDir = createOutputClassesDir(webappTargetDir);
        String outClassFilePath = FilenameUtils.separatorsToUnix(patchClassesDir +
            "/" + fileClassPath);
        PluginUtil.log(listener, "Copying file from [" + inClassFilePath + "] "
            + "to [" + outClassFilePath + "]");
        File outputClassFile = new File(outClassFilePath);
        FileUtils.copyFile(fileClass, outputClassFile);
        copyAllSubClasses(fileClass, outputClassFile, listener);
      }
    }
  }

  /**
   * Copies the sub-classes of the original input class file
   *
   * @param fileClass       the class file to get the sub-classes for
   * @param outputClassFile the original output class to get the folder to save sub-classes into
   * @param listener        a place to send output
   */
  protected void copyAllSubClasses(File fileClass,
                                   File outputClassFile,
                                   TaskListener listener) throws IOException {
    File dirClass = fileClass.getParentFile();
    if (!dirClass.exists()) {
      return;
    }
    File[] filesInDirectory = dirClass.listFiles();
    if (filesInDirectory == null || filesInDirectory.length <= 0) {
      return;
    }
    String className = fileClass.getName();
    className = className.substring(0, className.lastIndexOf("."));
    String nameToSearch = className + "$";
    for (File file : filesInDirectory) {
      if (!file.isFile()) {
        continue;
      }
      if (!file.getName().startsWith(nameToSearch)) {
        continue;
      }
      String outClassFilePath = FilenameUtils.separatorsToUnix(
          outputClassFile.getParentFile() + "/" + file.getName());
      PluginUtil.log(listener, "Copying file from[" + file.getAbsolutePath() + "]"
          + "to [" + outClassFilePath + "]");
      FileUtils.copyFile(file, new File(outClassFilePath));
    }
  }

  /**
   * Collects all the java files relative paths into a list in order to get the classes from the compiled
   *
   * @param rootUnixPath  the root unix path
   * @param directory     the directory to get the files from
   * @param modifiedFiles the final list of java files
   * @return the list of the java file relative paths
   */
  protected List<String> getJavaFileRelativePaths(String rootUnixPath,
                                                  File directory,
                                                  List<String> modifiedFiles) throws IOException {

    File[] list = directory.listFiles();
    if (list == null || list.length <= 0) {
      return modifiedFiles;
    }
    for (File vFile : list) {
      if (vFile.isDirectory()) {
        modifiedFiles = getJavaFileRelativePaths(rootUnixPath, vFile, modifiedFiles);
        continue;
      }
      String unixPath = FilenameUtils.separatorsToUnix(vFile.getAbsolutePath());
      String unixOriginPath = FilenameUtils.separatorsToUnix(rootUnixPath);
      //System.out.println("Found file ["+vFile.getName()+"] in ["+inputDir+"]");
      if (modifiedFiles == null) {
        modifiedFiles = new ArrayList<>();
      }
      modifiedFiles.add(unixPath.substring(unixOriginPath.length(), unixPath.length()));
    }
    return modifiedFiles;
  }
}
