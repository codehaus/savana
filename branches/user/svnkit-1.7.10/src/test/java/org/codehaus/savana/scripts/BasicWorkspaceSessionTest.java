package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Logger;

public class BasicWorkspaceSessionTest extends AbstractSavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(BasicWorkspaceSessionTest.class.getName());

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

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
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");

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
        assertEquals("------------------------------------------------------------------------------\n" +
                     "Branch Name            Source        Branch-Point  Last-Merge    Subpath\n" +
                     "------------------------------------------------------------------------------\n" +
                     "workspace              trunk         " + pad(branchPointRev, 14) + branchPointRev,
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


        SVNURL trunkUrl = REPO_URL.appendPath(projectName, false).appendPath("trunk", false);
        assertEquals(
                MessageFormat.format(
                        "Index: src/text/animals.txt\n" +
                        "===================================================================\n" +
                        "--- src/text/animals.txt\t(revision {0})\n" +
                        "+++ src/text/animals.txt\t(working copy)\n" +
                        "@@ -1,4 +1,4 @@\n" +
                        "-monkey\n" +
                        "+mongoose\n" +
                        " dog\n" +
                        " rat\n" +
                        " dragon\n" +
                        "\\ No newline at end of file",
                        branchPointRev),
                savana(DiffChangesFromSource.class));

        // check that we're still in the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals("Modified Files:\n" +
                     "-------------------------------------------------\n" +
                     "src/text/animals.txt",
                savana(ListChangesFromSource.class));

        // move the file "autos.txt" to "cars.txt"
        File autosFile = new File(WC1, "src/text/autos.txt");
        File carsFile = new File(WC1, "src/text/cars.txt");
        log.info("moving src/text/autos.txt to src/text/cars.txt");
        SVN.getMoveClient().doMove(autosFile, carsFile);

        // check in the change
        log.info("committing change");
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit\r\nrenamed autos.txt to cars.txt", null, null, false, false, SVNDepth.INFINITY);

        // check that we're still in the "workspace" branch, and that the revision has updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(++rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals(
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
                savana(ListChangesFromSource.class));

        // sync from trunk (should be a no-op since there aren't any changes to sync)
        log.info("syncing from trunk");
        savana(Synchronize.class);

        // check that we're still in the "workspace" branch, and that the revision has NOT updated
        assertEquals("workspace", new WorkingCopyInfo(SVN).getMetadataProperties().getBranchName());
        assertEquals(rev, SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.FILES, false, false));

        // list the working copy info and check it
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           workspace\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + branchPointRev + "\n" +
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

    private String pad(long rev, int n) {
        return StringUtils.rightPad(Long.toString(rev), n);
    }
}
