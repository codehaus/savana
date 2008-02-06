package org.codehaus.savana.scripts;

import org.codehaus.savana.*;
import org.codehaus.savana.util.cli.CommandLineProcessor;
import org.codehaus.savana.util.cli.SavanaArgument;
import org.codehaus.savana.util.cli.SavanaOption;
import org.tmatesoft.svn.cli.command.SVNCommandEventProcessor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006  Bazaarvoice Inc.
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
 */
public class CreateBranch extends SVNScript {
    private static final SavanaArgument BRANCH = new SavanaArgument(
            "branch", "the name of the branch to create");
    private static final SavanaOption FORCE = new SavanaOption(
            "F", "force", false, false, "force the branch to be created");
    private static final SavanaOption MESSAGE = new SavanaOption(
            "m", "message", true, false, "override the svn log message");

    private boolean _userBranch;

    private boolean _force;
    private String _branchName;
    private String _commitMessage;

    public CreateBranch()
            throws SVNException, SVNScriptException {
        this(false);
    }

    public CreateBranch(boolean userBranch)
            throws SVNException, SVNScriptException {
        _userBranch = userBranch;
    }

    public CommandLineProcessor constructCommandLineProcessor() {
        return new CommandLineProcessor(
                new CommandLineProcessor.ArgumentHandler(BRANCH) {
                    public void handle(String arg) {
                        _branchName = arg;
                    }
                },
                new CommandLineProcessor.OptionHandler(FORCE) {
                    public void ifSet() {
                        _force = true;
                    }
                },
                new CommandLineProcessor.OptionHandler(MESSAGE) {
                    public void withArg(String arg) {
                        _commitMessage = arg;
                    }
                });
    }

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //Don't allow the user to convert a working copy to a new branch if there are uncommitted changes, unless:
        //1. The force flag is enabled AND
        //2. The new branch will be a user branch
        if (!_force || !_userBranch) {
            logStart("Looking for local changes");
            LocalChangeStatusHandler statusHandler = new LocalChangeStatusHandler();
            SVNStatusClient statusClient = _clientManager.getStatusClient();
            statusClient.doStatus(wcInfo.getRootDir(), true, false, true, false, statusHandler);
            logEnd("Looking for local changes");
            if (statusHandler.isChanged()) {
                //TODO: Just list the changes here rather than making the user run 'svn status'
                String errorMessage =
                        "ERROR: Cannot create a new branch while the working copy has local changes." +
                        "\nRun 'svn status' to find changes";
                throw new SVNScriptException(errorMessage);
            }
        }

        //Get the project name from the working copy
        String projectName = wcInfo.getProjectName();

        //Get the branch type
        String branchType = (_userBranch) ? MetadataFile.BRANCH_TYPE_USER_BRANCH : MetadataFile.BRANCH_TYPE_RELEASE_BRANCH;

        //Get the source path of the new branch based on the current working copy.
        //WC=TRUNK          ==> SOURCE=TRUNK (Branch path of working copy)
        //WC=RELEASE BRANCH ==> SOURCE=RELEASE BRANCH (Branch path of working copy)
        //WC=USER BRANCH    ==> SOURCE=SOURCE OF USER BRANCH (Source path of working copy)
        String sourcePath = (MetadataFile.BRANCH_TYPE_USER_BRANCH.equals(wcInfo.getBranchType())) ?
                            wcInfo.getSourcePath() :
                            wcInfo.getBranchPath();

        //Get the branch path for the new branch
        String branchPath = (_userBranch) ?
                            wcInfo.getUserBranchPath(_branchName) :
                            wcInfo.getReleaseBranchPath(_branchName);

