package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.logging.Logger;

public class WorkspaceCodeFreezeTest extends AbstractSavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(WorkspaceCodeFreezeTest.class.getName());

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    /**
     * test the code freezing functionality
     * @throws Exception on error
     */
    public void testCodeFreeze() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");

        // update the wc
        log.info("updating working copy");
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false);

        // check that we are starting in the trunk
        assertEquals("trunk", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());

        // set the code freeze property
        log.info("setting code freeze property to true");
        SVN.getWCClient().doSetProperty(new WorkingCopyInfo(SVN).getMetadataFile(),
                MetadataFile.PROP_CODE_FROZEN, SVNPropertyValue.create("true"), false, SVNDepth.EMPTY, null, null);
        SVN.getCommitClient().doCommit(new File[]{WC1}, false, "branch admin - setting code freeze", null, null, false, false, SVNDepth.INFINITY);

        // update the wc
        log.info("updating working copy");
        long branchPointRev = SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false);

        // create a new workspace
        log.info("creating workspace");
        savana(CreateUserBranch.class, "workspace");

        // open a file in the wc, edit it, and write it back
        log.info("editing src/text/animals.txt");
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));

        // check in the change
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - changed monkey to mongoose", null, null, false, false, SVNDepth.INFINITY);

        // check that we're still in the "workspace" branch
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());

        // sync from trunk (should be a no-op since there aren't any changes to sync)
        log.info("syncing from trunk");
        savana(Synchronize.class);

        // promote the change to trunk
        try {
            log.info("promoting change to trunk without code freeze break message");
            savana(Promote.class, "-m", "trunk - promoting monkey -> mongoose change");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E200015: The workspacecodefreezetest trunk workspace is currently under code freeze. " +
                    "Please try again when code freeze is lifted.\n", e.getErr());
        }

        // list user branches - there should still be the branch that failed to promote
        assertEquals("------------------------------------------------------------------------------\n" +
                     "Branch Name            Source        Branch-Point  Last-Merge    Subpath\n" +
                     "------------------------------------------------------------------------------\n" +
                     "workspace              trunk         " + pad(branchPointRev, 14) + branchPointRev,
                     savana(ListUserBranches.class));

        // check that we are still in the "workspace" branch
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());

        // try to promote again using the default code freeze break message
        log.info("promoting change to trunk with code freeze break message");
        savana(Promote.class, "-m", "trunk - promoting monkey -> mongoose change - code freeze break");

        // list user branches - there should be none
        assertEquals("No branches were found.",
                     savana(ListUserBranches.class));

        // check that we're back in the "trunk" branch
        assertEquals("trunk", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());

        // read the file, and check that our change has been made
        assertTrue(FileUtils.readFileToString(animalsFile, "UTF-8").contains("mongoose"));
    }

    private String pad(long rev, int n) {
        return StringUtils.rightPad(Long.toString(rev), n);
    }
}
