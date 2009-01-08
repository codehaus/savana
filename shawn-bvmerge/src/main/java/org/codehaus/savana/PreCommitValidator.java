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
 * @author Brian Showers (brian@bazaarvoice.com)
 * @author Bryon Jacob (bryon@jacob.net)
 * @author Shawn Smith (shawn@bazaarvoice.com)
 */
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Implements the same checks on the commit message that are implemented by
 * the subversion "pre-commit" script on the back-end.  This is useful for
 * detecting problems with commit messages early on before a svn script has
 * done much work.
 */
public class PreCommitValidator {

    public void validate(String branchName, BranchType branchType, String commitMessage) throws SVNException {
        if (commitMessage.startsWith("branch admin")) {
            return;  // branch admin comment can do anything
        }

        String expectedWorkspaceName = commitMessage.split("\\s", 2)[0]; // first word (delimited by whitespace)
        boolean userBranch = (BranchType.USER_BRANCH == branchType);

        if (!branchName.equals(expectedWorkspaceName) &&
                !(userBranch && expectedWorkspaceName.startsWith("user branch"))) {
            if (userBranch) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_BAD_LOG_MESSAGE,
                        "The commit comment must start with \"user branch\" or the name of the modified workspace:\n" +
                                "  workspace: " + branchName + "\n" +
                                "  commit comment: " + commitMessage), SVNLogType.CLIENT);
            } else {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_BAD_LOG_MESSAGE,
                        "The commit comment must start with the name of the modified workspace:\n" +
                                "  workspace: " + branchName + "\n" +
                                "  commit comment: " + commitMessage), SVNLogType.CLIENT);
            }
        }
    }
}
