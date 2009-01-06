package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.codehaus.savana.BranchType;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.scripts.admin.CreateMetadataFile;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.logging.Logger;

public class BasicSavanaScriptsTest extends SavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(BasicSavanaScriptsTest.class.getName());

    private static final String EOL = System.getProperty("line.separator");

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

            // install savana preferred subversion hooks into the test repository
            File svnHookDir = new File(System.getProperty("savana.svn-hooks"));
            for (File svnHookFile :  svnHookDir.listFiles((FileFilter) HiddenFileFilter.VISIBLE)) {
                File repoHookFile = new File(new File(REPO_DIR, "hooks"), svnHookFile.getName());
                FileUtils.copyFile(svnHookFile, repoHookFile, false);
                repoHookFile.setExecutable(true);
            }

            // setup initial branching structure
            log.info("creating initial branch directories");
            SVN.getCommitClient().doMkDir(new SVNURL[] {
                    projectUrl.appendPath(BranchType.TRUNK.getDefaultPath(), false),
                    projectUrl.appendPath(BranchType.RELEASE_BRANCH.getDefaultPath(), false),
                    projectUrl.appendPath(BranchType.USER_BRANCH.getDefaultPath(), false),
            }, "branch admin - setup initial branch directories", null, true);

            // get the directory from the classpath that contains the project to import, and import the project
            // into the repository
            File importDir = new File(
                    BasicSavanaScriptsTest.class.getClassLoader().getResource(TEST_PROJECT_NAME).toURI());
            log.info("importing project");
            SVN.getCommitClient().doImport(importDir, TRUNK_URL, "trunk - initial import", null, true, false, SVNDepth.INFINITY);

            // check out the two repositories
            log.info("checking out repo into two working copies");
            SVN.getUpdateClient().doCheckout(TRUNK_URL, WC1, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);
            SVN.getUpdateClient().doCheckout(TRUNK_URL, WC2, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);

            // cd into the wc dir, and create the .savana metadata file
            cd(WC1);
            savana(CreateMetadataFile.class, TEST_PROJECT_NAME, TEST_PROJECT_NAME + "/trunk", "TRUNK");
            SVN.getCommitClient().doCommit(
                    new File[]{WC1}, false, "trunk - initial setup of savana", null, null, false, false, SVNDepth.INFINITY);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
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
        long rev = SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false);
        long branchPointRev = rev;

        // check that we are starting in the trunk
        assertEquals("trunk", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());

        // create a workspace
        log.info("creating workspace");
        savana(CreateUserBranch.class, "workspace");

        // list the user branches - there should be just the one
        assertEquals("--------------------------------------------------------------" + EOL +
                     "Branch Name         Source         Branch-Point   Last-Merge" + EOL +
                     "--------------------------------------------------------------" + EOL +
                     "workspace           trunk          " + branchPointRev + "              " + branchPointRev,
                     savana(ListUserBranches.class));

        // check that we've changed into the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // open a file in the wc, edit it, and write it back
        log.info("editing src/text/animals.txt");
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));

        // check in the change
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - changed monkey to mongoose", null, null, false, false, SVNDepth.INFINITY);


        assertEquals(
                MessageFormat.format(
                        "Index: src/text/animals.txt\n" +
                        "===================================================================\n" +
                        "--- src/text/animals.txt\t(.../{0})\t(revision {2})\n" +
                        "+++ src/text/animals.txt\t(...{1})\t(working copy)\n" +
                        "@@ -1,4 +1,4 @@\n" +
                        "-monkey\n" +
                        "+mongoose\n" +
                        " dog\n" +
                        " rat\n" +
                        " dragon\n" +
                        "\\ No newline at end of file",
                        TRUNK_URL.toString(),
                        toSvnkitAbsolutePath(WC1),
                        branchPointRev),
                savana(DiffChangesFromSource.class).replace("\r", ""));

        // check that we're still in the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals(
                MessageFormat.format(
                        "Modified Files:" + EOL +
                        "-------------------------------------------------" + EOL +
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
                new File[]{WC1}, false, "user branch commit - renamed autos.txt to cars.txt", null, null, false, false, SVNDepth.INFINITY);

        // check that we're still in the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals(MessageFormat.format(
                "Added Files:" + EOL +
                "-------------------------------------------------" + EOL +
                "src/text/cars.txt" + EOL +
                "" + EOL +
                "Modified Files:" + EOL +
                "-------------------------------------------------" + EOL +
                "src/text/animals.txt" + EOL +
                "" + EOL +
                "Deleted Files:" + EOL +
                "-------------------------------------------------" + EOL +
                "src/text/autos.txt",
                branchPointRev),
                     savana(ListChangesFromSource.class));

        // sync from trunk (should be a no-op since there aren't any changes to sync)
        log.info("syncing from trunk");
        savana(Synchronize.class);

        // check that we're still in the "workspace" branch, and that the revision has NOT updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // list the working copy info and check it
        assertEquals(
                "---------------------------------------------" + EOL +
                "Branch Name:           workspace" + EOL +
                "---------------------------------------------" + EOL +
                "Project Name:          test-project" + EOL +
                "Branch Type:           user branch" + EOL +
                "Source:                trunk" + EOL +
                "Branch Point Revision: " + branchPointRev + "" + EOL +
                "Last Merge Revision:   " + branchPointRev,
                savana(ListWorkingCopyInfo.class));

        // promote the change to trunk
        log.info("promoting change to trunk");
        savana(Promote.class, "-m", "trunk - promoting monkey -> mongoose change");

        // list user branches - there shouldn't be any
        assertEquals("No branches were found.",
                     savana(ListUserBranches.class));

        // check that we're back in the "trunk" branch, and that the revision has updated three times
        // (once to update the metadata, once to check changes into trunk, and once to delete the
        // workspace)
        assertEquals("trunk", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(rev += 2, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

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
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: Error: No source path found (you are probably in the TRUNK)." + EOL, e.getErr());
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
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false);
        log.info("creating workspace bob.editDrinks");
        savana(CreateUserBranch.class, "bob.editDrinks");

        // update working copy 2 and create a branch to add the bands list
        cd(WC2);
        log.info("updating working copy 2");
        SVN.getUpdateClient().doUpdate(WC2, SVNRevision.HEAD, SVNDepth.FILES, false, false);
        log.info("creating workspace sam.addBands");
        savana(CreateUserBranch.class, "sam.addBands");

        // create the bands list in working copy 2, add it, and commit it
        log.info("adding new file src/text/bands.txt");
        File wc2_bandsFile = new File(WC2, "src/text/bands.txt");
        FileUtils.writeStringToFile(wc2_bandsFile, "Beatles" + EOL + "Pink Floyd" + EOL + "Who", "UTF-8");
        SVN.getWCClient().doAdd(wc2_bandsFile, false, false, true, SVNDepth.INFINITY, false, false);
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "user branch commit - added bands.txt", null, null, false, false, SVNDepth.INFINITY);

        // cd to working copy 1, open the drinks file, append "tequila" to it, and commit it
        log.info("changing to working copy 1");
        cd(WC1);
        log.info("append 'tequila' to src/text/drinks.txt");
        File wc1_drinksFile = new File(WC1, "src/text/drinks.txt");
        String drinks = FileUtils.readFileToString(wc1_drinksFile, "UTF-8");
        FileUtils.writeStringToFile(wc1_drinksFile, drinks + "" + EOL + "tequila", "UTF-8");
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - added tequila to drinks.txt", null, null, false, false, SVNDepth.INFINITY);

        // promote the changes to bob.editDrinks into trunk
        log.info("promoting workspace bob.editDrinks");
        savana(Promote.class, "-m", "trunk - promoting changes to drinks.txt");

        // now, working copy 1 is pointed directly at trunk - append "leopard" to the animals list, and commit it
        log.info("editing src/text/animals.txt in trunk");
        File wc1_animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(wc1_animalsFile, "UTF-8");
        FileUtils.writeStringToFile(wc1_animalsFile, animals + EOL + "leopard", "UTF-8");
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "trunk - added leopard to animals.txt", null, null, false, false, SVNDepth.INFINITY);

        // cd to working copy 2 and do an update
        log.info("changing to working copy 2");
        cd(WC2);
        log.info("updating changes");
        SVN.getUpdateClient().doUpdate(WC2, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);

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
                new File[]{WC2}, false, "user branch sync - synced changes from trunk", null, null, false, false, SVNDepth.INFINITY);

        // promote our new file to trunk
        log.info("promoting workspace sam.addBands");
        savana(Promote.class, "-m", "trunk - promoting addition of bands.txt");

        // cd back to working copy 1 (which is pointed at trunk) - bands.txt shouldn't exist yet
        log.info("changing to working copy 1");
        cd(WC1);
        File wc1_bandsFile = new File(WC1, "src/text/bands.txt");
        assertFalse(wc1_bandsFile.exists());

        // do an update, and then it should
        log.info("updating working copy 1");
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        assertTrue(wc1_bandsFile.exists());

        // create another user branch, replace "gin" with "scotch", and commit it
        log.info("creating another branch bob.editDrinksAgain");
        savana(CreateUserBranch.class, "bob.editDrinksAgain");
        log.info("replace 'gin' with 'scotch' in src/text/drinks.txt");
        drinks = FileUtils.readFileToString(wc1_drinksFile, "UTF-8");
        FileUtils.writeStringToFile(wc1_drinksFile, drinks.replaceAll("gin", "scotch"), "UTF-8");
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - changed gin to bourbon", null, null, false, false, SVNDepth.INFINITY);

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
                new File[]{WC1}, false, "user branch commit - reverted change", null, null, false, false, SVNDepth.INFINITY);
    }

    /** Returns the absolute path of a file in the way that matches svnkit. */
    private String toSvnkitAbsolutePath(File file) {
        String path = file.getAbsolutePath().replace(File.separatorChar, '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }
}
