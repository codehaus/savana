package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;

public class PathEncodingTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testUserSubbranches() throws Exception {
        // setup a test project where the project name has a space in it
        String projectName = "path encoding test";
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, null);

        // create a user branch with a space in the name
        cd(WC1);
        savana(CreateUserBranch.class, "user A");

        // create and commit documents with spaces in the names
        File docDir = new File(WC1, "My Documents" + File.separatorChar + "Really Important!");
        File docFile = new File(docDir, "great ideas.txt");
        docDir.mkdirs();
        FileUtils.writeStringToFile(docFile, "maybe later\n");
        SVN.getWCClient().doAdd(docDir.getParentFile(), false, false, false, SVNDepth.INFINITY, false, false);
        SVN.getCommitClient().doCommit(new File[]{WC1}, false, "user branch commit", null, null, false, false, SVNDepth.INFINITY);

        // switch back to the trunk
        savana(SetBranch.class, "trunk");
        assertFalse(docDir.exists());

        // and switch back to the user branch
        savana(SetBranch.class, "user A");
        assertTrue(docDir.exists());

        // promote the changes
        savana(Promote.class, "-m", "trunk");

        // now create a sub branch with a space in the path
        savana(CreateUserBranch.class, "user B", "My Documents" + File.separatorChar + "Really Important!");
        FileUtils.writeStringToFile(docFile, "world peace would be good\n");
        SVN.getCommitClient().doCommit(new File[]{docDir}, false, "user branch commit", null, null, false, false, SVNDepth.INFINITY);

        // switch back to the trunk
        cd(docDir);
        savana(SetBranch.class, "trunk");
        assertEquals("maybe later\n", FileUtils.readFileToString(docFile));

        // and switch back to the user branch
        cd(WC1);
        savana(SetBranch.class, "user B");
        assertEquals("world peace would be good\n", FileUtils.readFileToString(docFile));
    }
}
