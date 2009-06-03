package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;

public class DeleteSubbranchTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testUserSubbranches() throws Exception {
        //Setup a test project with a working directory and import the 'test-subbranch' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-subbranch");

        //
        //Create a branch at the top-level and try to delete it
        //
        cd(WC1);
        long rootRev = createUserBranch("user-root", ".");

        assertEquals("------------------------------------------------------------------------------\n" +
                     "Branch Name            Source        Branch-Point  Last-Merge    Subpath\n" +
                     "------------------------------------------------------------------------------\n" +
                     "user-root              trunk         " + pad(rootRev, 14) + pad(rootRev, 0),
                savana(ListUserBranches.class));

        cd(new File(WC1, "animal/mammal"));
        assertDeleteFails("user-root", WC1);

        savana(SetBranch.class, "trunk");
        assertDeleteSucceeds("user-root");


        //
        //Create subbranches at the top-level and try to delete them
        //
        cd(WC1);
        long canineRev = createUserBranch("user-canine", "animal/mammal/dog");
        long dogRev = createUserBranch("user-dog", "animal/mammal/dog");
        long plantRev = createUserBranch("user-plant", "plant");

        assertEquals("------------------------------------------------------------------------------\n" +
                     "Branch Name            Source        Branch-Point  Last-Merge    Subpath\n" +
                     "------------------------------------------------------------------------------\n" +
                     "user-canine            trunk         " + pad(canineRev, 14) + pad(canineRev, 14) + "animal/mammal/dog\n" +
                     "user-dog               trunk         " + pad(dogRev, 14) + pad(dogRev, 14) + "animal/mammal/dog\n" +
                     "user-plant             trunk         " + pad(plantRev, 14) + pad(plantRev, 14) + "plant",
                savana(ListUserBranches.class));

        // deleting a branch in a subdirectory should fail
        cd(new File(WC1, "animal/mammal"));
        assertDeleteFails("user-dog", new File(WC1, "animal/mammal/dog"));
        // but switch away and it should succeed
        savana(SetBranch.class, "user-canine");
        assertDeleteSucceeds("user-dog");

        // deleting a branch in a sibling directory should fail
        cd(new File(WC1, "animal/mammal/dog"));
        assertDeleteFails("user-plant", new File(WC1, "plant"));
        // but switch to trunk and it should succeed
        cd(new File(WC1, "plant"));
        savana(SetBranch.class, "trunk");
        cd(new File(WC1, "animal/mammal/dog"));
        assertDeleteSucceeds("user-plant");

        // we can avoid the delete error by deleting the working copy too, not just setbranch away 
        cd(WC1);
        assertDeleteFails("user-canine", new File(WC1, "animal/mammal/dog"));
        FileUtils.deleteDirectory(new File(WC1, "animal/mammal"));
        assertDeleteSucceeds("user-canine");

        assertEquals("No branches were found.", savana(ListUserBranches.class));
    }

    private void assertDeleteSucceeds(String branchName) throws Exception {
        savana(DeleteUserBranch.class, branchName);
    }

    private void assertDeleteFails(String branchName, File branchDir) throws Exception {
        try {
            savana(DeleteUserBranch.class, branchName);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Use 'sav setbranch' to switch away from the branch before deleting it." +
                         "\nBranch Path: " + branchDir + "\n", e.getErr());
        }
    }

    private long createUserBranch(String branchName, String branchPath) throws Exception {
        File branchDir = new File(branchPath).getAbsoluteFile();
        savana(CreateUserBranch.class, branchName, branchDir.getPath());
        return SVN.getWCClient().doInfo(branchDir, SVNRevision.WORKING).getRevision().getNumber() - 1;
    }

    private String pad(long rev, int n) {
        return StringUtils.rightPad(Long.toString(rev), n);
    }
}
