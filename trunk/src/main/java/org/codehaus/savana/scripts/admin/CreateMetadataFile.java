package org.codehaus.savana.scripts.admin;

import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.scripts.SVNScript;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.io.FileWriter;

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
 * @author Brian Showers (brian@bazaarvoice.com)
 * @author Bryon Jacob (bryon@jacob.net)
 */
public class CreateMetadataFile extends SVNScript {
    private String _projectName = null;
    private String _branchPath = null;
    private String _branchType = null;
    private String _sourcePath = null;
    private String _commitMessage = "Added SVNScript Metadata File";
    private String _projectRoot = null;
    private String _trunkPath = MetadataFile.DEFAULT_TRUNK_PATH;
    private String _branchesPath = MetadataFile.DEFAULT_BRANCHES_PATH;
    private String _userBranchesPath = MetadataFile.DEFAULT_USER_BRANCHES_PATH;

    public CreateMetadataFile()
            throws SVNException, SVNScriptException {}

    public void initialize(String[] args)
            throws IllegalArgumentException {
        try {
            boolean settingMessageOption = false;
            boolean settingProjectRootOption = false;
            boolean settingTrunkPathOption = false;
            boolean settingBranchesPathOption = false;
            boolean settingUserBranchesPathOption = false;
            for (String arg : args) {
                // TODO: switch to apache CLI.
                if (settingMessageOption) {
                    _commitMessage = arg;
                    settingMessageOption = false;
                } else if ("-m".equals(arg)) {
                    settingMessageOption = true;
                } else if (settingProjectRootOption) {
                    _projectRoot = arg;
                    settingProjectRootOption = false;
                } else if ("-p".equals(arg)) {
                    settingProjectRootOption = true;
                } else if (settingTrunkPathOption) {
                    _trunkPath = arg;
                    settingTrunkPathOption = false;
                } else if ("-t".equals(arg)) {
                    settingTrunkPathOption = true;
                } else if (settingBranchesPathOption) {
                    _branchesPath = arg;
                    settingBranchesPathOption = false;
                } else if ("-b".equals(arg)) {
                    settingBranchesPathOption = true;
                } else if (settingUserBranchesPathOption) {
                    _userBranchesPath = arg;
                    settingUserBranchesPathOption = false;
                } else if ("-u".equals(arg)) {
                    settingUserBranchesPathOption = true;
                } else if (_projectName == null) {
                    _projectName = arg;
                } else if (_branchPath == null) {
                    _branchPath = arg;
                } else if (_branchType == null) {
                    _branchType = arg.toUpperCase();
                } else if (_sourcePath == null) {
                    _sourcePath = arg;
                } else {
                    throw new IllegalArgumentException("too many arguments!");
                }
            }
            if (settingMessageOption) {
                throw new IllegalArgumentException("the -m option takes an argument");
            }
            if (_branchType == null) {
                throw new IllegalArgumentException("not enough arguments!");
            }
            if (_projectRoot == null) {
                _projectRoot = _projectName;
            }
        }
        catch (Exception e) {
            throw e instanceof IllegalArgumentException ?
                  (IllegalArgumentException) e :
                  new IllegalArgumentException(e);
        }
    }

