/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006-2009  Bazaarvoice Inc.
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
import org.codehaus.savana.BranchType;
import org.codehaus.savana.LocalChangeStatusHandler;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.PathUtil;
import org.codehaus.savana.SVNEditorHelper;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNNotifyPrinter;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CreateBranch extends SAVCommand {

    private final boolean _userBranch;
    private final boolean _allowSubbranches;

    public CreateBranch(String name, String[] aliases, boolean userBranch, boolean allowSubbranches) {
        super(name, aliases);
        _userBranch = userBranch;
        _allowSubbranches = allowSubbranches;
    }

    @Override
    public boolean isCommitter() {
        return true;
    }

    @Override
    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options.add(SVNOption.FORCE); // force the branch to be created even if 'svn status' reports changes
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
        if (targets.size() > (_allowSubbranches ? 2 : 1)) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }
        String branchName = targets.get(0);
        boolean subpathSpecified = targets.size() > 1;
        File startingDirectory = new File(subpathSpecified ? targets.get(1) : "").getAbsoluteFile();

        //Validate the branch name doesn't have illegal characters
        if (StringUtils.containsAny(branchName, "/\\")) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR,
                    "ERROR: Branch name argument may not contain slash characters: " + branchName), SVNLogType.CLIENT);
        }

        //Get information about the current workspace from the metadata file
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager(), startingDirectory);
        MetadataProperties wcProps = wcInfo.getMetadataProperties();

        //If creating a subbranch, root the new workspace at the specified argument.  Otherwise root it where .savana lives.
        File branchRootDir = subpathSpecified ? startingDirectory : wcInfo.getRootDir();

        //Validate newBranchRootDir exists in the working copy and isn't marked deleted, added, etc. since we'll 'svn switch' it later
        logStart("Validating working copy directory status");
        SVNStatusClient statusClient = env.getClientManager().getStatusClient();
        SVNStatus branchRootDirStatus = statusClient.doStatus(branchRootDir, false);
        if (branchRootDirStatus.getKind() != SVNNodeKind.DIR) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR,
                    "ERROR: New branch root must be a versioned directory.\nPath: " + branchRootDir), SVNLogType.CLIENT);
        }
        SVNStatusType branchRootDirStatusType = branchRootDirStatus.getContentsStatus();
        if (branchRootDirStatusType != SVNStatusType.STATUS_NORMAL && branchRootDirStatusType != SVNStatusType.STATUS_MODIFIED) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR,
                    "ERROR: New branch root directory may not have a status of " +
                    branchRootDirStatusType + ".\nPath" + branchRootDir), SVNLogType.CLIENT);
        }
        logEnd("Validating working copy directory status");

        //Don't allow the user to convert a working copy to a new branch if there are uncommitted changes, unless:
        //1. The force flag is enabled AND
        //2. The new branch will be a user branch
        if (!env.isForce() || !_userBranch) {
            logStart("Looking for local changes");
            LocalChangeStatusHandler statusHandler = new LocalChangeStatusHandler();
            statusClient.doStatus(branchRootDir, SVNRevision.UNDEFINED,
                    SVNDepth.INFINITY, false, true, false, false, statusHandler, null);
            logEnd("Looking for local changes");
            if (statusHandler.isChanged()) {
                //TODO: Just list the changes here rather than making the user run 'svn status'
                String errorMessage =
                        "ERROR: Cannot create a new branch while the working copy has local changes." +
                        "\nRun 'svn status' to find changes";
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
            }
        }

        //Get the project name from the working copy
        String projectName = wcProps.getProjectName();

        //Get the branch type for the new branch
        BranchType branchType = _userBranch ? BranchType.USER_BRANCH : BranchType.RELEASE_BRANCH;

        //Get the branch path for the new branch
        String branchPath = _userBranch ?
                wcProps.getUserBranchPath(branchName) :
                wcProps.getReleaseBranchPath(branchName);

        //Get the source path and subpath of the new branch based on the current working copy.
        //WC=TRUNK          ==> SOURCE=TRUNK (Branch path of working copy)
        //WC=RELEASE BRANCH ==> SOURCE=RELEASE BRANCH (Branch path of working copy)
        //WC=USER BRANCH    ==> SOURCE=SOURCE OF USER BRANCH (Source path of working copy)
        String sourcePath;
        String branchSubpath = PathUtil.getPathTail(branchRootDir.getPath(), wcInfo.getRootDir().getPath());
        if (wcProps.getBranchType() == BranchType.USER_BRANCH) {
            sourcePath = wcProps.getSourcePath();
            branchSubpath = SVNPathUtil.append(wcProps.getBranchSubpath(), branchSubpath);
        } else {
            sourcePath = wcProps.getBranchPath();
        }

        //The new branch is a copy starting at the source path plus the branch subpath.
        String sourcePathPlusSubpath = SVNPathUtil.append(sourcePath, branchSubpath);

        String sourceMetadataFilePath = SVNPathUtil.append(sourcePath, wcInfo.getMetadataFile().getName());

        //Make sure the branch doesn't exist
        logStart("Check that the branch doesn't exist");
        SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);
        if (repository.checkPath(repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.NONE) {
            String errorMessage =
                    "ERROR: Branch already exists." +
                    "\nBranch Path: " + branchPath;
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        //Deny creating user branch with same name as release branch.
        if (_userBranch && repository.checkPath(repository.getRepositoryPath(wcProps.getReleaseBranchPath(branchName)), -1) != SVNNodeKind.NONE) {
            String errorMessage = MessageFormat.format("ERROR: There is a release branch for the project= {0} with name {1}. Please choose another name for your user branch", projectName, branchName);
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check that the branch doesn't exist");

        //Figure out the paths to the parent dir of the branch and to the metadata file
        String branchParentPath = SVNPathUtil.removeTail(branchPath);
        String branchMetadataFilePath = SVNPathUtil.append(branchPath, wcInfo.getMetadataFile().getName());

        //Perform the copy
        try {
            long sourceRevision = repository.getLatestRevision();

            //Figure out which subpaths to the branch are missing
            List<String> missingPaths = getMissingSubpaths(repository, branchParentPath, sourceRevision);

            //Create an editor
            logStart("Get commit editor");
            String commitMessage = getCommitMessage(branchName);
            ISVNEditor editor = repository.getCommitEditor(commitMessage, null, false, env.getRevisionProperties(), null);
            SVNEditorHelper editorHelper = new SVNEditorHelper(editor);
            editor.openRoot(sourceRevision);
            logEnd("Get commit editor");

            //Create any missing paths
            logStart("Create missing subpaths");
            for (String path : missingPaths) {
                //Open the parent of the path
                String pathParent = SVNPathUtil.removeTail(path);
                editorHelper.openDir(pathParent);

                //Add the path
                editor.addDir(path, null, sourceRevision);
                editorHelper.addOpenedDir(path);
            }
            logEnd("Create missing subpaths");

            //Open the target directory and copy the source branch to the target
            editorHelper.openDir(branchParentPath);
            editor.addDir(branchPath, sourcePathPlusSubpath, sourceRevision);
            editorHelper.addOpenedDir(branchPath);

            //Copy the metdata file if we're copying from a subpath, not the top-level of the source path
            if (branchSubpath.length() > 0) {
                editor.addFile(branchMetadataFilePath, sourceMetadataFilePath, sourceRevision);
            }

            //Update all of the properties on the metadata file
            logStart("Create metadata file");
            editorHelper.openFile(branchMetadataFilePath);
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_PROJECT_NAME, SVNPropertyValue.create(projectName));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_SOURCE_PATH, SVNPropertyValue.create(sourcePath));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_PATH, SVNPropertyValue.create(branchPath));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_TYPE, SVNPropertyValue.create(branchType.getKeyword()));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_SUBPATH, SVNPropertyValue.create(branchSubpath));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_POINT_REVISION, SVNPropertyValue.create(Long.toString(sourceRevision)));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_LAST_MERGE_REVISION, SVNPropertyValue.create(Long.toString(sourceRevision)));
            logEnd("Create metadata file");

            //Close and commit all of the edits
            logStart("Close editor");
            editorHelper.closeAll();
            logEnd("Close editor");
        }
        catch (SVNException e) {
            String errorMessage =
                    "ERROR: Failed to perform copy: " + e +
                    "\nSource: " + sourcePathPlusSubpath +
                    "\nBranch: " + branchPath;
            SVNErrorManager.error(SVNErrorMessage.create(e.getErrorMessage().getErrorCode(), errorMessage), SVNLogType.CLIENT);
        }

        //Switch the working copy
        logStart("Switch to new branch");
        SVNURL branchURL = wcInfo.getRepositoryURL(branchPath);
        SVNUpdateClient updateClient = env.getClientManager().getUpdateClient();
        updateClient.setEventHandler(new SVNNotifyPrinter(env));
        updateClient.doSwitch(branchRootDir, branchURL, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        logEnd("Switch to new branch");

        //Revert any merged changes to the metadata file
        logStart("Revert metadata file");
        SVNWCClient wcClient = env.getClientManager().getWCClient();
        wcClient.doRevert(new File[] {new File(branchRootDir, wcInfo.getMetadataFile().getName())}, SVNDepth.EMPTY, null);
        logEnd("Revert metadata file");

        wcInfo = new WorkingCopyInfo(env.getClientManager(), branchRootDir);
        env.getOut().println(wcInfo);
    }

    private List<String> getMissingSubpaths(SVNRepository repository, String path, long revision)
            throws SVNException {
        logStart("Get Missing Subpaths");
        List<String> subpaths = PathUtil.getAllSubpaths(path);

        //Determine which subpaths don't exits
        List<String> missingSubpaths = new ArrayList<String>();
        for (String subpath : subpaths) {
            if (repository.checkPath(subpath, revision) == SVNNodeKind.NONE) {
                missingSubpaths.add(subpath);
            }
        }

        logEnd("Get Missing Subpaths");

        return missingSubpaths;
    }

    private String getCommitMessage(String branchName) throws SVNException {
        // get the commit message from the command line.  if it's not specified, default
        // to a reasonable value instead of starting an interactive editor. 
        String message = getSVNEnvironment().getCommitMessage(null, null);
        if (StringUtils.isEmpty(message)) {
            message = branchName + " - creating branch";
        }
        return message;
    }
}
