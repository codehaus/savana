package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.PathUtil;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.text.MessageFormat;

/**
 * Basic tests for creating user branches on subdirectories (ie.where SOURCE_SUBPATH is not empty).
 * Verifies that basic operations work correctly on those user branches. 
 */
public class CreateSubbranchTest extends AbstractSavanaScriptsTestCase {

    private static final String EOL = System.getProperty("line.separator");

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testUserSubbranches() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);
        long rev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

        // create a user branch with a subpath of "src"
        cd(WC1);
        savana(CreateUserBranch.class, "user1-src", "src");
        long branchPointRev1 = rev++;

        // list the working copy info and check it
        assertEquals(
                WC1 + ":" + EOL +
                "---------------------------------------------" + EOL +
                "Branch Name:           trunk" + EOL +
                "---------------------------------------------" + EOL +
                "Project Name:          " + projectName + EOL +
                "Branch Type:           trunk" + EOL +
                "Source:                none" + EOL +
                "Branch Point Revision: none" + EOL +
                "Last Merge Revision:   none",
                savana(ListWorkingCopyInfo.class));

        // change into the subbranch and list the working copy info
        File WC1_src = new File(WC1, "src");
        cd(WC1_src);
        assertEquals(
                WC1_src + ":" + EOL +
                "---------------------------------------------" + EOL +
                "Branch Name:           user1-src" + EOL +
                "Branch Subpath:        src" + EOL +
                "---------------------------------------------" + EOL +
                "Project Name:          " + projectName + EOL +
                "Branch Type:           user branch" + EOL +
                "Source:                trunk" + EOL +
                "Branch Point Revision: " + branchPointRev1 + EOL +
                "Last Merge Revision:   " + branchPointRev1,
                savana(ListWorkingCopyInfo.class));

        // open a file in the wc, edit it, and write it back
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "user branch commit - changed monkey to mongoose", null, null, false, false, SVNDepth.INFINITY);
        rev++;

        // list the changes from the trunk, and check that the output is what we expect
        assertEquals("Modified Files:" + EOL +
                     "-------------------------------------------------" + EOL +
                     "text/animals.txt",
                savana(ListChangesFromSource.class));

        SVNURL trunkUrl = REPO_URL.appendPath(projectName, false).appendPath("trunk", false);
        assertEquals(
                MessageFormat.format(
                        "Index: text/animals.txt\n" +
                        "===================================================================\n" +
                        "--- text/animals.txt\t(.../{0}/src)\t(revision {2})\n" +
                        "+++ text/animals.txt\t(...{1}/src)\t(working copy)\n" +
                        "@@ -1,4 +1,4 @@\n" +
                        "-monkey\n" +
                        "+mongoose\n" +
                        " dog\n" +
                        " rat\n" +
                        " dragon\n" +
                        "\\ No newline at end of file",
                        trunkUrl.toString(),
                        toSvnkitAbsolutePath(WC1),
                        branchPointRev1),
                savana(DiffChangesFromSource.class).replace("\r", ""));

        // assert that sync is a no-op
        assertEquals("Branch is up to date.", savana(Synchronize.class));

        // change a file in the trunk
        File wc2_drinksFile = new File(WC2, "src/text/drinks.txt");
        String drinks = FileUtils.readFileToString(wc2_drinksFile, "UTF-8");
        FileUtils.writeStringToFile(wc2_drinksFile, drinks.replaceAll("gin", "scotch"), "UTF-8");
        SVN.getCommitClient().doCommit(
                new File[]{WC2}, false, "trunk - changed gin to scotch", null, null, false, false, SVNDepth.INFINITY);
        rev++;

        // assert that sync brings in changes
        assertEquals(
                "U    text" + File.separatorChar + "drinks.txt",
                savana(Synchronize.class).replaceAll("^---.*?\n", ""));

        // commit.  we have to update first for some reason related to the svn:mergeinfo property
        SVN.getUpdateClient().doUpdate(WC1_src, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        SVN.getCommitClient().doCommit(new File[]{WC1_src}, false, "user branch sync", null, null, false, false, SVNDepth.INFINITY);
        long lastMergeRev1 = rev++;

        // list the working copy info and check it
        assertEquals(
                WC1_src + ":" + EOL +
                "---------------------------------------------" + EOL +
                "Branch Name:           user1-src" + EOL +
                "Branch Subpath:        src" + EOL +
                "---------------------------------------------" + EOL +
                "Project Name:          " + projectName + EOL +
                "Branch Type:           user branch" + EOL +
                "Source:                trunk" + EOL +
                "Branch Point Revision: " + branchPointRev1 + EOL +
                "Last Merge Revision:   " + lastMergeRev1,
                savana(ListWorkingCopyInfo.class));

        // list the changes from the trunk, and check that the output is what we expect (sync merged in all changes)
        assertEquals("Modified Files:" + EOL +
                     "-------------------------------------------------" + EOL +
                     "text/animals.txt",
                savana(ListChangesFromSource.class));

        // diff the changes from the trunk, and check that the output is what we expect (sync merged in all changes)
        assertEquals(
                MessageFormat.format(
                        "Index: text/animals.txt\n" +
                        "===================================================================\n" +
                        "--- text/animals.txt\t(.../{0}/src)\t(revision {2})\n" +
                        "+++ text/animals.txt\t(...{1}/src)\t(working copy)\n" +
                        "@@ -1,4 +1,4 @@\n" +
                        "-monkey\n" +
                        "+mongoose\n" +
                        " dog\n" +
                        " rat\n" +
                        " dragon\n" +
                        "\\ No newline at end of file",
                        trunkUrl.toString(),
                        toSvnkitAbsolutePath(WC1),
                        lastMergeRev1),
                savana(DiffChangesFromSource.class).replace("\r", ""));

        // create a second user branch.  verify that it's created off the trunk and the subpath is set correctly
        File WC1_src_text = new File(WC1_src, "text");
        cd(WC1_src_text);
        try {
            savana(CreateUserBranch.class, "user1-src-text", ".");
            assertTrue("we expected an exception to be thrown", false);
        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: ERROR: Cannot create a user branch in a subdirectory of another user branch." +
                         "\nSwitch to 'trunk' or a release branch before creating the new branch." + EOL, e.getErr());
        }
        savana(SetBranch.class, "trunk");
        savana(CreateUserBranch.class, "user1-src-text", ".");
        long branchPointRev2 = rev;

        assertEquals(
                WC1_src_text + ":" + EOL +
                "---------------------------------------------" + EOL +
                "Branch Name:           user1-src-text" + EOL +
                "Branch Subpath:        src/text" + EOL +
                "---------------------------------------------" + EOL +
                "Project Name:          " + projectName + EOL +
                "Branch Type:           user branch" + EOL +
                "Source:                trunk" + EOL +
                "Branch Point Revision: " + branchPointRev2 + EOL +
                "Last Merge Revision:   " + branchPointRev2,
                savana(ListWorkingCopyInfo.class));

        // list the two user branches - there should be just the one
        assertEquals("------------------------------------------------------------------------------" + EOL +
                     "Branch Name            Source        Branch-Point  Last-Merge    Subpath" + EOL +
                     "------------------------------------------------------------------------------" + EOL +
                     "user1-src              trunk         " + pad(branchPointRev1, 14) + pad(lastMergeRev1, 14) + "src" + EOL +
                     "user1-src-text         trunk         " + pad(branchPointRev2, 14) + pad(branchPointRev2, 14) + "src/text",
                savana(ListUserBranches.class));

        // recursively list workspace info
        cd(WC1);
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           trunk\n" +
                "---------------------------------------------\n" +
                "Project Name:          createsubbranchtest\n" +
                "Branch Type:           trunk\n" +
                "Source:                none\n" +
                "Branch Point Revision: none\n" +
                "Last Merge Revision:   none\n" +
                "\n" +
                WC1_src_text + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           user1-src-text\n" +
                "Branch Subpath:        src/text\n" +
                "---------------------------------------------\n" +
                "Project Name:          createsubbranchtest\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + branchPointRev2 + "\n" +
                "Last Merge Revision:   " + branchPointRev2,
                savana(ListWorkingCopyInfo.class, "--recursive").replace("\r", ""));

        // switch from user1-src-text to user1-src
        cd(WC1_src);
        savana(SetBranch.class, "user1-src", "--force");

        // promote animals.txt
        savana(Promote.class, "-m", "trunk - changed animals.txt");

        // recursively list workspace info.  promote should have left us in the trunk
        cd(WC1);
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           trunk\n" +
                "---------------------------------------------\n" +
                "Project Name:          createsubbranchtest\n" +
                "Branch Type:           trunk\n" +
                "Source:                none\n" +
                "Branch Point Revision: none\n" +
                "Last Merge Revision:   none",
                savana(ListWorkingCopyInfo.class, "--recursive").replace("\r", ""));
    }

    /** Returns the absolute path of a file in the way that matches svnkit. */
    private String toSvnkitAbsolutePath(File file) {
        String path = PathUtil.normalizePath(file);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    private String pad(long rev, int n) {
        return StringUtils.rightPad(Long.toString(rev), n);
    }
}