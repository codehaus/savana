package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.logging.Logger;

public class MultipleWorkspacesTest extends AbstractSavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(MultipleWorkspacesTest.class.getName());

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

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
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);

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
        FileUtils.writeStringToFile(wc2_bandsFile, "Beatles\nPink Floyd\nWho", "UTF-8");
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
        FileUtils.writeStringToFile(wc1_drinksFile, drinks + "\ntequila", "UTF-8");
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
        FileUtils.writeStringToFile(wc1_animalsFile, animals + "\nleopard", "UTF-8");
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
                new File[]{WC1}, false, "user branch commit - changed gin to scotch", null, null, false, false, SVNDepth.INFINITY);

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
}