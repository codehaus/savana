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
        nonstandardConfigurationTest(null, "myproject", "trunk", "branches", "branches/user");
        nonstandardConfigurationTest("projects", "myproject", "trunk", "branches", "branches/user");
        nonstandardConfigurationTest("user/sample/projects", "myproject", "trunk", "branches/release", "branches/user");
        nonstandardConfigurationTest(null, "myproject", "head", "releases", "workspaces");
    }

    private void nonstandardConfigurationTest(String projectsDir,
                                              String projectName,
                                              String trunkDir,
                                              String branchesDir,
                                              String userBranchesDir) throws Exception {
        log.info("creating test repository");
        SVNAdminClient adminClient = SVN.getAdminClient();
        File repoDir = tempDir("non-standard-repo");
        SVNURL repoUrl = adminClient.doCreateRepository(repoDir, null, false, true);
        SVNURL projectsUrl = repoUrl;
        if (projectsDir != null) {
            String[] path = projectsDir.split("/");
            for (String dir : path) {
                projectsUrl = projectsUrl.appendPath(dir, true);
                SVN.getCommitClient().doMkDir(new SVNURL[]{projectsUrl}, "initial creation");
            }
        }

        SVNURL projUrl = projectsUrl.appendPath(projectName, true);
        SVNURL trunkPath = projUrl.appendPath(trunkDir, true);

        SVN.getCommitClient().doMkDir(new SVNURL[]{projUrl, trunkPath}, "initial creation");

        File wc = tempDir("wc");

        SVN.getUpdateClient().doCheckout(trunkPath, wc, SVNRevision.UNDEFINED, SVNRevision.HEAD, true);
        cd(wc);

        String projectDir = projectsDir == null ? projectName : (projectsDir + "/" + projectName);

        savana(CreateMetadataFile.class,
               projectName, projectDir + "/" + trunkDir, "TRUNK",
               "-p", projectDir,
               "-b", branchesDir,
               "-u", userBranchesDir,
               "-t", trunkDir);

        File textFile = new File(wc, "file.txt");
        FileUtils.writeStringToFile(textFile, "CONTENTS");

        SVN.getWCClient().doAdd(textFile, false, false, false, false);
        SVN.getCommitClient().doCommit(new File[]{wc}, false, "committing", false, true);

        savana(CreateBranch.class, "1.0");
        savana(CreateUserBranch.class, "user-1.0", "-m", "custom commit message");

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
        for (String branch : Arrays.asList(trunkDir, branchesDir + "/1.0", userBranchesDir + "/user-1.0")) {
            String branchPath = projectDir + "/" + branch;
            assertTrue(branchPath, paths.contains(branchPath));
            assertTrue(branchPath + "/.savana", paths.contains(branchPath + "/.savana"));
            assertTrue(branchPath + "/file.txt", paths.contains(branchPath + "/file.txt"));
        }
    }
}