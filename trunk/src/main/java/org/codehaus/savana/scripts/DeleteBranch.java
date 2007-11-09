package org.codehaus.savana.scripts;

import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNScriptException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.SVNCommitClient;

import java.util.HashMap;
import java.util.Map;

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
public class DeleteBranch extends SVNScript {
    private static final String FORCE_FLAG = "--force";

    private boolean _force;
    private String _projectName;
    private String _branchName;
    private boolean _userBranch;

    public DeleteBranch()
            throws SVNException, SVNScriptException {
        this(false);
    }

    public DeleteBranch(boolean userBranch)
            throws SVNException, SVNScriptException {
        _userBranch = userBranch;
    }

    public void initialize(String[] args)
            throws IllegalArgumentException {
        for (String arg : args) {
            if (FORCE_FLAG.equalsIgnoreCase(arg)) {
                _force = true;
                continue;
            }

            if (_projectName == null) {
                _projectName = arg;
                continue;
            }

            if (_branchName == null) {
                _branchName = arg;
                continue;
            }

            //If extra parameters were specified
            throw new IllegalArgumentException();
        }

        //Make sure project and branch names aren't null
        if (_projectName == null || _branchName == null) {
            throw new IllegalArgumentException();
        }
    }

    public void run()
            throws SVNException, SVNScriptException {
        //Find the source of the branch
        String branchPath = (_userBranch) ?
                            _pathGenerator.getUserBranchPath(_projectName, _branchName) :
                            _pathGenerator.getReleaseBranchPath(_projectName, _branchName);

        //Make sure the branch exists
        logStart("Check if branch exists");
        if (_repository.checkPath(_repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.DIR) {
            String errorMessage =
                    "ERROR: Could not find branch." +
                    "\nBranch Path: " + branchPath;
            throw new SVNScriptException(errorMessage);
        }
        logEnd("Check if branch exists");

        //If we aren't forcing the delete, make sure the branch has been promoted at least once
        if (!_force) {
            logStart("Check if promoted");
            //Get the properties of the metadata file
            String metadataFilePath = SVNPathUtil.append(branchPath, MetadataFile.METADATA_FILE_NAME);
            Map metadataFileProperties = new HashMap();
            _repository.getFile(metadataFilePath, -1, metadataFileProperties, null);

            //Make sure the branch was promoted
            if (metadataFileProperties.get(MetadataFile.PROP_LAST_PROMOTE_REVISION) == null) {
                String errorMessage =
                        "ERROR: Branch has not been promoted." +
                        "\nEither promote the branch or use the --force flag to delete" +
                        "\nBranch Path: " + branchPath;
                throw new SVNScriptException(errorMessage);
            }
            logEnd("Check if promoted");
        }

        //Perform delete
        logStart("Get Commit Client");
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL branchURL = repositoryURL.appendPath(branchPath, false);
        SVNCommitClient commitClient = _clientManager.getCommitClient();
        logEnd("Get Commit Client");

        logStart("Perform Delete");
        commitClient.doDelete(new SVNURL[]{branchURL}, "Deleting branch: " + branchURL);
        logEnd("Perform Delete");

        getOut().println("Deleted Branch: " + branchPath);
    }

    public String getUsageMessage() {
        return
                "Usage: ss deletebranch [options] <project name> <branch name>" +
                "\n  project name:  name of the branch's project" +
                "\n  branch name:   name of the branch to delete" +
                "\n  --force:       delete the branch even if it has never" +
                "\n                 been promoted";
    }
}
