package hudson.plugins.svn_partial_release_mgr.impl.help;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class PomComparator {
  public static final String PROPERTY_PREFIX = "${";
  public static final String PROPERTY_SUFFIX = "}";

  protected final Model oldPomModel;
  protected final Model newPomModel;

  protected List<String> updatedJars;

  public PomComparator(File oldPomFile,
                       File newPomFile) throws IOException {
    try {
      this.oldPomModel = toPomModel(oldPomFile);
      this.newPomModel = toPomModel(newPomFile);

    } catch (Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new IOException("Error while trying to compare the pom files"
          + " [" + oldPomFile.getAbsolutePath() + "],[" + newPomFile.getAbsolutePath() + "]!!" +
          ExceptionUtils.getStackTrace(e));
    }
  }

  public List<String> resolveUpdatedDependencies() {
    List<Dependency> newPomModelDependencies = newPomModel.getDependencies();
    if (newPomModelDependencies == null || newPomModelDependencies.isEmpty()) {
      return null;
    }
    List<String> modifiedDependencies = new ArrayList<>();
    List<Dependency> oldPomModelDependencies = oldPomModel.getDependencies();
    for (Dependency newDependency : newPomModelDependencies) {
      if (isDependencyProvided(newDependency)) {
        continue;
      }
      Dependency dependencyToCompareWith = findDependencyToCompareWith(newDependency,
          oldPomModelDependencies);
      boolean isDependencyUpdated = isDependencyUpdated(newDependency, dependencyToCompareWith);
      if (isDependencyUpdated) {
        modifiedDependencies.add(toJarFileName(newDependency));
      }
    }
    return modifiedDependencies;
  }

  protected String toJarFileName(Dependency updatedDependency) {
    StringBuilder jarFileName = new StringBuilder(
        getPomValue(updatedDependency.getArtifactId(),newPomModel));
    String version = getPomValue(updatedDependency.getVersion(),newPomModel);
    if (!StringUtils.isBlank(version)) {
      jarFileName.append("-").append(version);
    }
    String extension = !StringUtils.isBlank(updatedDependency.getType()) ?
        "." + updatedDependency.getType() : ".jar";
    jarFileName.append(extension);
    return jarFileName.toString();
  }

  protected boolean isDependencyProvided(Dependency newDependency) {
    return "provided".equalsIgnoreCase(newDependency.getScope());
  }

  protected boolean isDependencyUpdated(Dependency newDependency,
                                        Dependency dependencyToCompareWith) {
    if (dependencyToCompareWith == null) {
      return true;
    }
    return isDifferent(dependencyToCompareWith.getVersion(), oldPomModel,
        newDependency.getVersion(), newPomModel);
  }

  protected Dependency findDependencyToCompareWith(Dependency newDependency,
                                                   List<Dependency> oldPomModelDependencies) {
    for (Dependency oldPomModelDependency : oldPomModelDependencies) {
      if (isDifferent(oldPomModelDependency.getGroupId(), oldPomModel,
          newDependency.getGroupId(), newPomModel)) {
        continue;
      }
      if (isDifferent(oldPomModelDependency.getArtifactId(), oldPomModel,
          newDependency.getArtifactId(), newPomModel)) {
        continue;
      }
      return oldPomModelDependency;
    }
    return null;
  }

  protected Model toPomModel(File pomXmlFile) throws Exception {
    Reader reader = null;
    try {
      reader = new FileReader(pomXmlFile);
      MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
      return xpp3Reader.read(reader);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }


  public static boolean isDifferent(String s,
                                    Model pomModel,
                                    String target,
                                    Model targetModel) {
    return !PluginUtil.isEqualsSafe(getPomValue(s, pomModel),
        getPomValue(target, targetModel));
  }

  protected static String getPomValue(String originalValue,
                                      Model pomModel) {
    Properties properties = pomModel.getProperties();
    if (properties == null || properties.isEmpty()) {
      return originalValue;
    }
    if (StringUtils.isBlank(originalValue)) {
      return originalValue;
    }
    if (!originalValue.startsWith(PROPERTY_PREFIX)) {
      return originalValue;
    }
    String propertyKey = originalValue.substring(PROPERTY_PREFIX.length(),
        originalValue.lastIndexOf(PROPERTY_SUFFIX));
    if (properties.containsKey(propertyKey)) {
      return properties.getProperty(propertyKey);
    }
    return originalValue;
  }
}
