package hudson.plugins.svn_partial_release_mgr.api.model;

import org.apache.commons.lang.StringUtils;

import hudson.Util;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;
import hudson.scm.SubversionReleaseSCM;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class JobConfigurationUserInput {
  private final String remoteLocation;
  private final String tagName;
  private final String issuePrefixes;
  private final SubversionReleaseSCM.ModuleLocation location;

  public JobConfigurationUserInput(String remoteLocation,
                                   String tagName,
                                   String issuePrefixes) {
    this.remoteLocation = remoteLocation;
    this.tagName = tagName;
    this.issuePrefixes = issuePrefixes;
    this.location = toModuleLocation();
  }

  public String getTagName() {
    return tagName;
  }

  public String getIssuePrefixes() {
    return issuePrefixes;
  }

  public String getRemoteLocation() {
    return remoteLocation;
  }

  public SubversionReleaseSCM.ModuleLocation getLocation() {
    return location;
  }

  private SubversionReleaseSCM.ModuleLocation toModuleLocation() {
    String remoteLoc = StringUtils.trimToNull(remoteLocation);
    String localLocation = Constants.DIR_NAME_BUILDS + "/" + tagName;
    remoteLoc = Util.removeTrailingSlash(remoteLoc.trim());
    return new SubversionReleaseSCM.ModuleLocation(remoteLoc,
        StringUtils.trimToNull(localLocation));
  }
}
