package org.codehaus.savana.scripts;

import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;

public class CreateRemoteBranchTest extends AbstractSavanaScriptsTestCase {
    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testCreateRemoteUserBranch() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        long rev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

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

    public void testCreateRemoteReleaseBranch() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        long rev = SVN.getStatusClient().doStatus(WC1, false).getRevision().getNumber();

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

        //Make sure we can switch to the release branch
        savana(SetBranch.class, "b1.1.x", "--changeRoot");
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