    public void run()
            throws SVNException, SVNScriptException {
        //Make sure the branch type is valid
        if (!MetadataFile.BRANCH_TYPE_TRUNK.equals(_branchType) &&
            !MetadataFile.BRANCH_TYPE_RELEASE_BRANCH.equals(_branchType) &&
            !MetadataFile.BRANCH_TYPE_USER_BRANCH.equals(_branchType)) {
            String errorMessage =
                    "ERROR: Unknown branch type." +
                    "\nBranch Type: " + _branchType;
            throw new SVNScriptException(errorMessage);
        }


        boolean needsDelete = false;
        boolean needsRevert = false;

        File metadataFile = new File(System.getProperty("user.dir"),
                                     MetadataFile.METADATA_FILE_NAME);

        try {
            //Try to create the file
            if (!metadataFile.createNewFile()) {
                String errorMessage =
                        "ERROR: could not create metadata file." +
                        "\nPath: " + metadataFile.getAbsolutePath();
                throw new SVNScriptException(errorMessage);
            }

            //At this point, the script added the file, so it should delete it on a failure
            needsDelete = true;

            //Write some text into the file to warn users not to modify it
            FileWriter writer = new FileWriter(metadataFile, false);
            writer.write("DO NOT MODIFY THIS FILE\n");
            writer.write("This file is used by Savana to store metadata about branches");
            writer.close();

            //Add the file to source control
            SVNWCClient wcClient = _clientManager.getWCClient();
            wcClient.doAdd(metadataFile, false, false, false, false);

            //At this point, the script did the add, so it should revert on a failure
            needsRevert = true;

            //Determine the file's path relative to the repository
            SVNInfo metadataFileInfo = wcClient.doInfo(metadataFile, SVNRevision.WORKING);
            String metadataFilePath = metadataFileInfo.getURL().getURIEncodedPath();
            metadataFilePath = metadataFilePath.substring(_repository.getRepositoryRoot(true).getPath().length());

            //Find the path to the directory that the metadata file lives in
            String rootDirPath = SVNPathUtil.removeTail(metadataFilePath);
            if (rootDirPath.charAt(0) == '/') {
                rootDirPath = rootDirPath.substring(1);
            }

            //Make sure the rootDirPath and the branch path are the same
            if (!rootDirPath.equalsIgnoreCase(_branchPath)) {
                String errorMessage =
                        "ERROR: Branch path does not match current repository location." +
                        "\nBranch Path: " + _branchPath +
                        "\nCurrent Repository Location: " + rootDirPath;
                throw new SVNScriptException(errorMessage);
            }

            //Set the properties on the file
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_PROJECT_NAME, _projectName, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCH_PATH, _branchPath, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCH_TYPE, _branchType, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_SOURCE_PATH, _sourcePath, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_PROJECT_ROOT, _projectRoot, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_TRUNK_PATH, _trunkPath, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCHES_PATH, _branchesPath, false, false, null);
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_USER_BRANCHES_PATH, _userBranchesPath, false, false, null);

            //If we aren't on the trunk
            if (!MetadataFile.BRANCH_TYPE_TRUNK.equals(_branchType)) {
                //Set the branch point
                wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCH_POINT_REVISION, Long.toString(_repository.getLatestRevision()), false, false, null);

                //Set the last merge revision
                wcClient.doSetProperty(metadataFile, MetadataFile.PROP_LAST_MERGE_REVISION, Long.toString(_repository.getLatestRevision()), false, false, null);
            }

            //Commit the changes
            SVNCommitClient commitClient = _clientManager.getCommitClient();
            commitClient.doCommit(new File[]{metadataFile}, false, _commitMessage, false, false);
            getOut().println("-------------------------------------------------");
            getOut().println("SUCCESS: Commited metadata file.");
            getOut().println("-------------------------------------------------");
            getOut().println("");
            WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);
            getOut().println(wcInfo);
        }
        catch (SVNScriptException e) {
            if (needsRevert) {
                revertFile(metadataFile);
            }
            if (needsDelete) {
                deleteFile(metadataFile);
            }
            throw (e);
        }
        catch (Exception e) {
            if (needsRevert) {
                revertFile(metadataFile);
            }
            if (needsDelete) {
                deleteFile(metadataFile);
            }
            String errorMessage =
                    "ERROR: could not create metadata file." +
                    "\nPath: " + metadataFile.getAbsolutePath();
            throw new SVNScriptException(errorMessage, e);
        }
    }

    private void deleteFile(File file) {
        //Try to delete the file from the file system
        try {
            file.delete();
        }
        catch (Exception e) {}
    }

    private void revertFile(File file) {
        try {
            SVNWCClient wcClient = _clientManager.getWCClient();
            wcClient.doRevert(file, false);
        }
        catch (SVNException e) {}
    }

    public String getUsageMessage() {
        return
                "Usage: ssadmin createmetadatafile <project name> <branch path> <branch type> [<source path>]" +
                "\n  project name:  name of the project to branch" +
                "\n  branch path:   path to the branch relative to the repository root directory" +
                "\n  branch type:   type of branch (TRUNK|RELEASE BRANCH|USER BRANCH|" +
                "\n  source path:   path to the source relative to the repository root directory" +
                "\n                 for type TRUNK, this should be the same as <branch path>";
    }
}
