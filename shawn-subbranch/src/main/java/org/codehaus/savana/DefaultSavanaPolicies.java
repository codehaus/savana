/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2008-2009  Bazaarvoice Inc.
 * <p/>
 * This file is part of Savana.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Third party components of this software are provided or made available only subject
 * to their respective licenses. The relevant components and corresponding
 * licenses are listed in the "licenses" directory in this distribution. In any event,
 * the disclaimer of warranty and limitation of liability provision in this Agreement
 * will apply to all Software in this distribution.
 *
 * @author Shawn Smith (shawn@bazaarvoice.com)
 */
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DefaultSavanaPolicies implements ISavanaPolicies {

    private static final String DEFAULT_LOG_MESSAGE_ERROR =
            "Commit message does not match the required message pattern: @pattern" +
                    "\n  workspace: @branchName\n  commit comment: @logMessage";

    private Properties _properties;

    public void initialize(Properties properties) throws SVNException {
        _properties = properties;
    }

    public void validateLogMessage(String logMessage, MetadataProperties metadataProperties, Logger logger) throws SVNException {
        // each branch type has its own regular expression that messages must match
        String prefix = "logmessage." + metadataProperties.getBranchType().name().toLowerCase() + ".";

        // get the regular expression to match against the log message (if any) and expand variables
        String pattern = _properties.getProperty(prefix + "pattern");
        if (pattern == null) {
            return;  // nothing to validate, anything goes
        }
        pattern = replaceBranchKeywords(metadataProperties, pattern);

        logger.fine("Validating log message against pattern: " + pattern);

        // check the log message against the required pattern
        if (!Pattern.matches(pattern, logMessage)) {
            // get the error message template and expand variables
            String error = _properties.getProperty(prefix + "error", DEFAULT_LOG_MESSAGE_ERROR);
            error = replaceBranchKeywords(metadataProperties, error);
            error = replace(error, "@pattern", pattern);
            error = replace(error, "@logMessage", logMessage);
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_BAD_LOG_MESSAGE, error), SVNLogType.CLIENT);
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
