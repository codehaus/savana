package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;

/**
 * Tests that promote is not allowed if the user branch working copy is out-of-date.
 */
public class PromoteOutOfDateTest extends AbstractSavanaScriptsTestCase {

    private static final String EOL = System.getProperty("line.separator");

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testUserSubbranches() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);

        // create a user branch in WC1
        cd(WC1);
        savana(CreateUserBranch.class, "user1");

        // switch to the same user branch in WC1
        cd(WC2);
        savana(SetBranch.class, "user1");

        // open a file in WC2, edit it, and write it back
        File animalsFile = new File(WC2, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "user branch commit - changed monkey to mongoose", null, null, false, false, SVNDepth.INFINITY);

        // now try to promote the file from WC1.  it should fail because the workspace is out-of-date
        cd(WC1);
        try {
            savana(Promote.class, "-m", "trunk - changed animals.txt");
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Cannot promote while the working copy is out-of-date." +
                         "\nRun 'svn update' to update the working copy" + EOL, e.getErr());
        }

        // update then try the promote again.  it should succeed this time.
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        savana(Promote.class, "-m", "trunk - changed animals.txt");
    }
}
