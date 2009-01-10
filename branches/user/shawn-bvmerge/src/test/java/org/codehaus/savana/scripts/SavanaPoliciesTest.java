package org.codehaus.savana.scripts;

import org.codehaus.savana.BranchType;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNEditorHelper;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.util.logging.Logger;
import java.io.File;

/**
 */
public class SavanaPoliciesTest extends AbstractSavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(SavanaPoliciesTest.class.getName());

    private static final String EOL = System.getProperty("line.separator");

    private static final String PROJECT_NAME = "savanapoliciespresent";

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testSavanaPoliciesPresent() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        File WC = TestRepoUtil.setupProjectWithWC(REPO_URL, PROJECT_NAME, true, true, "test-project");

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
        assertReleaseBranchPromoteSucceeds("b3.3.x - #14211 - fixed a bug");
        assertReleaseBranchPromoteSucceeds("b3.3.x");

        // don't delete the user branch since we'll use it as the parent for the next sequence...


        //
        // Promote to USER BRANCH 
        //
        log.info("testing savana policies promote to user branch");
        createChainedUserBranch(REPO_URL, PROJECT_NAME, "user1", "user2");

        // test invalid promotion commit messages
        assertUserBranchPromoteFails("done");
        assertUserBranchPromoteFails("trunk - #14211 - fixed a bug");
        assertUserBranchPromoteFails("b3.3.x - #14211 - fixed a bug");
        assertUserBranchPromoteFails("user1fix");
        assertUserBranchPromoteFails("branch admin");  // branch admin is NOT allowed in Savana!  Savana doesn't have any commands for doing branch administration.

        // test valid promotion commit messages
        assertUserBranchPromoteSucceeds("user branch commit");
        assertUserBranchPromoteSucceeds("user branch");
        assertUserBranchPromoteSucceeds("user1 - fix");

        savana(DeleteUserBranch.class, "user2");
        savana(DeleteUserBranch.class, "user1");


        //
        // Remove the policy info and verify that it's ignored
        //
        savana(SetBranch.class, "trunk", "--changeRoot");

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
                    "The changeset may not modify Savana metadata files in the trunk or in a release branch:\n" +
                    "  workspace: trunk\n  metadata file: savanapoliciespresent/trunk/.savana" + EOL, e.getMessage());
        }
        // try again as branch admin
        SVN.getCommitClient().doCommit(new File[]{WC}, false, "branch admin - remove savana policies", null, null, false, false, SVNDepth.INFINITY);

        // ok, savana policies have been removed.  we should be able to promote with a random log message.
        savana(SetBranch.class, "user1");
        savana(Synchronize.class);
        assertTrunkPromoteSucceeds("blah blah blah");
    }

    public void testSavanaPoliciesMissing() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = "savanapoliciesmissing";
        TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, false, "test-project");

        // invalid commit messages should be passed by Savana but rejected by the commit hook
        savana(CreateUserBranch.class, "user3");
        try {
            savana(Promote.class, "-m", "done");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            assertTrue(e.getErr(), e.getErr().endsWith("svn: Commit failed (details follow):\n" +
                    "svn: Commit blocked by pre-commit hook (exit code 1) with output:\n" +
                    "The subversion commit comment must start with the name of the modified workspace:\n" +
                    "  workspace: trunk\n  commit comment: done" + EOL));
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
        try {
            savana(Promote.class, "-m", logMessage);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: The commit comment must start with \"user branch\" or the name of the modified workspace:\n" +
                    "  workspace: user1\n  commit comment: " + logMessage + EOL, e.getErr());
        }
    }

    private void assertUserBranchPromoteSucceeds(String logMessage) throws Exception {
        // test the promote.  if it works then it won't throw an exception
        savana(Promote.class, "-m", logMessage);

        // restore the workspace we just promoted (always user2 in this test)
        createChainedUserBranch(REPO_URL, PROJECT_NAME, "user1", "user2");
    }


    /** Create a user branch that is a child of another user branch--not currently natively supported in Savana. */
    private void createChainedUserBranch(SVNURL repoUrl, String projectName, String sourceBranch, String targetBranch) throws Exception {
        SVNRepository repository = SVN.createRepository(repoUrl, true);
        repository.setLocation(repoUrl, false);
        long sourceRevision = repository.getLatestRevision();

        String commitMessage = targetBranch + " - creating child user branch from " + sourceBranch;
        String userBranchesParent = SVNPathUtil.append(projectName, BranchType.USER_BRANCH.getDefaultPath());
        String sourceBranchPath = SVNPathUtil.append(userBranchesParent, sourceBranch);
        String userBranchPath = SVNPathUtil.append(userBranchesParent, targetBranch);
        String metadataFilePath = SVNPathUtil.append(userBranchPath, MetadataFile.METADATA_FILE_NAME);

        //Create an editor
        ISVNEditor editor = repository.getCommitEditor(commitMessage, null, false, null, null);
        SVNEditorHelper editorHelper = new SVNEditorHelper(editor);
        editor.openRoot(-1);

        //Open the target directory and copy the source branch to the target
        editorHelper.openDir(userBranchesParent);
        editor.addDir(userBranchPath, sourceBranchPath, sourceRevision);
        editorHelper.addOpenedDir(userBranchPath);

        //Update all of the properties on the metadata file
        editorHelper.openFile(metadataFilePath);
        editor.changeFileProperty(metadataFilePath, MetadataFile.PROP_BRANCH_PATH, SVNPropertyValue.create(userBranchPath));
        editor.changeFileProperty(metadataFilePath, MetadataFile.PROP_SOURCE_PATH, SVNPropertyValue.create(sourceBranchPath));
        editor.changeFileProperty(metadataFilePath, MetadataFile.PROP_BRANCH_POINT_REVISION, SVNPropertyValue.create(Long.toString(sourceRevision)));
        editor.changeFileProperty(metadataFilePath, MetadataFile.PROP_LAST_MERGE_REVISION, SVNPropertyValue.create(Long.toString(sourceRevision)));

        //Close and commit all of the edits
        editorHelper.closeAll();

        savana(SetBranch.class, targetBranch, "--changeRoot");
    }
}
