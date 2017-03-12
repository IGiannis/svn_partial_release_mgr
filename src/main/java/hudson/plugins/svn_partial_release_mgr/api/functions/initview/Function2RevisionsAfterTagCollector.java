package hudson.plugins.svn_partial_release_mgr.api.functions.initview;

import java.io.IOException;
import java.util.Collection;

import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.Revision;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function2RevisionsAfterTagCollector {

  /**
   * Returns a collection of all the revisions that are greater than the input tag revision
   * and to the latest source
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param latestTagRevision the tag revision number
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  Collection<Revision> getRevisions(JobConfigurationUserInput releaseInput,
                                    long latestTagRevision) throws IOException;
}
