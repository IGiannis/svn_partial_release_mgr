package hudson.plugins.svn_partial_release_mgr.api.model;

import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.SVNLogEntry;

import java.util.HashMap;
import java.util.Map;

import hudson.model.Run;
import hudson.plugins.svn_partial_release_mgr.api.constants.Constants;

/**
 * Pojo class used to hold a revision info and for UI view purposes
 *
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Revision {

  private final Map<Long, Run> builds;
  private final SVNLogEntry logEntry;
  private final boolean alreadyDeployed;
  private final String deployedDate;

  public Revision(SVNLogEntry logEntry) {
    this.builds = new HashMap<>();
    this.logEntry = logEntry;
    this.alreadyDeployed = false;
    this.deployedDate = null;
  }

  private Revision(Map<Long, Run> builds,
                   SVNLogEntry logEntry,
                   boolean alreadyDeployed,
                   String deployedDate) {
    this.builds = builds;
    this.logEntry = logEntry;
    this.alreadyDeployed = alreadyDeployed;
    this.deployedDate = deployedDate;
  }

  public Revision asAlreadyDeployed(String deployedDate) {
    return new Revision(builds, logEntry, true, deployedDate);
  }

  /**
   * Gets the number of the revision that this object represents.
   *
   * @return a revision number
   */
  public long getRevision() {
    return logEntry.getRevision();
  }

  public SVNLogEntry getLogEntry() {
    return logEntry;
  }

  public Map<Long, Run> getBuilds() {
    return builds;
  }

  public boolean isAlreadyDeployed() {
    return alreadyDeployed;
  }

  public String getDeployedDate() {
    return StringUtils.defaultString(deployedDate);
  }

  public String getStyle() {
    return isAlreadyDeployed() ?
        "border-bottom:1px solid "+ Constants.UI_TABLE_COLOR_ROW_SEPARATOR+";"
            + "background:"+Constants.UI_TABLE_COLOR_ALREADY_DEPLOYED+"" :
        "border-bottom:1px solid "+ Constants.UI_TABLE_COLOR_ROW_SEPARATOR;
  }
}
