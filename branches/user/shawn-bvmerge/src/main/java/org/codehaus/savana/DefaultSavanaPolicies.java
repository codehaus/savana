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
import java.util.regex.Pattern;

public class DefaultSavanaPolicies implements ISavanaPolicies {

    private static final String DEFAULT_LOG_MESSAGE_ERROR =
            "Commit message does not match the required message pattern: $pattern" +
                    "\n  workspace: $branchName\n  commit comment: $logMessage";

    private Properties _properties;

    public void initialize(Properties properties) throws SVNException {
        _properties = properties;
    }

    public void validateLogMessage(String commitMessage, MetadataProperties metadataProperties) throws SVNException {
        String branchTypeName = metadataProperties.getBranchType().name().toLowerCase();
        String pattern = _properties.getProperty(branchTypeName + ".pattern");
        if (pattern == null) {
            return;  // nothing to validate
        }

        // expand variables in the pattern template and apply the resulting regular expression
        pattern = replace(pattern, "branchType", metadataProperties.getBranchType().getKeyword().toLowerCase());
        pattern = replace(pattern, "branchName", metadataProperties.getBranchName());

        if (!Pattern.matches(pattern, commitMessage)) {
            // get the error message template and expand variables
            String error = _properties.getProperty(branchTypeName + "error", DEFAULT_LOG_MESSAGE_ERROR);
            error = replace(error, "pattern", pattern);
            error = replace(error, "branchType", metadataProperties.getBranchType().getKeyword().toLowerCase());
            error = replace(error, "branchName", metadataProperties.getBranchName());
            error = replace(error, "logMessage", commitMessage);
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_BAD_LOG_MESSAGE, error), SVNLogType.CLIENT);
        }
    }

    private String replace(String template, String key, String value) {
        return template.replaceAll("$\\Q" + key + "\\E\\b", value);
    }
}
