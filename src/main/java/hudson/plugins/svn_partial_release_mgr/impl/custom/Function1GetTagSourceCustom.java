package hudson.plugins.svn_partial_release_mgr.impl.custom;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function1GetTagSourceImpl;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function1GetTagSourceCustom extends Function1GetTagSourceImpl {

  public static final String ENVIRONMENT_DIR_NAME = "environments";
  public static final String SRC_DIR_NAME = "src";
  public static final String POM_XML_FILE_NAME = "pom.xml";

  /**
   * Overridden in order not to copy the all tag source but only the directories needed for the build
   *
   * @param backupDir the directory were the checkout source directory will be backed up
   * @param buildDir  the directory were the maven build will take place
   */
  @Override
  protected void copyTheBackupSource(File backupDir,
                                     File buildDir,
                                     TaskListener listener) throws IOException {
    // copy the tag
    PluginUtil.log(listener, "Custom code getting the backup src files..........");

    File envTagDir = new File(backupDir, ENVIRONMENT_DIR_NAME);
    File envDestDir = new File(buildDir, ENVIRONMENT_DIR_NAME);
    FileUtils.forceMkdir(envDestDir);
    FileUtils.copyDirectory(envTagDir, envDestDir);

    File srcTagDir = new File(backupDir, SRC_DIR_NAME);
    File srcDestDir = new File(buildDir, SRC_DIR_NAME);
    FileUtils.forceMkdir(srcDestDir);
    FileUtils.copyDirectory(srcTagDir, srcDestDir);

    // copy the pom.xml file
    File srcPomFile = new File(backupDir, POM_XML_FILE_NAME);
    File srcDest = new File(buildDir, POM_XML_FILE_NAME);
    FileUtils.copyFile(srcPomFile, srcDest);
  }
}
