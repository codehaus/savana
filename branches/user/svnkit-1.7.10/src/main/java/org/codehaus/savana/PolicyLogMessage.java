package org.codehaus.savana;

import org.codehaus.savana.scripts.SAVCommand;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Validates promote log messages against a regular expression.  This is designed to fail
 * promotes early in the same way an installed commit hook will, but before Promote has
 * started making changes to the local working copy.
 */
public class PolicyLogMessage {
    private static final String DEFAULT_LOG_MESSAGE_ERROR =
            "Commit message does not match the required message pattern: @pattern" +
                    "\n  workspace: @branchName\n  commit comment: @logMessage";

    private static final String DEFAULT_CODE_FREEZE_ERROR =
            "The @projectName @branchName workspace is currently under code freeze. " +
                    "Please try again when code freeze is lifted.";

    private static final String DEFAULT_CODE_FREEZE_BREAK_PATTERN =
            ".*?code freeze break$";

    private final Properties _properties;

    public PolicyLogMessage(Properties properties) throws SVNException {
        _properties = properties;
    }

    public void validateLogMessage(String logMessage, MetadataProperties metadataProperties, boolean codeFrozen) throws SVNException {
        // each branch type has its own regular expression that messages must match
        String prefix = "logmessage." + metadataProperties.getBranchType().name().toLowerCase() + ".";

        // get the regular expression to match against the log message (if any) and expand variables
        String pattern = _properties.getProperty(prefix + "pattern");
        if (pattern == null) {
            return;  // nothing to validate, anything goes
        }
        pattern = replaceBranchKeywords(metadataProperties, pattern);

        SAVCommand._sLog.fine("Validating log message against pattern: " + pattern);

        // normalize end-of-line characters etc.
        logMessage = SVNCommitUtil.validateCommitMessage(logMessage);

        // check the log message against the required pattern
        if (!Pattern.compile(pattern, Pattern.DOTALL).matcher(logMessage).matches()) {
            // get the error message template and expand variables
            String error = _properties.getProperty(prefix + "error", DEFAULT_LOG_MESSAGE_ERROR);
            error = replaceBranchKeywords(metadataProperties, error);
            error = replace(error, "@pattern", pattern);
            error = replace(error, "@logMessage", logMessage);
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_BAD_LOG_MESSAGE, error), SVNLogType.CLIENT);
        }

        if (!codeFrozen) {
            return;
        }

        // check the log message against the code freeze break pattern
        pattern = _properties.getProperty(prefix + "codefreezebreakpattern", DEFAULT_CODE_FREEZE_BREAK_PATTERN);
        if (!Pattern.compile(pattern, Pattern.DOTALL).matcher(logMessage).matches()) {
            // get the error message template and expand variables
            String error = _properties.getProperty(prefix + "codefreezeerror", DEFAULT_CODE_FREEZE_ERROR);
            error = replaceBranchKeywords(metadataProperties, error);
            error = replace(error, "@pattern", pattern);
            error = replace(error, "@logMessage", logMessage);
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CANCELLED, error), SVNLogType.CLIENT);
        }
    }

    private String replaceBranchKeywords(MetadataProperties metadataProperties, String template) throws SVNException {
        template = replace(template, "@projectName", metadataProperties.getProjectName());
        template = replace(template, "@branchName", metadataProperties.getBranchName());
        template = replace(template, "@branchType", metadataProperties.getBranchType());
        template = replace(template, "@sourceBranchName", metadataProperties.getSourceName());
        template = replace(template, "@sourceBranchType", metadataProperties.getSourceBranchType());
        return template;
    }

    private String replace(String template, String key, BranchType branchType) {
        if (branchType != null) {
            template = replace(template, key, branchType.getKeyword().toLowerCase());
        }
        return template;
    }

    private String replace(String template, String key, String value) {
        if (value != null) {
            template = template.replaceAll("\\Q" + key + "\\E\\b", value); // important part is \\b to enforce ends on a word boundary
        }
        return template;
    }

}
