package org.codehaus.savana.scripts;

import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for working with the Subversion command-line programs (not SVNKit).
 */
public class TestSvnUtil {
    private static final Logger _sLog = Logger.getLogger(TestSvnUtil.class.getName());

    public static final int WC_FORMAT;       // SVNAdminAreaFactory.WC_FORMAT_*

    public static final boolean REPO_PRE14;  // svn 1.3 or older (see SVNAdminClient.doCreateRepository pre14Compatible argument)
    public static final boolean REPO_PRE15;  // svn 1.4 or older (see SVNAdminClient.doCreateRepository pre15Compatible argument)
    public static final boolean REPO_PRE16;  // svn 1.5 or older (see SVNAdminClient.doCreateRepository pre16Compatible argument)

    static {
        // detect the version of the 'svn' command-line utility so the tests can create
        // working copy files that are compatible with 'svn'.
        String clientVersionString;
        try {
            clientVersionString = TestProcessUtil.exec("svn", "--version", "--quiet").trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        String[] clientFields = StringUtils.split(clientVersionString, ".");
        int clientMajor = Integer.parseInt(clientFields[0]);
        int clientMinor = Integer.parseInt(clientFields[1]);

        if (clientMajor == 1 && clientMinor == 3) {
            _sLog.info("Using svn 1.3-compatible working copy file format");
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_13;

        } else if (clientMajor == 1 && clientMinor == 4) {
            _sLog.info("Using svn 1.4-compatible working copy file format");
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_14;

        } else if (clientMajor == 1 && clientMinor == 5) {
            _sLog.info("Using svn 1.5-compatible working copy file format");
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_15;

        } else {
            _sLog.info("Using svn 1.6-compatible working copy file format");
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_16;
        }

        // detect the version of the 'svnlook' command-line utility used by the subversion
        // server.  the back-end pre-commit hook uses svnlook, so the test repository file
        // format needs to be compatible with it.
        String serverVersionString;
        try {
            serverVersionString = TestProcessUtil.exec("svnlook", "--version").trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Matcher serverMatcher = Pattern.compile("version ([0-9]+)\\.([0-9]+)\\b").matcher(serverVersionString);
        if (!serverMatcher.find()) {
            throw new IllegalStateException("Unexpected svnlook version string: " + serverVersionString);
        }
        int serverMajor = Integer.parseInt(serverMatcher.group(1));
        int serverMinor = Integer.parseInt(serverMatcher.group(2));

        if (serverMajor == 1 && serverMinor == 3) {
            _sLog.info("Using svn 1.3-compatible repository format");
            REPO_PRE14 = true;
            REPO_PRE15 = true;
            REPO_PRE16 = true;

        } else if (serverMajor == 1 && serverMinor == 4) {
            _sLog.info("Using svn 1.4-compatible repository format");
            REPO_PRE14 = false;
            REPO_PRE15 = true;
            REPO_PRE16 = true;

        } else if (serverMajor == 1 && serverMinor == 5) {
            _sLog.info("Using svn 1.5-compatible repository format");
            REPO_PRE14 = false;
            REPO_PRE15 = false;
            REPO_PRE16 = true;

        } else {
            _sLog.info("Using svn 1.6-compatible repository format");
            REPO_PRE14 = false;
            REPO_PRE15 = false;
            REPO_PRE16 = false;
        }
    }
}
