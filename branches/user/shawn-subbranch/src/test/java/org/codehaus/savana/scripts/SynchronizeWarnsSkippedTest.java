package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;

/**
 * A Synchronize operation can skip changes where the changed file has been
 * deleted from the user branch.  It's easy to miss them in a long list of
 * changed files, so verify that Synchronize highlights the fact that files
 * were skipped so the user can double check whether skip is ok or not.
 */
public class SynchronizeWarnsSkippedTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    /**
     * Test verifies that synchronizing a replaced file doesn't work.  If Subversion/SVNKit ever gets
     * fixed so that synchronizing a replaced file does work, the Promote command can be updated to
     * remove the special check for replaced files.
     */
    public void testSynchronizeWarnsSkipped() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase() + "-sync";
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);

        // in WC1, create a user branch
        cd(WC1);
        savana(CreateUserBranch.class, "user1");

        // in WC2 (trunk), change the filet
        FileUtils.writeStringToFile(new File(WC2, "src/text/animals.txt"), "grasshopper");
        FileUtils.writeStringToFile(new File(WC2, "src/text/autos.txt"), "pinto");
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "trunk - modified files", null, null, false, false, SVNDepth.INFINITY);


        // in WC1 (user branch), delete one file in svn and delete the second locally
        SVN.getWCClient().doDelete(new File(WC1, "src/text/animals.txt"), false, false);
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch - deleted animals.txt", null, null, false, false, SVNDepth.INFINITY);
        new File(WC1, "src/text/autos.txt").delete();

        // in WC1 (user branch), sync
        cd(WC1);
        assertEquals(
                "Skipped missing target: '" + new File("src/text/autos.txt") + "'\n" +
                "Skipped missing target: '" + new File("src/text/animals.txt") + "'\n" +
                "\n" +
                "WARNING: The following files were not synchronized!  They have changes in trunk\n" +
                "but have been deleted in the local user branch.  Merge the changes manually:\n" +
                "- " + new File("src/text/animals.txt") + "\n" +
                "- " + new File("src/text/autos.txt"),
                savana(Synchronize.class).replace("\r", ""));

        assertEquals(
                "Modified Files:\n" +
                "-------------------------------------------------\n" +
                "src/text/autos.txt\n" +
                "\n" +
                "Deleted Files:\n" +
                "-------------------------------------------------\n" +
                "src/text/animals.txt",
                savana(ListChangesFromSource.class).replace("\r", ""));
    }
}
