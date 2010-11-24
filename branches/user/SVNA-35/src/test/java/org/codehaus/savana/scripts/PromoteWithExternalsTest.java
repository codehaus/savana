package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;

/**
 * Test that svn:externals tied to a specific revision don't cause promotes to fail.
 * <p/>
 * This test validates the fix for http://jira.codehaus.org/browse/SVNA-35
 */
public class PromoteWithExternalsTest extends AbstractSavanaScriptsTestCase {

    public void testUserSubbranches() throws Exception {
        // create a new "external" repo, and seed it with some data
        SVNURL externalRepoUrl = TestRepoUtil.newRepository(false);
        File extWc = TestRepoUtil.setupProjectWithWC(
                externalRepoUrl, "mammals", false, false, "test-subbranch/animal/mammal");

        // take note of the URL and the revision number at this point
        SVNInfo extInfo = SVN.getWCClient().doInfo(extWc, SVNRevision.HEAD);
        String extUrl = extInfo.getURL().toString();
        long rev = extInfo.getRevision().getNumber();

        // update the file and commit
        FileUtils.writeStringToFile(new File(extWc, "dog/german_shephard.txt"), "woof");
        SVN.getCommitClient().doCommit(
                new File[]{extWc}, false, "trunk commit - changed bark to woof", null, null,
                false, false, SVNDepth.INFINITY);

        // create our "main" repo
        SVNURL repoUrl = TestRepoUtil.newRepository(false);
        File wc = TestRepoUtil.setupProjectWithWC(repoUrl, "main", true, true, "test-project");

        // set the svn:externals on this repo to reference the contents of our external repo
        String externalsValue = String.format("mammals -r %s %s", rev, extUrl);
        SVN.getWCClient().doSetProperty(wc, "svn:externals",
                SVNPropertyValue.create(externalsValue), true, SVNDepth.EMPTY, null, null);
        SVN.getCommitClient().doCommit(
                new File[]{wc}, false, "trunk commit - adding svn:externals", null, null, false,
                false, SVNDepth.INFINITY);

        // use savana to create a user branch
        cd(wc);
        savana(CreateUserBranch.class, "user1");

        // make a simple change and commit it
        FileUtils.writeStringToFile(new File(wc, "src/text/autos.txt"), "camry\rrodeo");
        SVN.getCommitClient().doCommit(
                new File[]{wc}, false, "user branch commit - updating autos", null, null, false,
                false, SVNDepth.INFINITY);

        // verify that we can promote (this is the line that fails before making 'svn promote' use
        // a status client that ignores externals)
        savana(Promote.class, "-m", "trunk - updating autos");

        // create another user branch
        savana(CreateUserBranch.class, "user2");

        // make a change to one of the files in the svn:externals directory
        FileUtils.writeStringToFile(new File(wc, "mammals/dog/german_shephard.txt"), "bow wow wow");

        // make a simple change to one of our "main" files and commit it
        FileUtils.writeStringToFile(new File(wc, "src/text/autos.txt"), "camry\nexpedition");
        SVN.getCommitClient().doCommit(
                new File[]{wc}, false, "user branch commit - updating autos", null, null, false,
                false, SVNDepth.INFINITY);

        // verify that we can promote - we are leaving uncommitted changes in our "external"
        // directory, which is okay - the modified externals directory will need to be committed
        // separately
        savana(Promote.class, "-m", "trunk - updating autos");

        // check that the external file is modified.
        SVNStatus svnStatus =
                SVN.getStatusClient().doStatus(new File(wc, "mammals/dog/german_shephard.txt"), true);
        assertEquals(SVNStatusType.STATUS_MODIFIED, svnStatus.getRemoteContentsStatus());
    }
}
