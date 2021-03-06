
package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;

/**
 * Test all the subbranch-specific error conditions that can be encountered by SetBranch.
 */
public class SetSubbranchTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testUserSubbranches() throws Exception {
        // setup a test project with a working directory and import the 'test-subbranch' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-subbranch");

        // setup initial workspaces
        cd(WC1);
        savana(CreateUserBranch.class, "user-root");
        savana(SetBranch.class, "trunk");
        savana(CreateUserBranch.class, "user-dog", "animal/mammal/dog");
        savana(CreateUserBranch.class, "user-animal", "animal", "--force");
        savana(CreateUserBranch.class, "user-animal2", "animal");
        savana(CreateUserBranch.class, "user-plant", "plant");

        //
        // Test Various SetBranch Combinations
        //

        // with all the switched subdirectories, setbranch at the root should refuse to run w/o --force
        cd(WC1);
        try {
            savana(SetBranch.class, "trunk");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Cannot switch branches while a subdirectory or file is switched relative to the root." +
                         "\nRun 'sav info -R' to find nested workspaces or retry with --force\n", e.getErr());
        }

        // reset everything back to trunk
        savana(SetBranch.class, "trunk", "--force");

        // from /animal with /=trunk, can't switch to a branch that's rooted at /plant
        cd(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-plant");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Can't switch to a branch that's not a child or parent of the current directory." +
                         "\nCurrent Directory: " + new File(WC1, "animal") +
                         "\nBranch Root:       " + new File(WC1, "plant") + "\n", e.getErr());
        }

        // set /==trunk and /animal==user-animal
        savana(SetBranch.class, "user-animal");

        // from /animal with /==trunk and /animal==user-animal, still can't switch to a branch that's rooted at /plant
        cd(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-plant");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Can't switch to a directory outside of the working copy." +
                         "\nWorking Copy Root: <project>/animal" +
                         "\nBranch Root:       <project>/plant\n", e.getErr());
        }

        // from / with /==trunk and /animal==user-animal, ok to switch to branch rooted at /plant
        cd(WC1);
        savana(SetBranch.class, "user-plant");

        // from /animal with /==trunk and /animal==user-animal, can't change to user-root at /
        cd(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-root");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Can't switch a subbranch to a top-level branch other than trunk." +
                         "\nWorking Copy Root: <project>/animal" +
                         "\nBranch Root:       <project>/\n", e.getErr());
        }

        // ...but it is OK to switch to trunk from /animal, since it realigns the subdir with its parent
        cd(new File(WC1, "animal"));
        savana(SetBranch.class, "trunk");

        // switch to a workspace a couple levels deep: /=trunk, /animal/mammal/dog=user-dog
        savana(SetBranch.class, "user-dog");

        // from /animal/mammal/dog, not allowed to switch to a different top-level user branch
        cd(new File(WC1, "animal/mammal/dog"));
        try {
            savana(SetBranch.class, "user-root");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Can't switch a subbranch to a top-level branch other than trunk." +
                         "\nWorking Copy Root: <project>/animal/mammal/dog" +
                         "\nBranch Root:       <project>/\n", e.getErr());
        }
        // from /animal/mammal/dog, not allowed to switch parent to another user branch
        try {
            savana(SetBranch.class, "user-animal2");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Can't switch a subbranch to a top-level branch other than trunk." +
                         "\nWorking Copy Root: <project>/animal/mammal/dog" +
                         "\nBranch Root:       <project>/animal\n", e.getErr());
        }
        // from /animal, not allowed to switch the root while there are switched subbranches
        cd(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-root");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Cannot switch branches while a subdirectory or file is switched relative to the root.\n" +
                         "Run 'sav info -R' to find nested workspaces or retry with --force\n", e.getErr());
        }
        // ... but it's ok if the user passes --force
        savana(SetBranch.class, "user-root", "--force");

        // not allowed to nest user branches
        try {
            savana(SetBranch.class, "user-dog");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Can't switch to a subbranch that's not based on the working copy." +
                         "\nBranch Source: " + projectName + "/trunk" +
                         "\nWorking Copy:  " + projectName + "/branches/user/user-root\n", e.getErr());
        }

        // reset back to trunk and muck up the 'animal' subdirectory locally
        cd(WC1);
        savana(SetBranch.class, "trunk");

        // can't switch to a subbranch if its root dir is deleted
        FileUtils.deleteDirectory(new File(WC1, "animal/mammal"));
        try {
            savana(SetBranch.class, "user-dog");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Branch root directory may not have a status of missing.\n" +
                         "Path: " + new File(WC1, "animal/mammal/dog") + "\n", e.getErr());
        }
        // can't switch to a subbranch if its root dir is missing
        FileUtils.deleteDirectory(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-animal");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Branch root directory may not have a status of missing." +
                         "\nPath: " + new File(WC1, "animal") + "\n", e.getErr());
        }
        // ...or it exists but isn't versioned...
        new File(WC1, "animal").mkdir();
        try {
            savana(SetBranch.class, "user-animal");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Cannot switch branches while the working copy has local changes.\n" +
                         "Run 'svn status' to find changes or retry with --force\n", e.getErr());
        }
        new File(WC1, "animal").delete();
        // ...or it exists but is deleted in the working copy (restore via 'svn update' then 'svn delete')
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        SVN.getWCClient().doDelete(new File(WC1, "animal"), false, true, false);
        new File(WC1, "animal").mkdir();
        try {
            savana(SetBranch.class, "user-animal");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200009: ERROR: Branch root directory may not have a status of deleted." +
                         "\nPath: " + new File(WC1, "animal") + "\n", e.getErr());
        }
    }
}