        //Make sure the branch doesn't exist
        logStart("Check that the branch doesn't exist");
        if (_repository.checkPath(_repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.NONE) {
            String errorMessage =
                    "ERROR: Branch already exists." +
                    "\nBranch Path: " + branchPath;
            throw new SVNScriptException(errorMessage);
        }
        logEnd("Check that the branch doesn't exist");

        //Figure out the paths to the parent dir of the branch and to the metadata file
        String branchParentPath = SVNPathUtil.removeTail(branchPath);
        String branchMetadataFilePath =
                SVNPathUtil.append(branchPath, MetadataFile.METADATA_FILE_NAME);

        //Figure out which subpaths to the branch are missing
        List<String> missingPaths = getMissingSubpaths(_repository, branchParentPath);

        //Perform the copy
        try {
            long sourceRevision = _repository.getLatestRevision();

            //Create an editor
            logStart("Get commit editor");
            final String commitMessage =
                    _commitMessage == null ? "Creating branch: " + _branchName : _commitMessage;
            ISVNEditor editor = _repository.getCommitEditor(commitMessage, null);
            SVNEditorHelper editorHelper = new SVNEditorHelper(editor);
            editor.openRoot(-1);
            logEnd("Get commit editor");

            //Create any missing paths
            logStart("Create missing subpaths");
            for (String path : missingPaths) {
                //Open the parent of the path
                String pathParent = SVNPathUtil.removeTail(path);
                editorHelper.openDir(pathParent);

                //Add the path
                editor.addDir(path, null, -1);
                editorHelper.addOpenedDir(path);
            }
            logEnd("Create missing subpaths");

            //Open the target directory and copy the source branch to the target
            editorHelper.openDir(branchParentPath);
            editor.addDir(branchPath, sourcePath, sourceRevision);
            editorHelper.addOpenedDir(branchPath);

            //Update all of the properties on the metadata file
            logStart("Create metadata file");
            editorHelper.openFile(branchMetadataFilePath);
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_PROJECT_NAME, projectName);
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_SOURCE_PATH, sourcePath);
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_PATH, branchPath);
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_TYPE, branchType);
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_BRANCH_POINT_REVISION, Long.toString(sourceRevision));
            editor.changeFileProperty(branchMetadataFilePath, MetadataFile.PROP_LAST_MERGE_REVISION, Long.toString(sourceRevision));
            logEnd("Create metadata file");

            //Close and commit all of the edits
            logStart("Close editor");
            editorHelper.closeAll();
            logEnd("Close editor");
        }
        catch (SVNException e) {
            String errorMessage =
                    "ERROR: Failed to perform copy." +
                    "\nSource: " + sourcePath +
                    "\nBranch: " + branchPath;
            throw new SVNScriptException(errorMessage, e);
        }

        //Switch the working copy
        logStart("Switch to new branch");
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL branchURL = repositoryURL.appendPath(branchPath, false);
        SVNUpdateClient updateClient = _clientManager.getUpdateClient();
        updateClient.setEventHandler(new SVNCommandEventProcessor(getOut(), System.err, false));
        updateClient.doSwitch(wcInfo.getRootDir(), branchURL, SVNRevision.HEAD, true);
        logEnd("Switch to new branch");

        //Revert the changes to the metadata file
        logStart("Revert metadata file");
        SVNWCClient wcClient = _clientManager.getWCClient();
        wcClient.doRevert(wcInfo.getMetadataFile(), false);
        logEnd("Revert metadata file");

        wcInfo = new WorkingCopyInfo(_clientManager);
        getOut().println(wcInfo);
    }

    private List<String> getMissingSubpaths(SVNRepository repository, String path)
            throws SVNException {
        logStart("Get Missing Subpaths");
        List<String> subpaths = PathUtil.getAllSubpaths(path);

        //Determine which subpaths don't exits
        List<String> missingSubpaths = new ArrayList<String>();
        for (String subpath : subpaths) {
            if (repository.checkPath(subpath, -1) == SVNNodeKind.NONE) {
                missingSubpaths.add(subpath);
            }
        }

        logEnd("Get Missing Subpaths");

        return missingSubpaths;
    }

    public String getUsageMessage() {
        return _commandLineProcessor.usage("createbranch");
    }
}
