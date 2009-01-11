package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.scripts.admin.CreateMetadataFile;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import java.io.File;
import java.text.MessageFormat;

public class BasicSavanaScriptsTest extends SavanaScriptsTestCase {

    private static final Log log = LogFactory.getLog(BasicSavanaScriptsTest.class);


    protected static final File REPO_DIR = tempDir("savana-test-repo");
    protected static final File WC1 = tempDir("savana-test-wc1");
    protected static final File WC2 = tempDir("savana-test-wc2");

    protected static SVNURL TRUNK_URL;

    static {
        try {
            // create the test repository, and set up the URLs to the repo, the project, and the project's trunk
            log.info("creating test repository");
            SVNAdminClient adminClient = SVN.getAdminClient();
            SVNURL repoUrl = adminClient.doCreateRepository(REPO_DIR, null, false, true);
            SVNURL projectUrl = repoUrl.appendPath(TEST_PROJECT_NAME, true);
            TRUNK_URL = projectUrl.appendPath("trunk", true);

            // get the directory from the classpath that contains the project to import, and import the project
            // into the repository
            File importDir = new File(
                    BasicSavanaScriptsTest.class.getClassLoader().getResource(TEST_PROJECT_NAME).toURI());
            log.info("importing project");
            SVN.getCommitClient().doImport(importDir, projectUrl, "initial import", null, true, false, SVNDepth.fromRecurse(true));

            // check out the two repositories
            log.info("checking out repo into two working copies");
            SVN.getUpdateClient().doCheckout(TRUNK_URL, WC1, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.fromRecurse(true), false);
            SVN.getUpdateClient().doCheckout(TRUNK_URL, WC2, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.fromRecurse(true), false);

            // cd into the wc dir, and create the .savana metadata file
            cd(WC1);
            savana(CreateMetadataFile.class, TEST_PROJECT_NAME, TEST_PROJECT_NAME + "/trunk", "TRUNK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        // cd into the wc1 directory before each test
        cd(WC1);
    }

    /**
     * test a simple user workspace session, with no errors or unusual cases.
     * <p/>
     * the user creates a workspace, makes a change to a file in his workspace, checks it in, and
     * promotes the workspace.  this test case is intentionally simple in its behavior, because it is
     * very rigorous in its assertions about the script output that is produced from the various scripts,
     * and about the progression of SVN revision numbers as the savana scripts run.
     *
     * @throws Exception on error
     */
    public void testBasicWorkspaceSession() throws Exception {
        // update the wc and record the starting revision
        log.info("updating working copy");
        long rev = SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false);
        long branchPointRev = rev;

        // check that we are starting in the trunk
        assertEquals("trunk", new WorkingCopyInfo(SVN).getBranchName());

        // create a workspace
        log.info("creating workspace");
        savana(CreateUserBranch.class, "workspace");

        // list the user branches - there should be just the one
        assertEquals("-----------------------------------------------------------------------------\n" +
                     "Branch Name         Source         Branch-Point   Last-Merge     Last-Promote\n" +
                     "-----------------------------------------------------------------------------\n" +
                     "workspace           trunk          " + branchPointRev + "              " + branchPointRev,
                     savana(ListUserBranches.class, TEST_PROJECT_NAME));

        // check that we've changed into the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false));

        // open a file in the wc, edit it, and write it back
        log.info("editing src/text/animals.txt");
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));

