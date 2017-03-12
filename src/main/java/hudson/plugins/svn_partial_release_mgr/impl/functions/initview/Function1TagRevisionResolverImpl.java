package hudson.plugins.svn_partial_release_mgr.impl.functions.initview;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.IOException;

import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function1TagRevisionResolver;
import hudson.plugins.svn_partial_release_mgr.api.model.JobConfigurationUserInput;
import hudson.scm.SubversionReleaseSCM;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class Function1TagRevisionResolverImpl implements Function1TagRevisionResolver {

  /**
   * Gets from the svn the revision number of the user input tag name
   *
   * @param releaseInput the input of the service to get the tag name from
   * @return the object that holds all the revisions from the tag and to the latest source
   */
  @Override
  public long resolveTagNameRevision(JobConfigurationUserInput releaseInput) throws IOException {
    try {
      SVNURL svnurl = releaseInput.getLocation().getSVNURL();
      SVNURL tagURL = SVNURL.parseURIDecoded(PluginUtil.pathToTag(svnurl,
          releaseInput.getTagName()));
      SVNRepository repository = SubversionReleaseSCM
          .createSvnClientManager().createRepository(tagURL, false);
      SVNDirEntry entry = repository.info(".", -1);
      //System.out.println("Latest Rev: " + entry.getRevision());
      long latestRevisionOfTag = entry.getRevision();
      System.out.println("LATEST_REVISION [" + latestRevisionOfTag + "] "
          + "of [" + tagURL.toString() + "]");
      return latestRevisionOfTag;

    } catch (SVNException e) {
      throw new IOException("Error while trying to get the "
          + "tag [" + releaseInput.getTagName() + "] revision number !!" +
          ExceptionUtils.getStackTrace(e));
    }
  }
}
