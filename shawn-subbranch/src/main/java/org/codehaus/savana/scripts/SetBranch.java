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

import org.codehaus.savana.BranchType;
import org.codehaus.savana.LocalChangeStatusHandler;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.PathUtil;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNNotifyPrinter;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SetBranch extends SAVCommand {

    public SetBranch() {
        super("setbranch", new String[]{"sb"});
    }

    @Override
    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options.add(SVNOption.FORCE); // force the branch to be created even if 'svn status' reports changes
        options.add(SAVOption.CHANGE_ROOT); // change the root of the current branch
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

        String branchPath = BranchType.TRUNK.getKeyword().equalsIgnoreCase(branchName) ?
                wcProps.getTrunkPath() :
                wcProps.getUserBranchPath(branchName);

        //If the path doesn't exist
        logStart("Check for the path");
        SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);
        if (repository.checkPath(repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.DIR) {
            //Try a release branch instead
            logStart("Check for the path as a release branch");
            branchPath = wcProps.getReleaseBranchPath(branchName);

            //If the path doesn't exist as a release branch either, throw an error
            if (repository.checkPath(repository.getRepositoryPath(branchPath), -1) != SVNNodeKind.DIR) {
                String errorMessage =
                        "ERROR: Could not find branch." +
                        "\nBranch Name: " + branchName;
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
            }
            logEnd("Check for the path as a release branch");
        }
        logEnd("Check for the path");

        //Get the metadata properties for the branch
        logStart("Get metadata properties");
        String branchMetadataFilePath = SVNPathUtil.append(branchPath, wcProps.getMetadataFileName());
        MetadataProperties branchProps = new MetadataProperties(repository, branchMetadataFilePath, -1);
        logEnd("Get metadata properties");

        //Get the root path for the working copy and the branch
        logStart("Get branch tree root paths");
        String wcRootPath = getBranchTreeRootPath(wcProps);
        String branchRootPath = getBranchTreeRootPath(branchProps);
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
        if (!env.isChangeRoot() && !wcRootPath.equals(branchRootPath)) {
            String errorMessage =
                    "ERROR: Can't switch to a branch with a different root than the working copy." +
                    "\nBranch Path:       " + branchPath +
                    "\nBranch Root:       " + branchRootPath +
                    "\nWorking Copy Root: " + wcRootPath;
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check switch type");

        //If the new branch is a subpath inside the working copy or vice versa, move the root directory.
        logStart("Get switch target directory");
        File branchRootDir = wcInfo.getRootDir();
        if (!wcProps.getSourceSubpath().equals(branchProps.getSourceSubpath())) {
            
            if (PathUtil.isSubpath(branchProps.getSourceSubpath(), wcProps.getSourceSubpath())) {
                // new branch is rooted in a subdirectory of the current working copy.  eg.
                //    workspace trunk   -> /
                //    workspace usersub -> /subdir
                // $ cd /project/working
                // $ sav sb trunk     => switches /project/working to 'trunk'
                // $ sav sb usersub   => switches /project/working/usersub to 'usersub'
                String relativeSubpath = PathUtil.getPathTail(branchProps.getSourceSubpath(), wcProps.getSourceSubpath());
                branchRootDir = new File(branchRootDir, relativeSubpath);

                // if in a subdirectory of the working copy, don't allow switching a sibling directory (must be a parent or child or self)
                File currentDirectory = new File("").getAbsoluteFile();
                if (!PathUtil.isAncestorOrDescendentOrSelf(branchRootDir, currentDirectory)) {
                    String errorMessage =
                            "ERROR: Can't switch to a branch that's not a child or parent of the current directory." +
                            "\nCurrent Directory: " + currentDirectory +
                            "\nBranch Root:       " + branchRootDir;
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
                }

            } else if (PathUtil.isSubpath(wcProps.getSourceSubpath(), branchProps.getSourceSubpath())) {
                // working copy is rooted in a subdirectory of the new branch.  eg.
                //    workspace trunk   -> /
                //    workspace usersub -> /subdir
                // $ cd /project/working
                // $ sav sb trunk     => switches /project/working to 'trunk'
                // $ sav sb usersub   => switches /project/working/usersub to 'usersub' branch
                // $ cd usersub
                // $ sav sb trunk     => switches /project/working/usersub to 'trunk' branch, but leaves parent alone
                String relativeSubpath = PathUtil.getPathTail(wcProps.getSourceSubpath(), branchProps.getSourceSubpath());
                branchPath = SVNPathUtil.append(branchPath, relativeSubpath);

                // if we want to switch to a location inside another branch (not the top-level directory)
                // make sure we're just realigning the branchRootDir with its parent working copy directory.
                WorkingCopyInfo parentInfo;
                try {
                    parentInfo = new WorkingCopyInfo(env.getClientManager(), branchRootDir.getParentFile());
                } catch (SVNException e) {
                    parentInfo = null;
                }
                if (parentInfo == null || !branchRootDir.equals(new File(parentInfo.getRootDir(), relativeSubpath)) ||
                    !parentInfo.getMetadataProperties().getBranchPath().equals(branchProps.getBranchPath())) {
                    String errorMessage =
                            "ERROR: Can't switch to a branch with a root above the working copy." +
                            "\nWorking Copy Root: <project>/" + wcProps.getSourceSubpath() +
                            "\nBranch Root:       <project>/" + branchProps.getSourceSubpath();
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
                }

            } else {
                // throw an error if the source subpath and working copy subpath aren't related.
                String errorMessage =
                        "ERROR: Can't switch to a directory outside of the working copy." +
                        "\nWorking Copy Root: <project>/" + wcProps.getSourceSubpath() +
                        "\nBranch Root:       <project>/" + branchProps.getSourceSubpath();
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
            }
        }
        logEnd("Get switch target directory");

        //Validate branchRootDir exists in the working copy and isn't marked deleted, added, etc. since we'll 'svn switch' it later
        logStart("Validating working copy directory status");
        SVNStatusClient statusClient = env.getClientManager().getStatusClient();
        SVNStatus branchRootDirStatus = statusClient.doStatus(branchRootDir, false);
        if (branchRootDirStatus == null || branchRootDirStatus.getKind() != SVNNodeKind.DIR) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                    "ERROR: Branch root must be a versioned directory.\nPath: " + branchRootDir), SVNLogType.CLIENT);
        }
        SVNStatusType branchRootDirStatusType = branchRootDirStatus.getContentsStatus();
        if (branchRootDirStatusType != SVNStatusType.STATUS_NORMAL && branchRootDirStatusType != SVNStatusType.STATUS_MODIFIED) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                    "ERROR: Branch root directory may not have a status of " +
                    branchRootDirStatusType + ".\nPath" + branchRootDir), SVNLogType.CLIENT);
        }
        logEnd("Validating working copy directory status");

        //Make sure all changes are committed before we run the switch
        if (!env.isForce()) {
            logStart("Check for local changes");
            LocalChangeStatusHandler statusHandler = new LocalChangeStatusHandler();
            statusClient.doStatus(branchRootDir, SVNRevision.UNDEFINED,
                    SVNDepth.INFINITY, false, true, false, false, statusHandler, null);
            if (statusHandler.isChanged()) {
                String errorMessage =
                        "ERROR: Cannot switch branches while the working copy has local changes." +
                        "\nRun 'svn status' to find changes or retry with --force";
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
            }
            if (statusHandler.isSwitched()) {
                String errorMessage =
                        "ERROR: Cannot switch branches while a subdirectory or file is switched relative to the root." +
                        "\nRun 'sav info -R' to find nested workspaces or retry with --force";
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
            }
            logEnd("Check for local changes");
        }

        //Create the branch URL
        SVNURL branchURL = wcInfo.getRepositoryURL(branchPath);

        //Switch the working copy to the new branch
        logStart("Get update client");
        SVNUpdateClient updateClient = env.getClientManager().getUpdateClient();
        updateClient.setEventHandler(new SVNNotifyPrinter(env));
        logEnd("Get update client");
        logStart("Do switch");
        updateClient.doSwitch(branchRootDir, branchURL, SVNRevision.UNDEFINED, SVNRevision.HEAD,
                SVNDepth.INFINITY, false, false);
        logEnd("Do switch");

        //Even if we are forcing the setbranch while there are uncommitted changes
        //Changes to the metadata file should never be ported from one branch to another
        File branchMetadataFile = new File(branchRootDir, wcProps.getMetadataFileName());
        if (env.isForce() && branchMetadataFile.exists()) {
            //Revert any changes to the metadata file'
            logStart("Revert metadata file");
            SVNWCClient wcClient = env.getClientManager().getWCClient();
            wcClient.doRevert(new File[] {branchMetadataFile}, SVNDepth.EMPTY, null);
            logEnd("Revert metadata file");
        }

        //Print out information about the new branch
        logStart("Print working copy info");
        wcInfo = new WorkingCopyInfo(env.getClientManager(), branchRootDir);
        env.getOut().println("");
        wcInfo.println(env.getOut(), false);
        logEnd("Print working copy info");
    }

    private String getBranchTreeRootPath(MetadataProperties props) {
        //Return the path that is the source of all branches from this point.
        // * For a user branch, that's just the real source of the user branch.
        // * Since release branches are like a trunk for user branches made off of them, we want to return the path of the release branch.
        // * For the trunk, there is no source path, so we want to return the branch path (which is trunk)
        return props.getBranchType() == BranchType.USER_BRANCH ? props.getSourcePath() : props.getBranchPath();
    }
}
