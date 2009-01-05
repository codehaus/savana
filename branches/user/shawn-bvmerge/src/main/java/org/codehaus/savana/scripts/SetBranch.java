package org.codehaus.savana.scripts;

import org.codehaus.savana.LocalChangeStatusHandler;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.util.cli.CommandLineProcessor;
import org.codehaus.savana.util.cli.SavanaArgument;
import org.codehaus.savana.util.cli.SavanaOption;
import org.tmatesoft.svn.cli.svn.SVNCommandEnvironment;
import org.tmatesoft.svn.cli.svn.SVNNotifyPrinter;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;

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
public class SetBranch extends SVNScript {
    private static final SavanaArgument BRANCH = new SavanaArgument(
            "branch", "the name of the branch to set");
    private static final SavanaOption FORCE = new SavanaOption(
            "F", "force", false, false, "force the branch to be changed, even if there are changes.");
    private static final SavanaOption CHANGE_ROOT = new SavanaOption(
            "C", "changeRoot", false, false, "change the root of the current branch");

    private boolean _force;
    private boolean _changeRoot;
    private String _branchName;

    public SetBranch()
            throws SVNException, SVNScriptException {
    }

    public CommandLineProcessor constructCommandLineProcessor() {
        return new CommandLineProcessor(
                new CommandLineProcessor.OptionHandler(FORCE) {
                    public void ifSet() {
                        _force = true;
                    }
                },
                new CommandLineProcessor.OptionHandler(CHANGE_ROOT) {
                    public void ifSet() {
                        _changeRoot = true;
                    }
                },
                new CommandLineProcessor.ArgumentHandler(BRANCH) {
                    public void handle(String arg) {
                        _branchName = arg;
                    }
                });
    }

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //Make sure all changes are committed first
        if (!_force) {
            logStart("Check for local changes");
            LocalChangeStatusHandler statusHandler = new LocalChangeStatusHandler();
            SVNStatusClient statusClient = _clientManager.getStatusClient();
            statusClient.doStatus(wcInfo.getRootDir(), SVNRevision.UNDEFINED,
                                  SVNDepth.fromRecurse(true), false, true, false, false,
                                  statusHandler, null);
            if (statusHandler.isChanged()) {
                //TODO: Just list the changes here rather than making the user run 'svn status'
                String errorMessage =
                        "ERROR: Cannot switch branches while the working copy has local changes." +
                        "\nRun 'svn status' to find changes";
                throw new SVNScriptException(errorMessage);
            }
            logEnd("Check for local changes");
        }

        String branchPath = MetadataFile.BRANCH_TYPE_TRUNK.equalsIgnoreCase(_branchName) ?
                            wcInfo.getTrunkPath() :
                            wcInfo.getUserBranchPath(_branchName);

        //If the path doesn't exist
        logStart("Check for the path");
        if (_repository.checkPath(_repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.DIR) {
            //Try a release branch instead
            logStart("Check for the path as a release branch");
            branchPath = wcInfo.getReleaseBranchPath(_branchName);

            //If the path doesn't exist as a release branch either, throw an error
            if (_repository.checkPath(_repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.DIR) {
                String errorMessage =
                        "ERROR: Could not find branch." +
                        "\nBranch Name: " + _branchName;
                throw new SVNScriptException(errorMessage);
            }
            logEnd("Check for the path as a release branch");
        }
        logEnd("Check for the path");

        //Get the metadata properties for the branch
        logStart("Get metadata properties");
        String branchMetadataFilePath = SVNPathUtil.append(branchPath, MetadataFile.METADATA_FILE_NAME);
        SVNProperties branchProperties = new SVNProperties();
        _repository.getFile(branchMetadataFilePath, -1, branchProperties, null);
        logEnd("Get metadata properties");

        //Get the root path for the working copy and the branch
        logStart("Get branch tree root paths");
        String wcRootPath = getBranchTreeRootPath(wcInfo.getBranchType(), wcInfo.getSourcePath(), wcInfo.getBranchPath());
        String branchRootPath = getBranchTreeRootPath(
                branchProperties.getStringValue(MetadataFile.PROP_BRANCH_TYPE),
                branchProperties.getStringValue(MetadataFile.PROP_SOURCE_PATH),
                branchProperties.getStringValue(MetadataFile.PROP_BRANCH_PATH));
        logEnd("Get branch tree root paths");

        //Make sure that we are switching to a branch that has the same root as the working copy
        //This means that the following switches are allowed without the force flag
        //1. trunk -> trunk
        //2. trunk -> user branch of trunk
        //3. user branch of trunk -> user branch of trunk
        //4. release branch -> same release branch
        //5. release branch -> user branch of the same release branch
        //6. user branch of a release branch -> user branch of the same release branch
        logStart("Check switch type");
        if (!_changeRoot && !wcRootPath.equals(branchRootPath)) {
            String errorMessage =
                    "ERROR: Can't switch to a branch with a different root than the working copy." +
                    "\nBranch Path:       " + branchPath +
                    "\nBranch Root:       " + branchRootPath +
                    "\nWorking Copy Root: " + wcRootPath;
            throw new SVNScriptException(errorMessage);
        }
        logEnd("Check switch type");

        //Create the branch URL
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL branchURL = repositoryURL.appendPath(branchPath, false);

        //Switch the working copy to the new branch
        logStart("Get update client");
        SVNUpdateClient updateClient = _clientManager.getUpdateClient();
        updateClient.setEventHandler(new SVNNotifyPrinter(
                    new SVNCommandEnvironment("savana", getOut(), getOut(), System.in)));
        logEnd("Get update client");
        logStart("Do switch");
        updateClient.doSwitch(wcInfo.getRootDir(), branchURL, SVNRevision.UNDEFINED, SVNRevision.HEAD,
                              SVNDepth.fromRecurse(true), false, false);
        logEnd("Do switch");

        //Even if we are forcing the setbranch while there are uncommitted changes
        //Changes to the metadata file should never be ported from one branch to another
        if (_force) {
            //Revert any changes to the metadata file'
            logStart("Revert metadata file");
            SVNWCClient wcClient = _clientManager.getWCClient();
            wcClient.doRevert(new File[] {wcInfo.getMetadataFile()}, 
                              SVNDepth.fromRecurse(false), null);
            logEnd("Revert metadata file");
        }

        //Print out information about the new branch
        logStart("Print working copy info");
        wcInfo = new WorkingCopyInfo(_clientManager);
        getOut().println("");
        getOut().println(wcInfo);
        logEnd("Print working copy info");
    }

    private String getBranchTreeRootPath(String branchType, String sourcePath, String branchPath) {
        //We want to find the path that is the source of all branches from this point
        //For a user branch, that's just the real source of the user branch

        //Since release branches are like a trunk for user branches made off of them, we want to return the
        //path of the release branch

        //For the trunk, there is no source path, so we want to return the branch path (which is trunk)
        return (MetadataFile.BRANCH_TYPE_USER_BRANCH.equals(branchType)) ?
               sourcePath :
               branchPath;
    }

    public String getUsageMessage() {
        return _commandLineProcessor.usage("setbranch");
    }
}
