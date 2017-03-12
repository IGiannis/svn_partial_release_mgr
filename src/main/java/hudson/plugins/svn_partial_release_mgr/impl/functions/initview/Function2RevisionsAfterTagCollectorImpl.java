package hudson.plugins.svn_partial_release_mgr.impl.functions.initview;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;

import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function2RevisionsAfterTagCollector;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.plugins.svn_partial_release_mgr.api.model.Revision;
import hudson.scm.SubversionReleaseSCM;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function2RevisionsAfterTagCollectorImpl
    implements Function2RevisionsAfterTagCollector {

  /**
   * Returns a collection of all the revisions that are greater than the input tag revision
   * and to the latest source
   *
   * @param releaseInput      the input of the service to get the tag name from
   * @param latestTagRevision the tag revision number
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  @Override
  public Collection<Revision> getRevisions(JobConfigurationUserInput releaseInput,
                                           long latestTagRevision) throws IOException {
    DAVRepositoryFactory.setup();
    SortedMap<Long, Revision> revisions = new TreeMap<>(Collections.reverseOrder());
    SubversionReleaseSCM.ModuleLocation moduleLocation = releaseInput.getLocation();

    SVNURL svnUrl;
    try {
      svnUrl = moduleLocation.getSVNURL();
      SVNLogClient log_client = SubversionReleaseSCM.createSvnClientManager().getLogClient();
      final Collection<SVNLogEntry> logEntries = new LinkedList<>();
      log_client.doLog(svnUrl, new String[]{""},
          SVNRevision.UNDEFINED, SVNRevision.create(new Date()),
          SVNRevision.create(latestTagRevision), false, true, 0,
          new ISVNLogEntryHandler() {
            @Override
            public void handleLogEntry(SVNLogEntry logEntry) {
              logEntries.add(logEntry);
            }
          });

      //repository = createSvnClientManager().createRepository(svnUrl, true);
      //logEntries = repository.log(new String[]{""}, null, start, end, true, false);
      for (SVNLogEntry logEntry : logEntries) {
        revisions.put(logEntry.getRevision(), new Revision(logEntry));
      }
    } catch (SVNException e) {
      throw new IOException("Error while trying to get the revisions!!" +
          ExceptionUtils.getStackTrace(e));
    }
    return revisions.values();
  }
}
