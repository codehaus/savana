
package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNWCClient;

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
        addFile(new File(WC1, "animal/reptile/alligator"), "bite");
        addFile(new File(WC1, "plant/tree/oak"), "leaf");
        SVN.getCommitClient().doCommit(new File[]{WC1},
                false, "trunk - initial data", null, null, false, false, SVNDepth.INFINITY).getNewRevision();

        // setup initial workspaces
        cd(WC1);
        savana(CreateUserBranch.class, "user-root");
        savana(CreateUserBranch.class, "user-animal", "animal");
        savana(CreateUserBranch.class, "user-animal2", "animal");
        savana(CreateUserBranch.class, "user-dog", "animal/mammal/dog");
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

        // from / with /==trunk and /animal==user-animal, ok to branch rooted at /plant is fine
        cd(WC1);
        savana(SetBranch.class, "user-plant");

        // from /animal with /==trunk and /animal==user-animal, can't change to user-root at /
        cd(new File(WC1, "animal"));
        try {
            savana(SetBranch.class, "user-root");
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Can't switch to a branch with a root above the working copy." +
                            "\nWorking Copy Root: <project>/animal" +
                            "\nBranch Root:       <project>/" + EOL, e.getErr());
        }

        // ...but it is OK to switch to trunk from /animal, since it realigns the subdir with its parent
        cd(new File(WC1, "animal"));
        savana(SetBranch.class, "trunk");

        // setup nested workspaces /=trunk, /animal=user-animal, /animal/mammal/dog=user-dog
        savana(SetBranch.class, "user-animal");
        savana(SetBranch.class, "user-dog");

        // from /animal/mammal/dog, not allowed to switch all the way back to trunk
        cd(new File(WC1, "animal/mammal/dog"));
        try {
            savana(SetBranch.class, "trunk");
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Can't switch to a branch with a root above the working copy." +
                            "\nWorking Copy Root: <project>/animal/mammal/dog" +
                            "\nBranch Root:       <project>/" + EOL, e.getErr());
        }
        // from /animal/mammal/dog, not allowed to switch parent to a different branch
        try {
            savana(SetBranch.class, "user-animal2");
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Can't switch to a branch with a root above the working copy." +
                            "\nWorking Copy Root: <project>/animal/mammal/dog" +
                            "\nBranch Root:       <project>/animal" + EOL, e.getErr());
        }
        // from /animal/mammal/dog, may switch back to match the parent
        savana(SetBranch.class, "user-animal");
        // ... and now it's ok to switch to user-animal2
        savana(SetBranch.class, "user-animal2");

        // create a new subdirectory and promote it using the 'user-animal2' workspace
        addFile(new File(WC1, "animal/mammal/cat/siamese.txt"), "meow");
        SVN.getCommitClient().doCommit(new File[]{WC1},
                false, "user branch commit - added cat", null, null, false, false, SVNDepth.INFINITY);
        savana(Promote.class, "-m", "trunk - added cat");

        // create a user branch scoped to the new directory
        savana(CreateUserBranch.class, "user-cat", "../cat");

        // switch back to the user-animal workspace which doesn't have a cat directory
        savana(SetBranch.class, "user-animal", "--force");
        // from here, we can't switch to the 'user-cat' workspace since the cat subdirectory doesn't exist in user-animal
        cd(new File(WC1, "animal/mammal"));
        try {
            savana(SetBranch.class, "user-cat");
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Branch root must be a versioned directory.\nPath: " + new File(WC1, "animal/mammal/cat") + EOL, e.getErr());
        }
    }

    private void addFile(File file, String data) throws SVNException, IOException {
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, data);
        SVNWCClient wcClient = SVN.getWCClient();
        wcClient.doAdd(file, false, false, true, SVNDepth.INFINITY, false, true);
    }
}
