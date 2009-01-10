package org.codehaus.savana.scripts;

import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;

import java.util.logging.Logger;

/**
 * Utilities for working with the Subversion command-line programs (not SVNKit).
 */
public class TestSvnUtil {
    private static final Logger _sLog = Logger.getLogger(TestRepoUtil.class.getName());

    public static final int WC_FORMAT;

    public static final boolean REPO_PRE14;
    public static final boolean REPO_PRE15;

    static {
        String versionString;
        try {
            versionString = TestProcessUtil.exec("svn", "--version", "--quiet").trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        String[] fields = StringUtils.split(versionString, ".");
        int major = Integer.parseInt(fields[0]);
        int minor = Integer.parseInt(fields[1]);

        if (major == 1 && minor == 3) {
            _sLog.info("Using svn 1.3-compatible repository and working copy file formats");
            REPO_PRE14 = true;
            REPO_PRE15 = true;
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_13;

        } else if (major == 1 && minor == 4) {
            _sLog.info("Using svn 1.4-compatible repository and working copy file formats");
            REPO_PRE14 = false;
            REPO_PRE15 = true;
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_14;

        } else {
            _sLog.info("Using svn 1.5-compatible repository and working copy file formats");
            REPO_PRE14 = false;
            REPO_PRE15 = false;
            WC_FORMAT = SVNAdminAreaFactory.WC_FORMAT_15;            
        }
    }
}
