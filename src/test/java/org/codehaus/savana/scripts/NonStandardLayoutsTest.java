package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.savana.scripts.admin.CreateMetadataFile;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NonStandardLayoutsTest extends SavanaScriptsTestCase {

    private static final Log log = LogFactory.getLog(NonStandardLayoutsTest.class);

    public void testNonstandardLayout() throws Exception {
        // this is actually the "standard" configuration...
        nonstandardConfigurationTest("myproject", "myproject/trunk",
                                     "trunk", "branches", "branches/user", "myproject");
        // this assumes that there is a top-level "projects" directory that contains our project,
        // which is otherwise standard layout
        nonstandardConfigurationTest("myproject", "projects/myproject/trunk",
                                     "trunk", "branches", "branches/user", "projects/myproject");
        // this assumes a deep "projects" directory, with release branches in branches/release
        nonstandardConfigurationTest("myproject", "user/sample/projects/myproject/trunk",
                                     "trunk", "branches/release", "branches/user",
                                     "user/sample/projects/myproject");
        // this assumes that trunk is called "head", and user branches are put in "workspaces"
        nonstandardConfigurationTest("myproject", "myproject/head",
                                     "head", "releases", "workspaces", "myproject");
        // this assumes that "trunk" and "branches" exist at the repo root, and projects exist
        // as children of trunk/branches
        nonstandardConfigurationTest("myproject", "trunk/myproject",
                                     "trunk/myproject", "branches", "branches/user", "/");
    }

    private void nonstandardConfigurationTest(String projectName,
                                              String branchPath,
                                              String trunkDir,
                                              String branchesDir,
                                              String userBranchesDir,
                                              String projectDir) throws Exception {
        // create a new test repository
        log.info("creating test repository");
        SVNAdminClient adminClient = SVN.getAdminClient();
        File repoDir = tempDir("non-standard-repo");
        SVNURL repoUrl = adminClient.doCreateRepository(repoDir, null, false, true);

        // create directories along the path to trunk
        SVNURL trunkPath = repoUrl;
        String[] path = branchPath.split("/");
        for (String dir : path) {
            trunkPath = trunkPath.appendPath(dir, true);
            SVN.getCommitClient().doMkDir(new SVNURL[]{trunkPath}, "initial creation");
        }

        // check out a working copy of the repo
        File wc = tempDir("wc");
        SVN.getUpdateClient().doCheckout(trunkPath, wc, SVNRevision.UNDEFINED,
                                         SVNRevision.HEAD, true);
        cd(wc);

        // bootstrap the project
        savana(CreateMetadataFile.class,
               projectName,
               branchPath,
               "TRUNK",
               "-p", projectDir,
               "-b", branchesDir,
               "-u", userBranchesDir,
               "-t", trunkDir);

        // add a file to the trunk
        File textFile = new File(wc, "file.txt");
        FileUtils.writeStringToFile(textFile, "CONTENTS");
        SVN.getWCClient().doAdd(textFile, false, false, false, false);
        SVN.getCommitClient().doCommit(new File[]{wc}, false, "committing", false, true);

        // create a branch, and a user branch from that branch.
        savana(CreateBranch.class, "1.0");
        savana(CreateUserBranch.class, "user-1.0", "-m", "custom commit message");

        // set the wc back to the 1.0 branch
        savana(SetBranch.class, "1.0");
        assertTrue(savana(ListWorkingCopyInfo.class)
                .contains("Branch Name:           1.0"));

        // edit the file in 1.0
        FileUtils.writeStringToFile(textFile, "NEW CONTENTS FROM 1.0");
        SVN.getCommitClient().doCommit(new File[]{wc}, false, "committing", false, true);

        // set the wc to user-1.0 and sync the changes
        savana(SetBranch.class, "user-1.0");
        assertTrue(savana(ListWorkingCopyInfo.class)
                .contains("Branch Name:           user-1.0"));
        assertEquals("CONTENTS", FileUtils.readFileToString(textFile));
        savana(Synchronize.class);
        assertEquals("NEW CONTENTS FROM 1.0", FileUtils.readFileToString(textFile));
        SVN.getCommitClient().doCommit(new File[]{wc}, false,
                                       "synced from release branch", false, true);

        // set the wc to the trunk and back - just to show we can
        savana(SetBranch.class, "-C", "trunk");
        // TODO: I'm not 100% sure this is the exact behavior we want for TRUNK
        assertTrue(savana(ListWorkingCopyInfo.class)
                .contains("Branch Name:           " +
                          trunkDir.substring(trunkDir.lastIndexOf("/") + 1)));
        savana(SetBranch.class, "-C", "user-1.0");
        assertTrue(savana(ListWorkingCopyInfo.class)
                .contains("Branch Name:           user-1.0"));

        // get a list of everything in the repo
        final Set<String> paths = new HashSet<String>();
        SVN.getLogClient().doList(
                repoUrl, SVNRevision.UNDEFINED, SVNRevision.HEAD, true,
                new ISVNDirEntryHandler() {
                    public void handleDirEntry(SVNDirEntry svnDirEntry) throws SVNException {
                        String path = svnDirEntry.getRelativePath();
                        log.info("path: " + path);
                        paths.add(path);
                    }
                });
        // assert that the correct directory for trunk, 1.0, and user-1.0 exist, and that each
        // contains .savana and file.txt
        for (String branch : Arrays.asList(trunkDir,
                                           branchesDir + "/1.0",
                                           userBranchesDir + "/user-1.0")) {
            String eachBranchPath = (projectDir + "/" + branch).replaceAll("\\/+", "/")
                    .replaceAll("^\\/", "");
            assertTrue(eachBranchPath, paths.contains(eachBranchPath));
            assertTrue(eachBranchPath + "/.savana", paths.contains(eachBranchPath + "/.savana"));
            assertTrue(eachBranchPath + "/file.txt", paths.contains(eachBranchPath + "/file.txt"));
        }

        // edit the file in user-1.0
        FileUtils.writeStringToFile(textFile, "NEW CONTENTS FROM user-1.0");
        SVN.getCommitClient().doCommit(new File[]{wc}, false, "committing", false, true);

        // promote the workspace
        savana(Synchronize.class);

        SVN.getUpdateClient().doUpdate(wc, SVNRevision.HEAD, false);
        savana(Promote.class, "promoting my changes");

        // check that we're back to 1.0, and that the changes are available to us
        assertTrue(savana(ListWorkingCopyInfo.class)
                .contains("Branch Name:           1.0"));
        assertEquals("NEW CONTENTS FROM user-1.0", FileUtils.readFileToString(textFile));

        // check that the user-1.0 branch is now gone
        SVN.getLogClient().doList(
                repoUrl, SVNRevision.UNDEFINED, SVNRevision.HEAD, true,
                new ISVNDirEntryHandler() {
                    public void handleDirEntry(SVNDirEntry svnDirEntry) throws SVNException {
                        String path = svnDirEntry.getRelativePath();
                        assertFalse(path.contains("user-1.0"));
                    }
                });

    }
}