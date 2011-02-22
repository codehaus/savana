package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Tests that Promote will fail if files have been replaced (deleted then re-added).
 * Replaced files cause a few problems:
 * - history on the replaced files is lost.
 * - synchronizing replaced files into a user workspace can get unexpected merge
 *   results that effectively throw away changes.
 *
 * UPDATE 2009-06-03: In initial tests it appears that SVNKit 1.3.0 (and Subversion 1.6)
 * fix the underlying problem that required preventing replaced files: sav sync/svn merge
 * now appears to sync changes in replaced files correctly.  So we may be able to remove
 * the special handling around replaced files.  TODO: verify that the fix works with all
 * versions of the Subversion server, ie. updating to SVNKit 1.3.0 is sufficient and
 * doesn't require upgrading the server to SVN 1.6.
 */
public class PreventReplacedFileTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    /**
     * Test verifies that synchronizing a replaced file doesn't work.  If Subversion/SVNKit ever gets
     * fixed so that synchronizing a replaced file does work, the Promote command can be updated to
     * remove the special check for replaced files.
     */
    public void testSynchronizeReplaced() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase() + "-sync";
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);
        long branchPointRev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

        // in WC1, create a user branch
        cd(WC1);
        savana(CreateUserBranch.class, "user1");

        // in WC2 (trunk), delete a file and commit
        cd(WC2);
        File animalsFile = new File(WC2, "src/text/animals.txt");
        SVN.getWCClient().doDelete(animalsFile, false, false);
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "trunk - deleted animals.txt", null, null, false, false, SVNDepth.INFINITY);


        // in WC2 (trunk), recreate the file and commit
        cd(WC2);
        FileUtils.writeStringToFile(animalsFile, "grasshopper");
        SVN.getWCClient().doAdd(animalsFile, false, false, false, SVNDepth.EMPTY, false, false);
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "trunk - re-added animals.txt", null, null, false, false, SVNDepth.INFINITY);

        // in WC1 (user branch), modify animals.txt
        cd(WC1);
        appendStringToFile(new File(WC1, "src/text/animals.txt"), "\ndragonfly");
        long changeset = SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch - modify animals.txt", null, null, false, false, SVNDepth.INFINITY).getNewRevision();

        SVNURL trunkUrl = REPO_URL.appendPath(projectName, false).appendPath("trunk", false);
        assertEqualsNormalized(
                MessageFormat.format(
                        "Index: src/text/animals.txt\n" +
                        "===================================================================\n" +
                        "--- src/text/animals.txt\t(.../{0})\t(revision {2})\n" +
                        "+++ src/text/animals.txt\t(...{1})\t(working copy)\n" +
                        "@@ -1,4 +1,5 @@\n" +
                        " monkey\n" +
                        " dog\n" +
                        " rat\n" +
                        "-dragon\n" +
                        "\\ No newline at end of file\n" +
                        "+dragon\n" +
                        "+dragonfly\n" +
                        "\\ No newline at end of file",
                        trunkUrl.toString(),
                        TestDirUtil.toSvnkitAbsolutePath(WC1),
                        branchPointRev),
                savana(DiffChangesFromSource.class));

        // in WC1 (user branch), sync animals.txt with the parent.  animals.txt is left in conflict.
        assertEqualsNormalized(
                MessageFormat.format(
                        "--- Merging r{0} through r{1} into ''.'':\n" +
                        "   C src/text/animals.txt\n" +
                        "Summary of conflicts:\n" +
                        "  Tree conflicts: 1",
                        branchPointRev + 1,
                        branchPointRev + 4),
                savana(Synchronize.class));

        // "sav diff" correctly reports the diff between the trunk and user branch.
        assertEqualsNormalized(
                MessageFormat.format(
                        "Index: src/text/animals.txt\n" +
                        "===================================================================\n" +
                        "--- src/text/animals.txt\t(.../{0})\t(revision {2})\n" +
                        "+++ src/text/animals.txt\t(...{1})\t(working copy)\n" +
                        "@@ -1 +1,5 @@\n" +
                        "-grasshopper\n" +
                        "\\ No newline at end of file\n" +
                        "+monkey\n" +
                        "+dog\n" +
                        "+rat\n" +
                        "+dragon\n" +
                        "+dragonfly\n" +
                        "\\ No newline at end of file",
                        trunkUrl.toString(),
                        TestDirUtil.toSvnkitAbsolutePath(WC1),
                        changeset),
                savana(DiffChangesFromSource.class));
    }

    public void testPromoteReplaced() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase() + "-promote";
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");

        // create a user branch
        cd(WC1);
        savana(CreateUserBranch.class, "user1");

        // delete a file and check in the delete
        File animalsFile = new File(WC1, "src/text/animals.txt");
        File autosFile = new File(WC1, "src/text/autos.txt");
        SVN.getWCClient().doDelete(animalsFile, false, false);
        SVN.getWCClient().doDelete(autosFile, false, false);
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - deleted files", null, null, false, false, SVNDepth.INFINITY);

        // create a new version of the file and check it in
        FileUtils.writeStringToFile(animalsFile, "grasshopper");
        FileUtils.writeStringToFile(autosFile, "gremlin");
        SVN.getWCClient().doAdd(animalsFile, false, false, false, SVNDepth.EMPTY, false, false);
        SVN.getWCClient().doAdd(autosFile, false, false, false, SVNDepth.EMPTY, false, false);
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - re-added files", null, null, false, false, SVNDepth.INFINITY);

        // promote should fail
        try {
            savana(Promote.class, "-m", "trunk - replaced files");
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Cannot promote branch user1 while there are replaced files:\n" +
                         "- " + new File("src/text/animals.txt") + "\n" +
                         "- " + new File("src/text/autos.txt") + "\n", e.getErr());
        }

        // get back to the user branch
        SVN.getWCClient().doRevert(new File[]{WC1}, SVNDepth.INFINITY, null);
        savana(SetBranch.class, "user1");

        // recover by deleting the file, re-doing the changes, and re-promoting
        SVN.getWCClient().doDelete(animalsFile, false, false);
        SVN.getWCClient().doDelete(autosFile, false, false);
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - delete before revert", null, null, false, false, SVNDepth.INFINITY);
        savana(RevertToSource.class, animalsFile.getPath(), autosFile.getPath());

        // now make the change again without replacing the file
        FileUtils.writeStringToFile(animalsFile, "grasshopper");
        FileUtils.writeStringToFile(autosFile, "gremlin");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - re-do changes", null, null, false, false, SVNDepth.INFINITY);

        // promote should succeed
        savana(Promote.class, "-m", "trunk - replaced file contents");
    }

    private void appendStringToFile(File file, String string) throws IOException {
        FileWriter out = new FileWriter(file, true);
        try {
            out.write(string);
        } finally {
            out.close();
        }
    }
}