        // check in the change
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "changed monkey to mongoose", false, true);


        assertEquals(
                MessageFormat.format(
                        "Index: {0}\n" +
                        "===================================================================\n" +
                        "--- {0}\t(.../{1})\t(revision {3})\n" +
                        "+++ {0}\t(...{2})\t(working copy)\n" +
                        "@@ -1,4 +1,4 @@\n" +
                        "-monkey\n" +
                        "+mongoose\n" +
                        " dog\n" +
                        " rat\n" +
                        " dragon\n" +
                        "\\ No newline at end of file\n" +
                        "\n" +
                        "Property changes on: {2}/.savana\n" +
                        "___________________________________________________________________\n" +
                        "Added: LAST_MERGE_REVISION\n" +
                        "   + {3}\n" +
                        "Added: BRANCH_POINT_REVISION\n" +
                        "   + {3}\n" +
                        "Modified: BRANCH_PATH\n" +
                        "   - test-project/trunk\n" +
                        "   + test-project/branches/user/workspace\n" +
                        "Added: SOURCE_PATH\n" +
                        "   + test-project/trunk\n" +
                        "Modified: BRANCH_TYPE\n" +
                        "   - TRUNK\n" +
                        "   + USER BRANCH",
                        animalsFile.getAbsolutePath(),
                        TRUNK_URL.toString(),
                        WC1.getAbsolutePath(),
                        branchPointRev),
                savana(DiffChangesFromSource.class));

        // check that we're still in the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false));

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals(
                MessageFormat.format(
                        "Property changes on: /Users/bjacob/working/savana/target/testdata/savana-test-wc1/.savana\n" +
                        "___________________________________________________________________\n" +
                        "Added: LAST_MERGE_REVISION\n" +
                        "   + {0}\n" +
                        "Added: BRANCH_POINT_REVISION\n" +
                        "   + {0}\n" +
                        "Modified: BRANCH_PATH\n" +
                        "   - test-project/trunk\n" +
                        "   + test-project/branches/user/workspace\n" +
                        "Added: SOURCE_PATH\n" +
                        "   + test-project/trunk\n" +
                        "Modified: BRANCH_TYPE\n" +
                        "   - TRUNK\n" +
                        "   + USER BRANCH\n" +
                        "\n" +
                        "Modified Files:\n" +
                        "-------------------------------------------------\n" +
                        "src/text/animals.txt",
                        branchPointRev),
                savana(ListChangesFromSource.class));

        // move the file "autos.txt" to "cars.txt"
        File autosFile = new File(WC1, "src/text/autos.txt");
        File carsFile = new File(WC1, "src/text/cars.txt");
        log.info("moving src/text/autos.txt to src/text/cars.txt");
        SVN.getMoveClient().doMove(autosFile, carsFile);

        // check in the change
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "renamed autos.txt to cars.txt", false, true);

        // check that we're still in the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false));

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals(MessageFormat.format(
                "Property changes on: /Users/bjacob/working/savana/target/testdata/savana-test-wc1/.savana\n" +
                "___________________________________________________________________\n" +
                "Added: LAST_MERGE_REVISION\n" +
                "   + {0}\n" +
                "Added: BRANCH_POINT_REVISION\n" +
                "   + {0}\n" +
                "Modified: BRANCH_PATH\n" +
                "   - test-project/trunk\n" +
                "   + test-project/branches/user/workspace\n" +
                "Added: SOURCE_PATH\n" +
                "   + test-project/trunk\n" +
                "Modified: BRANCH_TYPE\n" +
                "   - TRUNK\n" +
                "   + USER BRANCH\n" +
                "\n" +
                "Added Files:\n" +
                "-------------------------------------------------\n" +
                "src/text/cars.txt\n" +
                "\n" +
                "Modified Files:\n" +
                "-------------------------------------------------\n" +
                "src/text/animals.txt\n" +
                "\n" +
                "Deleted Files:\n" +
                "-------------------------------------------------\n" +
                "src/text/autos.txt",
                branchPointRev),
                     savana(ListChangesFromSource.class));

        // sync from trunk
        log.info("syncing from trunk");
        savana(Synchronize.class);

        // check that we're still in the "workspace" branch, and that the revision has NOT updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getBranchName());
        assertEquals(rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false));

        // list the working copy info and check it
        assertEquals(
                "---------------------------------------------\n" +
                "Branch Name:           workspace\n" +
                "---------------------------------------------\n" +
                "Project Name:          test-project\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + branchPointRev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(ListWorkingCopyInfo.class));

        // promote the change to trunk
        log.info("promoting change to trunk");
        savana(Promote.class, "promoting monkey -> mongoose change");

        // list user branches - there shouldn't be any
        assertEquals("No branches were found.",
                     savana(ListUserBranches.class, TEST_PROJECT_NAME));

        // check that we're back in the "trunk" branch, and that the revision has updated three times
        // (once to update the metadata, once to check changes into trunk, and once to delete the
        // workspace)
        assertEquals("trunk", new WorkingCopyInfo(SVN).getBranchName());
        assertEquals(rev += 3, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false));

        // read the file, and check that our change has been made
        assertTrue(FileUtils.readFileToString(animalsFile, "UTF-8").contains("mongoose"));
    }


    /**
     * tests the case where "list changes from source" is run in trunk.
     * <p/>
     * This scenario is always erroneous, so this unit test checks for the appropriate exception to be thrown from
     * the savana command.
     *
     * @throws Exception on error
     */
    public void testListChangesFromTrunk() throws Exception {
        try {
            // ListChangesFromSource from trunk should never work
            savana(ListChangesFromSource.class);
            assertTrue("we expected an exception to be thrown", false);
        } catch (SVNScriptException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("Error: No source path found (you are probably in the TRUNK).", e.getMessage());
        }
    }


    /**
     * test several concurrent workspaces making changes and syncronizing back and forth.
     * <p/>
     * This scenario simulates two users making changes in multiple workspaces, promoting their changes, and
     * syncronizing them back and forth.  This is meant to simulate a real working environment, where two different
     * developers might have different tasks to accomplish, and each creates a workspace for their work, promotes it
     * into the shared space (trunk) and synchronizes the other's changes along the way.
     *
     * @throws Exception on error
     */
    public void testMultipleWorkspaces() throws Exception {

        // update working copy 1 and create a branch to edit the drinks list
        cd(WC1);
        log.info("updating working copy 1");
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, false);
        log.info("creating workspace bob.editDrinks");
        savana(CreateUserBranch.class, "bob.editDrinks");

        // update working copy 2 and create a branch to add the bands list
        cd(WC2);
        log.info("updating working copy 2");
        SVN.getUpdateClient().doUpdate(WC2, SVNRevision.HEAD, false);
        log.info("creating workspace sam.addBands");
        savana(CreateUserBranch.class, "sam.addBands");

        // create the bands list in working copy 2, add it, and commit it
        log.info("adding new file src/text/bands.txt");
        File wc2_bandsFile = new File(WC2, "src/text/bands.txt");
        FileUtils.writeStringToFile(wc2_bandsFile, "Beatles\nPink Floyd\nWho", "UTF-8");
        SVN.getWCClient().doAdd(wc2_bandsFile, false, false, true, true);
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "added bands.txt", false, true);

        // cd to working copy 1, open the drinks file, append "tequila" to it, and commit it
        log.info("changing to working copy 1");
        cd(WC1);
        log.info("append 'tequila' to src/text/drinks.txt");
        File wc1_drinksFile = new File(WC1, "src/text/drinks.txt");
        String drinks = FileUtils.readFileToString(wc1_drinksFile, "UTF-8");
        FileUtils.writeStringToFile(wc1_drinksFile, drinks + "\ntequila", "UTF-8");
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "added tequila to drinks.txt", false, true);

        // promote the changes to bob.editDrinks into trunk
        log.info("promoting workspace bob.editDrinks");
        savana(Promote.class, "promoting changes to drinks.txt");

        // now, working copy 1 is pointed directly at trunk - append "leopard" to the animals list, and commit it
        log.info("editing src/text/animals.txt in trunk");
        File wc1_animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(wc1_animalsFile, "UTF-8");
        FileUtils.writeStringToFile(wc1_animalsFile, animals + "\nleopard", "UTF-8");
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "added leopard to animals.txt", false, true);

        // cd to working copy 2 and do an update
        log.info("changing to working copy 2");
        cd(WC2);
        log.info("updating changes");
        SVN.getUpdateClient().doUpdate(WC2, SVNRevision.HEAD, true);

        // the drinks file and animals file should not contain any of the above changes at this point
        File wc2_drinksFile = new File(WC2, "src/text/drinks.txt");
        File wc2_animalsFile = new File(WC2, "src/text/animals.txt");
        assertFalse(FileUtils.readFileToString(wc2_drinksFile).contains("tequila"));
        assertFalse(FileUtils.readFileToString(wc2_animalsFile).contains("leopard"));

        // but once we synchronize the changes, they should
        log.info("syncing from trunk");
        savana(Synchronize.class);
        assertTrue(FileUtils.readFileToString(wc2_drinksFile).contains("tequila"));
        assertTrue(FileUtils.readFileToString(wc2_animalsFile).contains("leopard"));

        // commit our sync from trunk into this branch
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "synced changes from trunk", false, true);

        // promote our new file to trunk (savana requires a sync first)
        log.info("promoting workspace sam.addBands");
        savana(Synchronize.class);
        savana(Promote.class, "promoting addition of bands.txt");

        // cd back to working copy 1 (which is pointed at trunk) - bands.txt shouldn't exist yet
        log.info("changing to working copy 1");
        cd(WC1);
        File wc1_bandsFile = new File(WC1, "src/text/bands.txt");
        assertFalse(wc1_bandsFile.exists());

        // do an update, and then it should
        log.info("updating working copy 1");
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, true);
        assertTrue(wc1_bandsFile.exists());

        // create another user branch, replace "gin" with "scotch", and commit it
        log.info("creating another branch bob.editDrinksAgain");
        savana(CreateUserBranch.class, "bob.editDrinksAgain");
        log.info("replace 'gin' with 'scotch' in src/text/drinks.txt");
        drinks = FileUtils.readFileToString(wc1_drinksFile, "UTF-8");
        FileUtils.writeStringToFile(wc1_drinksFile, drinks.replaceAll("gin", "scotch"), "UTF-8");
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "changed gin to bourbon", false, true);

        // check that the change happened
        assertFalse(FileUtils.readFileToString(wc1_drinksFile, "UTF-8").contains("gin"));
        assertTrue(FileUtils.readFileToString(wc1_drinksFile, "UTF-8").contains("scotch"));

        // do a savana "revert to source" to reverse the change, and check that it has reverted
        log.info("reverting change");
        savana(RevertToSource.class, wc1_drinksFile.getAbsolutePath());
        assertTrue(FileUtils.readFileToString(wc1_drinksFile, "UTF-8").contains("gin"));
        assertFalse(FileUtils.readFileToString(wc1_drinksFile, "UTF-8").contains("scotch"));

        // commit the reverted change
        log.info("committing the reverted change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "reverted change", false, true);
    }
}
