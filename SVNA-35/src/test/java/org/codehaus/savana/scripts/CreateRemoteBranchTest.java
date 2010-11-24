package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;

public class CreateRemoteBranchTest extends AbstractSavanaScriptsTestCase {
    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testCreateRemoteUserBranch() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        long rev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

        // we should be able to create remote branches even if the working copy has edits
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));

        // create a remote user branch
        cd(WC1);
        assertEquals(
                "---------------------------------------------\n" +
                "Branch Name:           user1\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + rev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(CreateUserBranch.class, "user1", "--remote"));

        //Make sure the working copy is unchanged
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           trunk\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           trunk\n" +
                "Source:                none\n" +
                "Branch Point Revision: none\n" +
                "Last Merge Revision:   none",
                savana(ListWorkingCopyInfo.class));

        //Revert the file modification
        SVN.getWCClient().doRevert(new File[]{animalsFile}, SVNDepth.INFINITY, null);

        //Make sure we can switch to the user branch
        savana(SetBranch.class, "user1");
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           user1\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + rev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(ListWorkingCopyInfo.class));
    }

    public void testCreateRemoteUserSubBranch() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        long rev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

        // we should be able to create remote branches even if the working copy has edits
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));

        // create a remote user branch
        cd(WC1);
        assertEquals(
                "---------------------------------------------\n" +
                "Branch Name:           user1-src\n" +
                "Branch Subpath:        src\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + rev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(CreateUserBranch.class, "user1-src", "src", "--remote"));

        //Make sure the working copy is unchanged at the root
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           trunk\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           trunk\n" +
                "Source:                none\n" +
                "Branch Point Revision: none\n" +
                "Last Merge Revision:   none",
                savana(ListWorkingCopyInfo.class));

        // change into the subbranch and make sure that the working copy is unchanged
        File WC1_src = new File(WC1, "src");
        cd(WC1_src);
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           trunk\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           trunk\n" +
                "Source:                none\n" +
                "Branch Point Revision: none\n" +
                "Last Merge Revision:   none",
                savana(ListWorkingCopyInfo.class));

        //Revert the file modification
        SVN.getWCClient().doRevert(new File[]{animalsFile}, SVNDepth.INFINITY, null);

        //Make sure we can switch to the user sub branch
        savana(SetBranch.class, "user1-src", "--force");
        assertEquals(
                WC1_src + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           user1-src\n" +
                "Branch Subpath:        src\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           user branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + rev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(ListWorkingCopyInfo.class));
    }

    public void testCreateRemoteReleaseBranch() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        long rev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

        // we should be able to create remote branches even if the working copy has edits
        File animalsFile = new File(WC1, "src/text/animals.txt");
        String animals = FileUtils.readFileToString(animalsFile, "UTF-8");
        FileUtils.writeStringToFile(animalsFile, animals.replaceAll("key", "goose"));

        // create a remote release branch
        cd(WC1);
        assertEquals(
                "---------------------------------------------\n" +
                "Branch Name:           b1.1.x\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           release branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + rev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(CreateReleaseBranch.class, "b1.1.x", "--remote"));

        //Make sure the working copy is unchanged
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           trunk\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           trunk\n" +
                "Source:                none\n" +
                "Branch Point Revision: none\n" +
                "Last Merge Revision:   none",
                savana(ListWorkingCopyInfo.class));

        //Revert the file modification
        SVN.getWCClient().doRevert(new File[]{animalsFile}, SVNDepth.INFINITY, null);

        //Make sure we can switch to the release branch
        savana(SetBranch.class, "b1.1.x", "--force", "--changeRoot");
        assertEquals(
                WC1 + ":\n" +
                "---------------------------------------------\n" +
                "Branch Name:           b1.1.x\n" +
                "---------------------------------------------\n" +
                "Project Name:          " + projectName + "\n" +
                "Branch Type:           release branch\n" +
                "Source:                trunk\n" +
                "Branch Point Revision: " + rev + "\n" +
                "Last Merge Revision:   " + rev,
                savana(ListWorkingCopyInfo.class));
    }
}
