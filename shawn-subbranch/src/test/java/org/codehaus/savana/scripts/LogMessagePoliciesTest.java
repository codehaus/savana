package org.codehaus.savana.scripts;

import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;
import java.util.logging.Logger;

public class LogMessagePoliciesTest extends AbstractSavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(LogMessagePoliciesTest.class.getName());

    private static final String EOL = System.getProperty("line.separator");

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testLogMessagePolicies() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");

        //
        // Promote to TRUNK
        //
        log.info("testing savana policies promote to trunk");
        savana(CreateUserBranch.class, "user1");

        // test invalid promotion commit messages
        assertTrunkPromoteFails("user branch commit");
        assertTrunkPromoteFails("user1");
        assertTrunkPromoteFails("b3.3.x - fix bug in the branch");
        assertTrunkPromoteFails("trunkfix");
        assertTrunkPromoteFails("branch admin");  // branch admin is NOT allowed in Savana!  Savana doesn't have any commands for doing branch administration.

        // test valid promotion commit messages
        assertTrunkPromoteSucceeds("trunk - #14211 - fixed a bug");
        assertTrunkPromoteSucceeds("trunk");

        savana(SetBranch.class, "trunk");
        savana(DeleteUserBranch.class, "user1");


        //
        // Promote to RELEASE BRANCH
        //
        log.info("testing savana policies promote to release branch");
        savana(CreateReleaseBranch.class, "b3.3.x");
        savana(CreateUserBranch.class, "user1");

        // test invalid promotion commit messages
        assertReleaseBranchPromoteFails("user branch commit");
        assertReleaseBranchPromoteFails("user1");
        assertReleaseBranchPromoteFails("trunk - fix bug in the trunk");
        assertReleaseBranchPromoteFails("b3.3.xfix");
        assertReleaseBranchPromoteFails("branch admin");  // branch admin is NOT allowed in Savana!  Savana doesn't have any commands for doing branch administration.

        // test valid promotion commit messages
        assertReleaseBranchPromoteSucceeds("b3.3.x\n#14211 - fixed a bug\nreviews by sam");
        assertReleaseBranchPromoteSucceeds("b3.3.x");

        // don't delete the user branch since we'll use it for the next sequence...


        //
        // Promote to USER BRANCH 
        //
        log.info("testing savana policies promote to user branch");

        // test invalid promotion commit messages
        assertUserBranchPromoteFails("done");
        assertUserBranchPromoteFails("trunk - #14211 - fixed a bug");
        assertUserBranchPromoteFails("b3.3.x - #14211 - fixed a bug");
        assertUserBranchPromoteFails("user1fix");
        assertUserBranchPromoteFails("branch admin");  // branch admin is NOT allowed in Savana!  Savana doesn't have any commands for doing branch administration.

        // test valid promotion commit messages
        assertUserBranchPromoteSucceeds("user branch commit");
        assertUserBranchPromoteSucceeds("user branch commit\nmade changes\npassed tests");
        assertUserBranchPromoteSucceeds("user branch");
        assertUserBranchPromoteSucceeds("user branch. made changes, passed tests");
        assertUserBranchPromoteSucceeds("user branch\r\nmade changes\r\npassed tests");
        assertUserBranchPromoteSucceeds("user1 - fix");
        assertUserBranchPromoteSucceeds("user1\nfix\npassed tests");

        savana(SetBranch.class, "trunk", "--changeRoot");
        savana(DeleteUserBranch.class, "user1");


        //
        // Remove the policy info and verify that it's ignored
        //

        // in user branch, remove the policy subversion property from the metadata file
        savana(CreateUserBranch.class, "user1");
        SVN.getWCClient().doSetProperty(new File(WC, MetadataFile.METADATA_FILE_NAME), MetadataFile.PROP_SAVANA_POLICIES,
                null, false, SVNDepth.EMPTY, null, null);
        SVN.getCommitClient().doCommit(new File[]{WC}, false, "user branch commit", null, null, false, false, SVNDepth.INFINITY);
        // promote.  should fail because the trunk hasn't been updated yet and the promote destination drives the policy data.
        assertTrunkPromoteFails("blah blah blah");

        // in trunk, remove the policy subversion property from the metadata file
        savana(SetBranch.class, "trunk");
        // remove the policy subversion property from the metadata file
        SVN.getWCClient().doSetProperty(new File(WC, MetadataFile.METADATA_FILE_NAME), MetadataFile.PROP_SAVANA_POLICIES,
                null, false, SVNDepth.EMPTY, null, null);
        // commit should fail because you're not allowed to modify .savana in the trunk (pre-commit hook)
        try {
            SVN.getCommitClient().doCommit(new File[]{WC}, false, "user branch commit", null, null, false, false, SVNDepth.INFINITY);
            assertTrue("we expected an exception to be thrown", false);
        } catch (SVNException e) {
            assertEquals("svn: Commit failed (details follow):\n" +
                    "svn: Commit blocked by pre-commit hook (exit code 1) with output:\n" +
                    "The changeset may not modify Savana metadata files in the trunk or in a release branch:" +
                    "\n  workspace: trunk\n  metadata file: " + projectName + "/trunk/.savana",
                    e.getMessage().trim().replace("\r", ""));
        }
        // try again as branch admin
        SVN.getCommitClient().doCommit(new File[]{WC}, false, "branch admin - remove savana policies", null, null, false, false, SVNDepth.INFINITY);

        // ok, savana policies have been removed.  we should be able to promote with a random log message.
        savana(SetBranch.class, "user1");
        savana(Synchronize.class);
        TestRepoUtil.touchCounterFile(WC);
        SVN.getCommitClient().doCommit(new File[]{WC}, false, "user branch commit", null, null, false, false, SVNDepth.INFINITY);
        try {
            savana(Promote.class, "-m", "blah blah blah");
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: Commit failed (details follow):\n" +
                    "svn: Commit blocked by pre-commit hook (exit code 1) with output:\n" +
                    "The subversion commit comment must start with the name of the modified workspace:" +
                    "\n  workspace: trunk\n  commit comment: blah blah blah",
                    e.getErr().trim().replace("\r", ""));
        }
    }

    private void assertTrunkPromoteFails(String logMessage) throws Exception {
        try {
            savana(Promote.class, "-m", logMessage);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: The commit comment must start with the name of the modified workspace:\n" +
                    "  workspace: trunk\n  commit comment: " + logMessage + EOL, e.getErr());
        }
    }

    private void assertTrunkPromoteSucceeds(String logMessage) throws Exception {
        // test the promote.  if it works then it won't throw an exception
        savana(Promote.class, "-m", logMessage);

        // restore the workspace we just promoted (always user1 in this test)
        savana(CreateUserBranch.class, "user1");
    }

    private void assertReleaseBranchPromoteFails(String logMessage) throws Exception {
        try {
            savana(Promote.class, "-m", logMessage);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: The commit comment must start with the name of the modified workspace:\n" +
                    "  workspace: b3.3.x\n  commit comment: " + logMessage + EOL, e.getErr());
        }
    }

    private void assertReleaseBranchPromoteSucceeds(String logMessage) throws Exception {
        // test the promote.  if it works then it won't throw an exception
        savana(Promote.class, "-m", logMessage);

        // restore the workspace we just promoted (always user1 in this test)
        savana(CreateUserBranch.class, "user1");
    }

    private void assertUserBranchPromoteFails(String logMessage) throws Exception {
        MetadataProperties wcProps = new WorkingCopyInfo(SVN).getMetadataProperties();
        try {
            wcProps.getSavanaPolicies().validateLogMessage(logMessage, wcProps);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SVNException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: The commit comment must start with \"user branch\" or the name of the modified workspace:\n" +
                    "  workspace: user1\n  commit comment: " + logMessage, e.getMessage());
        }
    }

    private void assertUserBranchPromoteSucceeds(String logMessage) throws Exception {
        MetadataProperties wcProps = new WorkingCopyInfo(SVN).getMetadataProperties();
        wcProps.getSavanaPolicies().validateLogMessage(logMessage, wcProps);
    }
}
