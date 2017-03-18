package hudson.plugins.svn_partial_release_mgr.api.constants;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Constants {
  public static final String ENCODING_UTF8 = "UTF-8";
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  public static final String TITLE = "PARTIAL-RELEASE-MGR";
  public static final String LOG_PREFIX = TITLE + " TOOL : ";
  public static final String ACTION_NAME = "Svn-Partial Release Manager";

  public static final String ENV_PARAM_RELEASE_REVISIONS = "RELEASE_REVISIONS";
  public static final String ENV_PARAM_EXTRA_INPUT = "EXTRA_INPUT";
  public static final String SYSTEM_PROPERTY_BUILD_STARTED = "SYSTEM_PROPERTY_BUILD_STARTED";
  public static final String ENV_PARAM_GENERATE_PARTIAL_PATCH = "GENERATE_PARTIAL_PATCH";
  public static final String ENV_PARAM_GENERATE_SRC_PARTIAL_PATCH = "GENERATE_SRC_PARTIAL_PATCH";
  public static final String ENV_PARAM_GENERATE_PATCH_FOR_EVERY_ISSUE =
      "GENERATE_PATCH_FOR_EVERY_ISSUE";
  public static final String ENV_PARAM_IS_FAST_BUILD = "IS_FAST_BUILD";
  public static final String ENV_PARAM_IS_TEST_BUILD = "IS_TEST_BUILD";
  public static final String ENV_PARAM_INCLUDE_PREV_PATCH_SOURCES = "INCLUDE_PREV_PATCH_SOURCES";

  public static final String CLASS_EXTENSION = ".class";
  public static final String DIR_NAME_DEPLOYMENTS = "deployments";
  public static final String DIR_NAME_RESOURCES = "resources";
  public static final String DIR_NAME_WEBAPP = "webapp";
  public static final String DIR_NAME_WEBINF = "WEB-INF";
  public static final String DIR_NAME_CLASSES = "classes";
  public static final String DIR_NAME_LIB = "lib";
  public static final String DIR_NAME_JAVA = "java";
  public static final String DIR_NAME_CHECKOUT = "checkout";
  public static final String DIR_NAME_REVISIONS = "revisions";
  public static final String DIR_NAME_BUILDS = "build";
  public static final String DIR_NAME_TAGS = "tags";
  public static final String DIR_NAME_SRC = "src";
  public static final String DIR_NAME_MAIN = "main";
  public static final String DIR_NAME_TARGET = "target";
  public static final String DEPLOYMENT_INFO_XML_FILE_NAME = "deploymentInfo.xml";

  public static final Pattern URL_PATTERN =
      Pattern.compile("(https?|svn(\\+[a-z0-9]+)?|file)://.+");
  public static final String DEPLOY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

  public static final String DEPLOY_DATE_FORMAT_FILE = "yyyyMMdd_HHmmss";
  public static final String PATCH_DIR_NAME_FULL = "full_patch";
  public static final String PATCH_DIR_NAME_SRC = "patch-src";
  public static final String PATCH_DIR_NAME = "patch";
  public static final String PATCH_DIR_NAME_PER_ISSUE = "issues_patch";


  public static final String DEFAULT_ISSUE_PREFIXES = "PBBPMG-,VGR-";
  public static final String CONFIGURATION_PROPERTIES_FILE = "configuration.properties";

  public static final String UI_TABLE_COLOR_ROW_SEPARATOR = "lightgrey";
  public static final String UI_TABLE_COLOR_ALREADY_DEPLOYED = "#EBE8E8";
  public static final String POM_XML_FILE_NAME = "pom.xml";

  public static final Map<String,String> additionalUserInputParameters = new HashMap<String,String>(){
    {
      put(Constants.ENV_PARAM_GENERATE_PARTIAL_PATCH,"generatePartialPatch");
      put(Constants.ENV_PARAM_GENERATE_SRC_PARTIAL_PATCH,"generateSourcePartialPatch");
      put(Constants.ENV_PARAM_GENERATE_PATCH_FOR_EVERY_ISSUE,"generatePatchForEveryIssue");
      put(Constants.ENV_PARAM_IS_FAST_BUILD,"isFastBuild");
      put(Constants.ENV_PARAM_IS_TEST_BUILD,"isTestBuild");
      put(Constants.ENV_PARAM_INCLUDE_PREV_PATCH_SOURCES,"includePreviousPatchSources");
    }
  };

  public static final Properties XML_OUTPUT_PROPERTIES = new Properties() {
    {
      put(OutputKeys.ENCODING, Constants.ENCODING_UTF8);
      put(OutputKeys.INDENT, 4);
      put(OutputKeys.METHOD, "xml");
      put(OutputKeys.DOCTYPE_PUBLIC, "yes");
    }
  };

  public static final class XmlFileFilter implements FileFilter {
    @Override
    public boolean accept(File dir) {
      return (dir.isFile() && dir.getName().equalsIgnoreCase(DEPLOYMENT_INFO_XML_FILE_NAME));
    }
  }
}
