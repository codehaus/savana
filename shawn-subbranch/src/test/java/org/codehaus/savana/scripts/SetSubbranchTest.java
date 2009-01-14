
package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.io.IOException;

/**
 * Test all the subbranch-specific error conditions that can be encountered by SetBranch.
 */
public class SetSubbranchTest extends AbstractSavanaScriptsTestCase {

    private static final String EOL = System.getProperty("line.separator");

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testUserSubbranches() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, null);

        // setup initial data
        addFile(new File(WC1, "animal/mammal/dog/german_shephard.txt"), "bark");
        addFile(new File(WC1, "animal/reptile/alligator"), "teeth");
        addFile(new File(WC1, "plant/tree/maple"), "syrup");
        SVN.getCommitClient().doCommit(new File[]{WC1},
                false, "trunk - initial data", null, null, false, false, SVNDepth.INFINITY).getNewRevision();

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
            assertEquals("svn: ERROR: Cannot switch branches while a subdirectory or file is switched relative to the root." +
                         "\nRun 'sav info -R' to find nested workspaces or retry with --force" + EOL, e.getErr());
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
            assertEquals("svn: ERROR: Can't switch to a branch that's not a child or parent of the current directory." +
                         "\nCurrent Directory: " + new File(WC1, "animal") +
                         "\nBranch Root:       " + new File(WC1, "plant") + EOL, e.getErr());
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
            assertEquals("svn: ERROR: Can't switch to a directory outside of the working copy." +
                         "\nWorking Copy Root: <project>/animal" +
                         "\nBranch Root:       <project>/plant" + EOL, e.getErr());
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
            assertEquals("svn: ERROR: Can't switch a subbranch to a top-level branch other than trunk." +
                         "\nWorking Copy Root: <project>/animal" +
                         "\nBranch Root:       <project>/" + EOL, e.getErr());
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
            assertEquals("svn: ERROR: Can't switch a subbranch to a top-level branch other than trunk." +
                         "\nWorking Copy Root: <project>/animal/mammal/dog" +
                         "\nBranch Root:       <project>/" + EOL, e.getErr());
        }
        // from /animal/mammal/dog, not allowed to switch parent to another user branch
        try {
            savana(SetBranch.class, "user-animal2");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Can't switch a subbranch to a top-level branch other than trunk." +
                         "\nWorking Copy Root: <project>/animal/mammal/dog" +
                         "\nBranch Root:       <project>/animal" + EOL, e.getErr());
        }
        // from /animal, not allowed to switch the root while there are switched subbranches
        cd(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-root");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Cannot switch branches while a subdirectory or file is switched relative to the root.\n" +
                         "Run 'sav info -R' to find nested workspaces or retry with --force" + EOL, e.getErr());
        }
        // ... but it's ok if the user passes --force
        savana(SetBranch.class, "user-root", "--force");

        // not allowed to nest user branches
        try {
            savana(SetBranch.class, "user-dog");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Can't switch to a subbranch that's not based on the working copy." +
                         "\nBranch Source: " + projectName + "/trunk" +
                         "\nWorking Copy:  " + projectName + "/branches/user/user-root" + EOL, e.getErr());
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
            assertEquals("svn: '" + new File("animal/mammal/dog") + "' is not a working copy" + EOL, e.getErr());
        }
        // can't switch to a subbranch if its root dir is missing
        FileUtils.deleteDirectory(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-animal");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Branch root directory may not have a status of missing." +
                         "\nPath: " + new File(WC1, "animal") + EOL, e.getErr());
        }
        // ...or it exists but isn't versioned...
        new File(WC1, "animal").mkdir();
        try {
            savana(SetBranch.class, "user-animal");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: Directory '" + new File("animal/.svn") + "' containing working copy admin area is missing" + EOL, e.getErr());
        }
        new File(WC1, "animal").delete();
        // ...or it exists but is deleted in the working copy (restore via 'svn update' then 'svn delete')
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        SVN.getWCClient().doDelete(new File(WC1, "animal"), false, true, false);
        assertTrue(new File(WC1, "animal").exists());
        new File(WC1, "animal").mkdir();
        try {
            savana(SetBranch.class, "user-animal");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Branch root directory may not have a status of deleted." +
                         "\nPath: " + new File(WC1, "animal") + EOL, e.getErr());
        }
    }

    private void addFile(File file, String data) throws SVNException, IOException {
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, data);
        SVNWCClient wcClient = SVN.getWCClient();
        wcClient.doAdd(file, false, false, true, SVNDepth.INFINITY, false, true);
    }
}
