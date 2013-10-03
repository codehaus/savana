/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006-2013  Bazaarvoice Inc.
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
package org.codehaus.savana.scripts;

import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.PathUtil;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeleteBranch extends SAVCommand {

    private final boolean _userBranch;

    public DeleteBranch(String name, String[] aliases, boolean userBranch) {
        super(name, aliases);
        _userBranch = userBranch;
    }

    @Override
    public boolean isCommitter() {
        return true;
    }

    @Override
    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options = SVNOption.addLogMessageOptions(options);
        return options;
    }

    public void doRun() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(null, false);
        if (targets.isEmpty()) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_INSUFFICIENT_ARGS), SVNLogType.CLIENT);
        }
        if (targets.size() > 1) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }
        String branchName = targets.get(0);

        //Get information about the current workspace from the metadata file
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager());
        MetadataProperties wcProps = wcInfo.getMetadataProperties();

        //Find the source of the branch
        String branchPath = _userBranch ?
                            wcProps.getUserBranchPath(branchName) :
                            wcProps.getReleaseBranchPath(branchName);

        //Make sure the branch exists
        logStart("Check if branch exists");
        SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);
        if (repository.checkPath(repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.DIR) {
            String errorMessage =
                    "ERROR: Could not find branch." +
                    "\nBranch Path: " + branchPath;
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check if branch exists");

        //Get the metadata properties for the branch
        logStart("Get metadata properties");
        String branchMetadataFilePath = SVNPathUtil.append(branchPath, wcProps.getMetadataFileName());
        MetadataProperties branchProps = new MetadataProperties(repository, branchMetadataFilePath, -1);
        logEnd("Get metadata properties");

        //Check if the user is switched to the branch we're about to delete.  If they are, don't
        //delete the branch since 'svn up' with a switched subdirectory pointing at a deleted
        //branch can fail with unfriendly error messages.
        logStart("Check if working copy is set to branch");
        // find the likely location of the branch in the working copy
        File wcBranchRootDir = adjustPath(wcInfo.getRootDir(), wcProps.getSourceSubpath(), branchProps.getSourceSubpath());
        // find the path in the repository that the working copy directory is pointing to
        String wcBranchPath;
        try {
            SVNWCClient wcClient = env.getClientManager().getWCClient();
            SVNInfo wcBranchInfo = wcClient.doInfo(wcBranchRootDir, SVNRevision.WORKING);
            wcBranchPath = PathUtil.getPathTail(wcBranchInfo.getURL(), wcBranchInfo.getRepositoryRootURL());
        } catch (SVNException e) {
            //The wcBranchRootDir isn't a valid versioned directory so it's not part of the branch we will delete
            wcBranchPath = null;
        }
        // if the working copy is pointing to the branch to delete, fail.
        if (branchPath.equals(wcBranchPath)) {
            String errorMessage =
                    "ERROR: Use 'sav setbranch' to switch away from the branch before deleting it." +
                    "\nBranch Path: " + wcBranchRootDir;
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check if working copy is set to branch");

        //Perform delete
        logStart("Get Commit Client");
        SVNURL branchURL = wcInfo.getRepositoryURL(branchPath);
        SVNCommitClient commitClient = env.getClientManager().getCommitClient();
        logEnd("Get Commit Client");

        logStart("Perform Delete");
        String commitMessage = getCommitMessage(branchName);
        commitClient.doDelete(new SVNURL[]{branchURL}, commitMessage, env.getRevisionProperties());
        logEnd("Perform Delete");

        env.getOut().println("Deleted Branch: " + branchPath);
    }

    private String getCommitMessage(String branchName) throws SVNException {
        // get the commit message from the command line.  if it's not specified, default
        // to a reasonable value instead of starting an interactive editor.
        String message = getSVNEnvironment().getCommitMessage(null, null);
        if (StringUtils.isEmpty(message)) {
            message = branchName + " - deleting branch";
        }
        return message;
    }

    /** Start from 'dir', walk up the directory hierarchy along 'pathUp', then walk down along 'pathDown'. */
    private File adjustPath(File dir, String pathUp, String pathDown) throws SVNException {
        String commonPrefix = SVNPathUtil.getCommonPathAncestor(pathUp, pathDown);
        pathUp = PathUtil.getPathTail(pathUp, commonPrefix);
        pathDown = PathUtil.getPathTail(pathDown, commonPrefix);

        String dirPath = PathUtil.normalizePath(dir);
        if (!dirPath.endsWith(pathUp)) {
            return null;
        }
        return new File(StringUtils.removeEnd(dirPath, pathUp), pathDown);
    }
}
