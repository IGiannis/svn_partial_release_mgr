package hudson.plugins.svn_partial_release_mgr.api.functions.initview;

import java.io.IOException;

import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public interface Function1TagRevisionResolver {

  /**
   * Gets from the svn the revision number of the user input tag name
   *
   * @param releaseInput the input of the service to get the tag name from
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  long resolveTagNameRevision(JobConfigurationUserInput releaseInput) throws IOException;
}
