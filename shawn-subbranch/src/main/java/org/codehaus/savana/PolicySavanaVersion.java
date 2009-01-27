package org.codehaus.savana;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.scripts.SAVCommand;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.util.Properties;

/**
 * Validates that Savana is new enough.  Repo administrators can set a policy
 * property that all users must use at least version x.y.z to force users to
 * upgrade.  This is especially useful when bugs are discovered and fixed in
 * Savana and users must upgrade to avoid potentially messing up files in the
 * repository due to the bug. 
 */
public class PolicySavanaVersion {

    public static final String VERSION_KEY = "minimum_savana_version";

    private final Properties _properties;

    public PolicySavanaVersion(Properties properties) throws SVNException {
        _properties = properties;
    }

    public void validateSavanaVersion() throws SVNException {
        // get the minimum required version of savana
        String versionString = _properties.getProperty(VERSION_KEY);
        if (versionString == null) {
            return;  // nothing to validate, anything goes
        }
        long[] requiredVersionNumbers = parseRequiredVersionNumbers(versionString);

        // get the actual version of Savana.  this should be set by the maven build
        long[] actualVersionNumbers = new long[] {
                Version.VERSION_MAJOR,
                Version.VERSION_MINOR,
                Version.VERSION_PATCH,
                Version.VERSION_REVISION,
        };

        // if Savana wasn't built via maven (maybe via an IDE etc.) log a warning
        if (compare(new long[]{0, 0, 0, 0}, actualVersionNumbers) == 0) {
            SAVCommand._sLog.warning("Unable to verify that Savana is version " + versionString +
                                     " or newer because the Savana version is unknown.");
            return;
        }

        // compare the major/minor/revision version number
        if (compare(requiredVersionNumbers, actualVersionNumbers) > 0) {
            String error = String.format(
                    "ERROR: Savana is version %s but the project requires a\n" +
                    "minimum version of %s.  Please upgrade Savana.",
                    Version.VERSION_SHORT, formatVersionString(requiredVersionNumbers));
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, error), SVNLogType.CLIENT);
        }
    }

    private long[] parseRequiredVersionNumbers(String versionString) throws SVNException {
        // the version number should be "<major>.<minor>.<revision>" such as "1.2.4567"
        // it doesn't need all three fields--just "<major>" or "<major>.<minor>" is ok too.
        String[] versionFields = StringUtils.split(versionString, ".");
        long[] versionNumbers = new long[versionFields.length];
        try {
            for (int i = 0; i < versionFields.length; i++) {
                versionNumbers[i] = Long.parseLong(versionFields[i]);
            }
        } catch (Exception e) {
            String error = "Error parsing Savana policy property '" + VERSION_KEY + "': " + versionString;
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.BAD_PROPERTY_VALUE, error), SVNLogType.CLIENT);
        }
        return versionNumbers;
    }

    private String formatVersionString(long[] versionNumbers) {
        return StringUtils.join(ArrayUtils.toObject(versionNumbers), '.');
    }

    private int compare(long[] expected, long[] actual) {
        // note: we don't care if expected.length < actual.length
        for (int i = 0; i < expected.length; i++) {
            int result = Long.signum(expected[i] - actual[i]);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
