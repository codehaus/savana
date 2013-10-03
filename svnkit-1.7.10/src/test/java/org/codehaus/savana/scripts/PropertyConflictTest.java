package org.codehaus.savana.scripts;

import junit.framework.TestCase;
import org.codehaus.savana.BranchType;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNMerger;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNDiffConflictChoiceStyle;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.ISVNConflictHandler;
import org.tmatesoft.svn.core.wc.ISVNMerger;
import org.tmatesoft.svn.core.wc.ISVNMergerFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictResult;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.io.File;

/**
 * Verify that SVNKit issue http://svnkit.com/tracker/view.php?id=295 is fixed.
 * <p>
 * This test doesn't use Savana because it's validating a SVNKit bug.  But it
 * sets up a situation similar to what can happen using "sav synchronize".
 */
public class PropertyConflictTest extends TestCase {

    private SVNClientManager SVN = TestRepoUtil.SVN;

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testPropertyConflicts() throws Exception {
        //Setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, false, false, "test-project");

        //Create a user branch in WC1
        TestDirUtil.cd(WC1);

        //Set a property on a file in the top-level directory
        File file = new File(WC1, "counter.txt");
        SVN.getWCClient().doSetProperty(file, "TEST_PROP", SVNPropertyValue.create("String1"), false, SVNDepth.EMPTY, null, null);
        SVN.getCommitClient().doCommit(new File[]{file}, false, "trunk - prop1", null, null, false, false, SVNDepth.EMPTY);

        //Branch the trunk
        SVNURL projectUrl = REPO_URL.appendPath(projectName, false);
        SVNURL trunkUrl = projectUrl.appendPath(BranchType.TRUNK.getDefaultPath(), false);
        SVNURL branchUrl = projectUrl.appendPath(BranchType.USER_BRANCH.getDefaultPath(), false).appendPath("test", false);
        SVN.getCopyClient().doCopy(new SVNCopySource[] {
                new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, trunkUrl),
        }, branchUrl, false, true, false, "user branch - create", null);

        //Modify the property in trunk
        SVN.getWCClient().doSetProperty(file, "TEST_PROP", SVNPropertyValue.create("String22"), false, SVNDepth.EMPTY, null, null);
        long changeset = SVN.getCommitClient().doCommit(new File[]{file}, false, "trunk - prop2", null, null, false, false, SVNDepth.EMPTY).getNewRevision();

        //Switch to the branch
        SVN.getUpdateClient().doSwitch(WC1, branchUrl, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);

        //Modify the property in the branch
        SVN.getWCClient().doSetProperty(file, "TEST_PROP", SVNPropertyValue.create("String333"), false, SVNDepth.EMPTY, null, null);
        SVN.getCommitClient().doCommit(new File[]{file}, false, "user branch - prop3", null, null, false, false, SVNDepth.EMPTY);

        //Merge the trunk change into the branch.  This will throw a NullPointerException if SVNKit issue #295 isn't fixed.
        SVN.getUpdateClient().doUpdate(WC1, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        SVNDiffClient diffClient = SVN.getDiffClient();
        diffClient.setDiffGenerator(new DefaultSVNDiffGenerator());
        DefaultSVNOptions svnOptions = new DefaultSVNOptions(TestRepoUtil.SUBVERSION_CONFIG_DIR, true);
        svnOptions.setMergerFactory(new AlwaysPostponeMergerFactory());
        diffClient.setOptions(svnOptions);
        diffClient.doMerge(
                trunkUrl, SVNRevision.create(changeset - 1),
                trunkUrl, SVNRevision.create(changeset),
                WC1, SVNDepth.INFINITY, true, false, false, false);

        //Check that the properties were identified as conflicted
        SVNStatus status = SVN.getStatusClient().doStatus(file, false);
        assertEquals(SVNStatusType.STATUS_CONFLICTED, status.getPropertiesStatus());
    }

    private static class AlwaysPostponeMergerFactory implements ISVNMergerFactory {
        public ISVNMerger createMerger(byte[] conflictStart, byte[] conflictSeparator, byte[] conflictEnd) {
            return new DefaultSVNMerger(conflictStart, conflictSeparator, conflictEnd, new ISVNConflictHandler() {
                public SVNConflictResult handleConflict(SVNConflictDescription conflictDescription) throws SVNException {
                    return new SVNConflictResult(SVNConflictChoice.POSTPONE, null);
                }
            }, SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST);
        }
    }
}
