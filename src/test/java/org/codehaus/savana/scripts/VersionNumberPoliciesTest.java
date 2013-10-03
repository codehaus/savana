package org.codehaus.savana.scripts;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.PolicySavanaVersion;
import org.codehaus.savana.Version;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class VersionNumberPoliciesTest extends AbstractSavanaScriptsTestCase {

    private SVNURL REPO_URL = TestRepoUtil.DEFAULT_REPO;

    public void testVersionNumberPolicies() throws Exception {
        // setup a test project with a working directory
        String projectName = getClass().getSimpleName().toLowerCase() + "-versions";
        File WC = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, null);

        long major = Version.VERSION_MAJOR;
        long minor = Version.VERSION_MINOR;
        long patch = Version.VERSION_PATCH;
        long revision = Version.VERSION_REVISION;

        // default required minimum should work
        savana(ListWorkingCopyInfo.class);

        // test version numbers that should work
        assertMinimumRequiredVersionSucceeds(WC);
        assertMinimumRequiredVersionSucceeds(WC, 1, 0, 0);
        assertMinimumRequiredVersionSucceeds(WC, 0, 9, 8, 7, 6);
        assertMinimumRequiredVersionSucceeds(WC, major, minor, patch, revision - 1);
        assertMinimumRequiredVersionSucceeds(WC, major, minor, patch, revision);
        assertMinimumRequiredVersionSucceeds(WC, major, minor, patch - 1);
        assertMinimumRequiredVersionSucceeds(WC, major, minor, patch);
        assertMinimumRequiredVersionSucceeds(WC, major, minor);
        assertMinimumRequiredVersionSucceeds(WC, major, minor - 1);
        assertMinimumRequiredVersionSucceeds(WC, major);
        assertMinimumRequiredVersionSucceeds(WC, major - 1);

        // test version numbers that should fail
        assertMinimumRequiredVersionFails(WC, 99);
        assertMinimumRequiredVersionFails(WC, major + 1, 0, 0);
        assertMinimumRequiredVersionFails(WC, major, minor + 1, 0);
        assertMinimumRequiredVersionFails(WC, major, minor, patch + 1);
        assertMinimumRequiredVersionFails(WC, major, minor, patch, revision + 1);
    }

    public void testBranchOpsCheckVersionNumber() throws Exception {
        // setup a test project with a working directory
        String projectName = getClass().getSimpleName().toLowerCase() + "-operations";
        File WC1 = TestRepoUtil.setupProjectWithWC(REPO_URL, projectName, true, true, "test-project");
        File WC2 = TestRepoUtil.createTrunkWC(REPO_URL, projectName);

        // WC1: create a user branch
        cd(WC1);
        savana(CreateUserBranch.class, "user1");

        // WC2 (trunk): set the minimum required version to the next version
        setMinimumRequiredVersionPolicy(WC2, 9, 8, 7, 6543);
        SVN.getCommitClient().doCommit(new File[]{WC2}, false, "branch admin - update minimum required version", null, null, false, false, SVNDepth.INFINITY);

        // WC1 (user branch): sav info should work since the working copy doesn't see the new policies
        savana(ListWorkingCopyInfo.class);

        // WC1 (user branch): sav synchronize should not work since trunk has a new minimum required version #
        try {
            savana(Synchronize.class);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: E204900: ERROR: Savana is version " + Version.VERSION_SHORT + " but the project requires a\n" +
                         "minimum version of 9.8.7.6543.  Please upgrade Savana.\n", e.getErr());
        }
    }

    private void assertMinimumRequiredVersionSucceeds(File dir, long... versionNumbers) throws Exception {
        setMinimumRequiredVersionPolicy(dir, versionNumbers);

        savana(ListWorkingCopyInfo.class);
    }

    private void assertMinimumRequiredVersionFails(File dir, long... versionNumbers) throws Exception {
        setMinimumRequiredVersionPolicy(dir, versionNumbers);

        try {
            savana(ListWorkingCopyInfo.class);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            String versionString = StringUtils.join(ArrayUtils.toObject(versionNumbers), '.');
            assertEquals("svn: E204900: ERROR: Savana is version " + Version.VERSION_SHORT + " but the project requires a\n" +
                         "minimum version of " + versionString + ".  Please upgrade Savana.\n", e.getErr());
        }
    }

    private void setMinimumRequiredVersionPolicy(File dir, long... versionNumbers) throws Exception {
        //Load the default policy properties
        Properties properties = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(TestDirUtil.POLICIES_FILE));
        try {
            properties.load(in);
        } finally {
            IOUtils.closeQuietly(in);
        }

        //Set the minimum required version in the Properties object
        if (versionNumbers.length == 0) {
            properties.remove(PolicySavanaVersion.VERSION_KEY);
        } else {
            String versionString = StringUtils.join(ArrayUtils.toObject(versionNumbers), '.');
            properties.put(PolicySavanaVersion.VERSION_KEY, versionString);
        }

        //Convert the Properties object to a String
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        properties.store(buf, null);   // jdk 1.5-friendly (don't use jdk 1.6 Reader version)
        String policies = new String(buf.toByteArray(), "ISO-8859-1");

        //Set the Savana policies as a Subversion property on the .savana file
        SVN.getWCClient().doSetProperty(new File(dir, MetadataFile.METADATA_FILE_NAME),
                MetadataFile.PROP_SAVANA_POLICIES, SVNPropertyValue.create(policies), false, SVNDepth.EMPTY, null, null);
    }

}
