package hudson.plugins.svn_partial_release_mgr.api.constants;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.svn_partial_release_mgr.api.model.TagDeploymentInfo;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class PluginUtil {
  public static final Properties configurationProperties = loadConfigurationProperties();

  /**
   * Reads a property list (key and element pairs) from the resources
   */
  private static Properties loadConfigurationProperties() {
    InputStream fis = null;
    try {
      fis = PluginUtil.class.getResourceAsStream("/" + Constants.CONFIGURATION_PROPERTIES_FILE);
      if (fis == null) {
        return null;
      }
      Properties applicationProperties = new Properties();
      applicationProperties.load(fis);
      return applicationProperties;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  /**
   * Returns a string configuration value that has been set for this configuration key
   *
   * @param property : the name of the configuration key we are looking for
   * @return the configuration value for this key
   */
  public static String getConfiguration(String property) {
    return configurationProperties != null ? configurationProperties.getProperty(property) : null;
  }

  public static String pathToTag(SVNURL svnurl,
                                 String tagName) {
    return pathToTagWithSvnUrl(svnurl.toDecodedString(), tagName);
  }

  public static String pathToTagWithSvnUrl(String svnurlFullPath,
                                           String tagName) {
    String pathToTag = pathToTag("trunk", svnurlFullPath, tagName);
    if (!StringUtils.isBlank(pathToTag)) {
      return pathToTag;
    }
    return pathToTag("branches", svnurlFullPath, tagName);
  }

  protected static String pathToTag(String prefix,
                                    String fullPath,
                                    String tagName) {
    if (!fullPath.contains(prefix)) {
      return null;
    }
    fullPath = fullPath.substring(0, fullPath.lastIndexOf(prefix));
    fullPath = fullPath + Constants.DIR_NAME_TAGS + "/" + tagName;
    return fullPath;
  }

  public static void log(TaskListener listener,
                         String message) {
    String dateString = "[" +
        DateFormatUtils.format(new Date(), Constants.DEPLOY_DATE_FORMAT) + "]";
    listener.getLogger().println(dateString + " " + Constants.LOG_PREFIX + message);
  }

  public static String getWorkspaceDeploymentPath(String workspaceRootPath,
                                                  String tagName) {
    return FilenameUtils.separatorsToUnix(workspaceRootPath +
        "/" + Constants.DIR_NAME_DEPLOYMENTS + "/" + tagName);
  }

  public static String getWorkspaceBuildRootPath(String workspaceRootPath,
                                                 String tagName) {
    return FilenameUtils.separatorsToUnix(workspaceRootPath +
        "/" + Constants.DIR_NAME_BUILDS + "/" + tagName);
  }

  public static String getFullPathToTagBackupSource(String workspaceRootPath,
                                                    String tagName) {
    return FilenameUtils.separatorsToUnix(workspaceRootPath + "/" +
        getRelativePathToTagBackupSource(tagName));
  }

  public static String getRelativePathToTagBackupSource(String tagName) {
    return Constants.DIR_NAME_CHECKOUT + "/" + Constants.DIR_NAME_TAGS + "/" + tagName;
  }

  public static String getWorkspaceTagDeploymentDatePath(FilePath workspace,
                                                         TagDeploymentInfo tagDeploymentInfo) throws
      IOException {
    String deploymentWorkspaceDirPath = PluginUtil.getWorkspaceDeploymentPath(workspace.getRemote(),
        tagDeploymentInfo.getTagName());
    String dirDateName = DateFormatUtils.format(parse(tagDeploymentInfo.getDeploymentDate(),
        Constants.DEPLOY_DATE_FORMAT), Constants.DEPLOY_DATE_FORMAT_FILE);
    return FilenameUtils.separatorsToUnix(deploymentWorkspaceDirPath +
        "/" + dirDateName);
  }

  public static String getWorkspaceTagBuildRootDirectory(FilePath workspace,
                                                         TagDeploymentInfo tagDeploymentInfo) throws
      IOException {
    return PluginUtil.getWorkspaceBuildRootPath(workspace.getRemote(),
        tagDeploymentInfo.getTagName());
  }

  public static Date parse(String dateString,
                           String dateFormat) throws IOException {
    try {
      return new SimpleDateFormat(dateFormat).parse(dateString);
    } catch (ParseException e) {
      throw new IOException("Could not parse the date [" + dateString + "] "
          + "with format [" + dateFormat + "]");
    }
  }

  /**
   * Returns all the files that satisfy the filter criteria wherever (in any replace-directory) they are inside this folder
   *
   * @param parentDir : the parent directory
   * @param filter    : the files filter
   * @return a list of all the available files
   */
  public static List<File> getAllFilesInAllSubDirectories(File parentDir,
                                                          FileFilter filter) {
    List<File> filesList = null;
    return getAllFilesInAllSubDirectories(filesList, parentDir, filter);
  }

  /**
   * Returns all the files that satisfy the filter criteria wherever (in any replace-directory) they are inside this folder
   *
   * @param filesList : the list to attach the files into
   * @param parentDir : the parent directory
   * @param filter    : the files filter
   * @return a list of all the available files
   */
  public static List<File> getAllFilesInAllSubDirectories(List<File> filesList,
                                                          File parentDir,
                                                          FileFilter filter) {
    if (parentDir == null || !parentDir.exists()) {
      return filesList;
    }
    File[] files = parentDir.listFiles(filter);
    if (files != null) {
      for (File vFile : files) {
        if (filesList == null) {
          filesList = new ArrayList<>();
        }
        filesList.add(vFile);
      }
    }
    File[] directories = parentDir.listFiles();
    if (directories != null) {
      for (File vDirectory : directories) {
        filesList = getAllFilesInAllSubDirectories(filesList, vDirectory, filter);
      }
    }
    return filesList;
  }

  /**
   * Puts an object to an inner Set of the given Map ( initializes it first if null )
   *
   * @param mapValues : the Map to insert the object into
   * @param key       : the key under which the object is to be inserted
   * @param obj       : the Object to be inserted
   * @return the updated Map
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, Set<V>> putCheckedObjectInInnerSet(Map<K, Set<V>> mapValues,
                                                                 K key,
                                                                 V obj) {
    Set<V> innerSet = null;
    if (mapValues != null && mapValues.containsKey(key)) {
      innerSet = mapValues.get(key);
    }
    innerSet = addCheckedObjectInSet(innerSet, obj);
    mapValues = addCheckedObjectInMap(mapValues, key, innerSet);
    return mapValues;
  }

  /**
   * Adds an object to a given Set ( initializes it first if null )
   *
   * @param existingObjectsSet : the Set to insert the object into
   * @param obj                : the Object to be inserted
   * @return the updated list
   */
  @SuppressWarnings("unchecked")
  public static <T, O extends T> Set<T> addCheckedObjectInSet(Set<T> existingObjectsSet,
                                                              O obj) {
    if (obj != null) {
      if (existingObjectsSet == null) {
        existingObjectsSet = new HashSet<>();
      }
      existingObjectsSet.add(obj);
    }
    return existingObjectsSet;
  }

  /**
   * Puts an object to a given Map ( initializes it first if null )
   *
   * @param mapValues : the Map to insert the object into
   * @param key       : the key under which the object is to be inserted
   * @param obj       : the Object to be inserted
   * @return the updated Map
   */
  @SuppressWarnings("unchecked")
  public static <K, V, O extends V> Map<K, V> addCheckedObjectInMap(Map<K, V> mapValues,
                                                                    K key,
                                                                    O obj) {
    if (obj != null) {
      if (mapValues == null) {
        mapValues = new HashMap<>();
      }
      mapValues.put(key, obj);
    }
    return mapValues;
  }

  /**
   * Puts an object to an inner Map of the given Map ( initializes it first if null )
   *
   * @param mapValues : the Map to insert the object into
   * @param key       : the key under which the object is to be inserted
   * @param innerKey  : the key under which the object is to be inserted
   * @param obj       : the Object to be inserted
   * @return the updated Map
   */
  @SuppressWarnings("unchecked")
  public static <A, K, V> Map<A, Map<K, V>> putCheckedObjectInInnerMap(Map<A, Map<K, V>> mapValues,
                                                                       A key,
                                                                       K innerKey,
                                                                       V obj) {
    Map<K, V> innerMap = null;
    if (mapValues != null && mapValues.containsKey(key)) {
      innerMap = mapValues.get(key);
    }
    innerMap = addCheckedObjectInMap(innerMap, innerKey, obj);
    mapValues = addCheckedObjectInMap(mapValues, key, innerMap);
    return mapValues;
  }

  /**
   * Flag to check if we should make the job disabled
   *
   * @return true if the job has been started
   */
  public static boolean isJobStarted() {
    String systemFlag = System.getProperty(Constants.SYSTEM_PROPERTY_BUILD_STARTED);
    return Boolean.TRUE.toString().equalsIgnoreCase(systemFlag);
  }

  /**
   * Flag to check if we should make the job disabled
   */
  public static void setJobStarted() {
    System.setProperty(Constants.SYSTEM_PROPERTY_BUILD_STARTED,
        String.valueOf(Boolean.TRUE));
  }

  /**
   * Flag to check if we should make the job disabled
   */
  public static void setJobEnded() {
    System.setProperty(Constants.SYSTEM_PROPERTY_BUILD_STARTED,
        String.valueOf(Boolean.FALSE));
  }

  /**
   * Returns the method declared in the given class or in any superclass
   *
   * @param clazz       : the class object to execute the method of
   * @param methodName  : the method name of the class object to execute
   * @param classParams : the parameter objects array
   * @return the reflect method outorge object
   */
  public static Method getReflectionMethodInAnySuperClass(Class clazz,
                                                          String methodName,
                                                          Class[] classParams) {
    Method mainMethod = getClassMethod(clazz, methodName, classParams);
    while (mainMethod == null) {
      Class superClass = clazz.getSuperclass();
      mainMethod = getClassMethod(superClass, methodName, classParams);
    }
    return mainMethod;
  }

  /**
   * Returns the method declared in the given class ( and not in any superclass )
   *
   * @param clazz       : the class object to execute the method of
   * @param methodName  : the method name of the class object to execute
   * @param classParams : the parameter objects array
   * @return the reflect method outorge object
   */
  protected static Method getClassMethod(Class clazz,
                                         String methodName,
                                         Class[] classParams) {
    try {
      return clazz.getDeclaredMethod(methodName, classParams);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * Calls a method of a given object by reflection
   *
   * @param instanceOfClass : the class object to execute the method of
   * @param methodName      : the method name of the class object to execute
   * @param paramClasses    : the parameter classes array
   * @param paramObjects    : the parameter objects array
   * @return the method outorge object
   */
  public static Object reflectionCallMethod(Object instanceOfClass,
                                            String methodName,
                                            Class[] paramClasses,
                                            Object[] paramObjects) throws Exception {
    return reflectionCallMethod(instanceOfClass, methodName, paramClasses, paramObjects, true);
  }

  /**
   * Calls a method of a given object by reflection
   *
   * @param instanceOfClass : the class object to execute the method of
   * @param methodName      : the method name of the class object to execute
   * @param paramClasses    : the parameter classes array
   * @param paramObjects    : the parameter objects array
   * @return the method outorge object
   */
  public static Object reflectionCallMethod(Object instanceOfClass,
                                            String methodName,
                                            Class[] paramClasses,
                                            Object[] paramObjects,
                                            boolean searchSuperClasses) throws Exception {
    // look up its main(String[]) method
    Method mainMethod = searchSuperClasses ?
        getReflectionMethodInAnySuperClass(instanceOfClass.getClass(), methodName, paramClasses) :
        getClassMethod(instanceOfClass.getClass(), methodName, paramClasses);
    if (mainMethod == null) {
      throw new NoSuchMethodException("Error! " + instanceOfClass.getClass().getName() + " " +
          "class does not contain a [" + methodName + "] method.");
    }
    // run the main(String[]) method with the given arguments
    return mainMethod.invoke(instanceOfClass, paramObjects);
  }

  /**
   * Compares a string against another one after eliminating the null pointer exception
   *
   * @param s      the String to be compared
   * @param target the String to be compared against
   * @return the cropped String
   */
  public static boolean isEqualsSafe(String s,
                                     String target) {
    return isEqualsSafe(s, target, false);
  }

  /**
   * Compares a string against another one after eliminating the null pointer exception
   *
   * @param s          the String to be compared
   * @param target     the String to be compared against
   * @param ignoreCase if true the equal comparator will ignore the case
   * @return the cropped String
   */
  public static boolean isEqualsSafe(String s,
                                     String target,
                                     boolean ignoreCase) {
    if (s == null && target == null) {
      return true;
    }
    if (s == null) {
      return false;
    }
    return ignoreCase ? s.equalsIgnoreCase(target) : s.equals(target);
  }
}
