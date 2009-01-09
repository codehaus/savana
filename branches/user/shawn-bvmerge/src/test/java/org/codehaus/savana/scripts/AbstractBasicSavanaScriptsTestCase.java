package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.codehaus.savana.BranchType;
import org.codehaus.savana.scripts.admin.CreateMetadataFile;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Logger;

/**
 * Extends AbstractSavanaScriptsTestCase to setup a subversion repository with
 * default trunk, release, user branches and a default Savana configuration.  
 */
public abstract class AbstractBasicSavanaScriptsTestCase extends AbstractSavanaScriptsTestCase {
    private static final Logger log = Logger.getLogger(AbstractBasicSavanaScriptsTestCase.class.getName());

    protected static final File SVN_HOOKS_DIR = new File(System.getProperty("savana.svn-hooks")).getAbsoluteFile();
    protected static final File POLICIES_FILE = new File(SVN_HOOKS_DIR, "savana-policies.properties");

    protected static final String TEST_PROJECT_NAME = "test-project";

    protected File REPO_DIR;
    protected File WC1, WC2;
    protected SVNURL TRUNK_URL;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        REPO_DIR = createTempDir("savana-test-repo");
        WC1 = createTempDir("savana-test-wc1");
        WC2 = createTempDir("savana-test-wc2");

        // create the test repository, and set up the URLs to the repo, the project, and the project's trunk
        log.info("creating test repository");
        SVNAdminClient adminClient = SVN.getAdminClient();
        SVNURL repoUrl = adminClient.doCreateRepository(REPO_DIR, null, false, true);
        SVNURL projectUrl = repoUrl.appendPath(TEST_PROJECT_NAME, true);
        TRUNK_URL = projectUrl.appendPath("trunk", true);

        // install savana preferred subversion hooks into the test repository
        for (File svnHookFile : SVN_HOOKS_DIR.listFiles((FileFilter) HiddenFileFilter.VISIBLE)) {
            File repoHookFile = new File(new File(REPO_DIR, "hooks"), svnHookFile.getName());
            FileUtils.copyFile(svnHookFile, repoHookFile, false);
            repoHookFile.setExecutable(true);
        }

        // setup initial branching structure
        log.info("creating initial branch directories");
        SVN.getCommitClient().doMkDir(new SVNURL[] {
                projectUrl.appendPath(BranchType.TRUNK.getDefaultPath(), false),
                projectUrl.appendPath(BranchType.RELEASE_BRANCH.getDefaultPath(), false),
                projectUrl.appendPath(BranchType.USER_BRANCH.getDefaultPath(), false),
        }, "branch admin - setup initial branch directories", null, true);

        // get the directory from the classpath that contains the project to import, and import the project
        // into the repository
        File importDir = new File(
                BasicWorkspaceSessionTest.class.getClassLoader().getResource(TEST_PROJECT_NAME).toURI());
        log.info("importing project");
        SVN.getCommitClient().doImport(importDir, TRUNK_URL, "trunk - initial import", null, true, false, SVNDepth.INFINITY);

        // check out the two repositories
        log.info("checking out repo into two working copies");
        SVN.getUpdateClient().doCheckout(TRUNK_URL, WC1, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        SVN.getUpdateClient().doCheckout(TRUNK_URL, WC2, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);

        // cd into the wc dir
        cd(WC1);

        // create the .savana metadata file with the default policies file
        savana(CreateMetadataFile.class, TEST_PROJECT_NAME, "TRUNK", "--savanaPoliciesFile", POLICIES_FILE.getAbsolutePath());
        SVN.getCommitClient().doCommit(
                new File[]{WC1}, false, "trunk - initial setup of savana", null, null, false, false, SVNDepth.INFINITY);
    }
}
