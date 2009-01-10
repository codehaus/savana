package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.codehaus.savana.BranchType;
import org.codehaus.savana.scripts.admin.CreateMetadataFile;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.admin.ISVNAdminAreaFactorySelector;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

public abstract class TestRepoUtil {
    private static final Logger _sLog = Logger.getLogger(TestRepoUtil.class.getName());

    public static File SUBVERSION_CONFIG_DIR = TestDirUtil.createTempDir("subversion-config");

    public static final SVNClientManager SVN = SVNClientManager.newInstance(
            new DefaultSVNOptions(SUBVERSION_CONFIG_DIR, true), "savana-user", "");

    /**
     * Create a repository that most test cases can share.
     */
    public static final SVNURL DEFAULT_REPO = newRepositoryNoCheckedExceptions(true);

    /**
     * Create a new test subversion repository.
     */
    public static SVNURL newRepository(boolean installHooks) throws SVNException, IOException {
        _sLog.info("creating test repository");

        FSRepositoryFactory.setup();

        // configure SVNKit to use file formats that match the installed version of subversion   
        SVNAdminAreaFactory.setSelector(new ISVNAdminAreaFactorySelector() {
            public Collection getEnabledFactories(File path, Collection factories, boolean writeAccess) {
                Collection<SVNAdminAreaFactory> enabledFactories = new TreeSet<SVNAdminAreaFactory>();
                for (SVNAdminAreaFactory factory : (Collection<SVNAdminAreaFactory>) factories) {
                    if (factory.getSupportedVersion() == TestSvnUtil.WC_FORMAT) {
                        enabledFactories.add(factory);
                    }
                }
                return enabledFactories;
            }
        });

        // create the repository
        File repoDir = TestDirUtil.createTempDir(nextRepositoryName());
        SVNAdminClient adminClient = SVN.getAdminClient();
        SVNURL repoUrl = adminClient.doCreateRepository(repoDir, null, false, true,
                TestSvnUtil.REPO_PRE14, TestSvnUtil.REPO_PRE15);

        // install savana preferred subversion hooks into the test repository
        if (installHooks) {
            for (File svnHookFile : TestDirUtil.SVN_HOOKS_DIR.listFiles()) {
                if (!svnHookFile.isHidden() && !svnHookFile.getName().endsWith(".properties")) {
                    File repoHookFile = new File(new File(repoDir, "hooks"), svnHookFile.getName());
                    FileUtils.copyFile(svnHookFile, repoHookFile, false);
                    repoHookFile.setExecutable(true);
                }
            }
        }

        return repoUrl;
    }

    private static SVNURL newRepositoryNoCheckedExceptions(boolean installHooks) {
        try {
            return newRepository(installHooks);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Creates a project in the repo and an associated working directory, for a project that uses the standard branch paths. */
    public static File setupProjectWithWC(SVNURL repoUrl, String projectRoot,
                                          boolean configureSavana, boolean installPolicies,
                                          String resourceToImport)
            throws Exception {
        _sLog.info("creating new project " + projectRoot);

        SVNURL projectUrl = repoUrl.appendPath(projectRoot, false);

        // delete any existing project at the new project URL
        SVNRepository repository = SVN.createRepository(repoUrl, true);
        repository.setLocation(repoUrl, false);
        if (repository.checkPath(projectRoot, -1) != SVNNodeKind.NONE) {
            SVN.getCommitClient().doDelete(new SVNURL[]{projectUrl}, "branch admin - delete old project", null);
        }

        // setup initial branching structure
        SVN.getCommitClient().doMkDir(new SVNURL[] {
                projectUrl.appendPath(BranchType.TRUNK.getDefaultPath(), false),
                projectUrl.appendPath(BranchType.RELEASE_BRANCH.getDefaultPath(), false),
                projectUrl.appendPath(BranchType.USER_BRANCH.getDefaultPath(), false),
        }, "branch admin - setup initial branch directories", null, true);

        // import initial project files from a directory in the classpath
        if (resourceToImport != null) {
            File importDir = new File(TestRepoUtil.class.getClassLoader().getResource(resourceToImport).toURI());
            SVNURL trunkUrl = projectUrl.appendPath(BranchType.TRUNK.getDefaultPath(), false);
            SVN.getCommitClient().doImport(importDir, trunkUrl, "trunk - initial import", null, true, false, SVNDepth.INFINITY);
        }

        // check out the project, start in the trunk
        File wc = createTrunkWC(repoUrl, projectRoot);

        // create the .savana metadata file
        if (configureSavana) {
            if (installPolicies) {
                TestSavanaUtil.savana(CreateMetadataFile.class, projectRoot, "TRUNK",
                        "--savanaPoliciesFile", TestDirUtil.POLICIES_FILE.getAbsolutePath());
            } else {
                TestSavanaUtil.savana(CreateMetadataFile.class, projectRoot, "TRUNK");

            }
            SVN.getCommitClient().doCommit(new File[] {wc}, false,
                    "trunk - initial setup of savana", null, null, false, false, SVNDepth.INFINITY);
        }

        return wc;
    }

    /** Create a directory containing the trunk of the specified project, for a project that uses the standard branch paths. */
    public static File createTrunkWC(SVNURL repoUrl, String projectRoot) throws SVNException {
        SVNURL projectUrl = repoUrl.appendPath(projectRoot, false);

        // check out the project, start in the trunk
        File wc = TestDirUtil.createTempDir(nextWorkingDirName(projectRoot));
        SVNURL trunkUrl = projectUrl.appendPath(BranchType.TRUNK.getDefaultPath(), false);
        SVN.getUpdateClient().doCheckout(trunkUrl, wc, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);

        // cd into the wc dir
        TestDirUtil.cd(wc);

        return wc;
    }

    private static int _sRepositoryCounter;
    private static final Map<String, MutableInt> _sWorkingDirCounters = new HashMap<String, MutableInt>();

    private static synchronized String nextRepositoryName() {
        return "savana-test-repo-" + toAlphaString(_sRepositoryCounter++);
    }

    private static synchronized String nextWorkingDirName(String projectRoot) {
        String projectName = projectRoot.replace('/', '-');
        MutableInt counter = _sWorkingDirCounters.get(projectName);
        if (counter == null) {
            _sWorkingDirCounters.put(projectName, counter = new MutableInt());
        }
        counter.increment();
        return "savana-test-wc-" + projectName + "-" + counter.intValue();
    }

    /** Returns a String from the sequence 'A', 'B', ..., 'Z', 'AA', ... 'ZZ', 'BA' ... */
    private static String toAlphaString(int value) {
        StringBuilder buf = new StringBuilder();
        do {
            buf.append((char)('A' + (value % 26)));
            value /= 26;
        } while (value != 0);
        buf.reverse();
        return buf.toString();
    }


    public static File touchCounterFile(File dir) throws IOException {
        File counterFile = new File(dir, "counter.txt");
        int value = Integer.parseInt(FileUtils.readFileToString(counterFile).trim());
        FileUtils.writeStringToFile(counterFile, Integer.toString(value + 1));
        return counterFile;
    }
}
