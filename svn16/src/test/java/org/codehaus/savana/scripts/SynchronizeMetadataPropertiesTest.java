package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.BranchType;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNPropertyData;

import java.io.File;

/**
 * Validates that a changes in trunk .savana file are correctly merged or not merged
 * into a user branch during a Synchronize operation. 
 */
public class SynchronizeMetadataPropertiesTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    /**
     * Test verifies that synchronizing a replaced file doesn't work.  If Subversion/SVNKit ever gets
     * fixed so that synchronizing a replaced file does work, the Promote command can be updated to
     * remove the special check for replaced files.
     */
    public void testSynchronizeWarnsSkipped() throws Exception {
        // setup a test project with a working directory and without the default Savana policies property
        String projectName = getClass().getSimpleName().toLowerCase() + "-sync";
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, false, null);
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);

        // in WC1, create a user branch
        cd(WC1);
        savana(CreateUserBranch.class, "user1");

        // in WC2 (trunk), add the Savana policies
        cd(WC2);
        byte[] policiesData = FileUtils.readFileToByteArray(TestDirUtil.POLICIES_FILE);
        doSetProperty(WC2, MetadataFile.PROP_SAVANA_POLICIES, SVNPropertyValue.create(MetadataFile.PROP_SAVANA_POLICIES, policiesData));
        // force conflicts on all the properties that are set by CreateBranch to make sure they're resolved correctly.
        // this trashes the trunk .savana metadata file, but that's OK for the purposes of this test.  we do this because
        // svn 1.5 and SVNKit 1.2.2 incorrectly find conflicts on these properties w/a DAV repository, but not with a SVN
        // or FILE repository.  since this test uses a FILE repository, we have to force the conflicts like this.
        doSetProperty(WC2, MetadataFile.PROP_PROJECT_NAME, SVNPropertyValue.create("new project name"));
        doSetProperty(WC2, MetadataFile.PROP_BRANCH_PATH, SVNPropertyValue.create("new branch path"));
        doSetProperty(WC2, MetadataFile.PROP_BRANCH_TYPE, SVNPropertyValue.create(BranchType.RELEASE_BRANCH.getKeyword()));
        doSetProperty(WC2, MetadataFile.PROP_SOURCE_ROOT, SVNPropertyValue.create("new source root"));
        doSetProperty(WC2, MetadataFile.PROP_SOURCE_ROOT_BACKWARD_COMPATIBLE, SVNPropertyValue.create("new source root (old)"));
        doSetProperty(WC2, MetadataFile.PROP_SOURCE_SUBPATH, SVNPropertyValue.create("new source subpath"));
        doSetProperty(WC2, MetadataFile.PROP_BRANCH_POINT_REVISION, SVNPropertyValue.create("-123"));
        doSetProperty(WC2, MetadataFile.PROP_LAST_MERGE_REVISION, SVNPropertyValue.create("-456"));
        long changeset = SVN.getCommitClient().doCommit(new File[] {WC2}, false,
                "branch admin - added Savana policies", null, null, false, false, SVNDepth.INFINITY).getNewRevision();

        // in WC1 (user branch), sync
        cd(WC1);
        assertEquals(
                "--- Merging r" + (changeset - 1) + " through r" + changeset + " into '.':\n" +
                " U   .savana",
                savana(Synchronize.class, "--non-interactive"));

        // verify that the metadata file is not marked conflicted
        SVNStatus status = SVN.getStatusClient().doStatus(new File(WC1, MetadataFile.METADATA_FILE_NAME), false);
        assertEquals(SVNStatusType.STATUS_MODIFIED, status.getPropertiesStatus());

        // verify that the savana policies changes from trunk were applied to the user branch
        SVNPropertyData policiesProperty = SVN.getWCClient().doGetProperty(
                new File(WC1, MetadataFile.METADATA_FILE_NAME), MetadataFile.PROP_SAVANA_POLICIES, SVNRevision.WORKING, SVNRevision.WORKING);
        assertEquals(new String(policiesData, "UTF-8"), SVNPropertyValue.getPropertyAsString(policiesProperty.getValue()));

        // now revert the metadata file, make a change in the user branch, and verify that a conflict is discovered
        SVN.getWCClient().doRevert(new File[]{WC1}, SVNDepth.INFINITY, null);
        doSetProperty(WC1, MetadataFile.PROP_SAVANA_POLICIES, SVNPropertyValue.create("# generate conflict"));
        assertEquals(
                "--- Merging r" + (changeset - 1) + " through r" + changeset + " into '.':\n" +
                " C   .savana\n" +
                "Summary of conflicts:\n" +
                "  Property conflicts: 1",
                savana(Synchronize.class, "--non-interactive"));

        // verify that the metadata file *is* marked conflicted
        status = SVN.getStatusClient().doStatus(new File(WC1, MetadataFile.METADATA_FILE_NAME), false);
        assertEquals(SVNStatusType.STATUS_CONFLICTED, status.getPropertiesStatus());
    }

    private void doSetProperty(File wcDir, String propName, SVNPropertyValue propValue) throws SVNException {
        SVN.getWCClient().doSetProperty(new File(wcDir, MetadataFile.METADATA_FILE_NAME), propName, propValue, false, SVNDepth.EMPTY, null, null);
    }
}
