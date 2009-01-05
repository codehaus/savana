/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006-2008  Bazaarvoice Inc.
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
package org.codehaus.savana.scripts.admin;

import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.BranchType;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.scripts.SAVCommand;
import org.codehaus.savana.scripts.SAVCommandEnvironment;
import org.codehaus.savana.scripts.SAVOption;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CreateMetadataFile extends SAVCommand {

    public CreateMetadataFile() {
        super("createmetadatafile", new String[]{"bootstrap"});
    }

    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options.add(SAVOption.PROJECT_NAME);
        options.add(SAVOption.TRUNK_PATH);
        options.add(SAVOption.RELEASE_BRANCHES_PATH);
        options.add(SAVOption.USER_BRANCHES_PATH);
        options.add(SAVOption.VERSIONED_SYMLINKS_SUPPORTED);
        return options;
    }

    public void run() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(null, true);
        if (targets.size() < 3) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_INSUFFICIENT_ARGS), SVNLogType.CLIENT);
        }
        if (targets.size() > 4) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }
        String projectRoot = targets.get(0);
        String branchPath = targets.get(1);
        BranchType branchType = BranchType.fromKeyword(targets.get(2));
        String sourcePath = (targets.size() > 3) ? targets.get(3) : null;

        //Default the optional arguments if they weren't specified
        String projectName = env.getProjectName();
        if (projectName == null) {
            projectName = SVNPathUtil.tail(projectRoot);
        }

        //Source path is required for release and user branches, not allowed for trunk branch
        if (branchType == BranchType.TRUNK && sourcePath != null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_MUTUALLY_EXCLUSIVE_ARGS,
                    "ERROR: source path may not be specified for the trunk branch"), SVNLogType.CLIENT);
        } else if (branchType != BranchType.TRUNK && sourcePath == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_MUTUALLY_EXCLUSIVE_ARGS,
                    "ERROR: source path must be specified for release and user branches"), SVNLogType.CLIENT);
        }

        //Create the metadata file in the current directory
        File workspaceDir = new File(System.getProperty("user.dir"));
        File metadataFile = new File(workspaceDir, MetadataFile.METADATA_FILE_NAME);

        //Determine the file's path relative to the repository
        SVNWCClient wcClient = env.getClientManager().getWCClient();
        SVNInfo workspaceDirInfo = wcClient.doInfo(workspaceDir, SVNRevision.WORKING);
        SVNURL repositoryUrl = workspaceDirInfo.getRepositoryRootURL();
        SVNURL workspaceDirUrl = workspaceDirInfo.getURL();
        String workspaceDirPath = StringUtils.removeStart(workspaceDirUrl.getPath(), repositoryUrl.getPath());
        workspaceDirPath = StringUtils.removeStart(workspaceDirPath.substring(1), "/");

        //Make sure the rootDirPath and the branch path are the same
        if (!workspaceDirPath.equalsIgnoreCase(branchPath)) {
            String errorMessage =
                    "ERROR: Branch path argument does not match current repository location." +
                    "\nBranch Path: " + branchPath +
                    "\nCurrent Repository Location: " + workspaceDirPath;
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.BAD_RELATIVE_PATH, errorMessage), SVNLogType.CLIENT);
        }

        SVNRepository repository = env.getClientManager().createRepository(repositoryUrl, true);
        long latestRevision = repository.getLatestRevision();

        try {
            //Try to create the file
            if (!metadataFile.createNewFile()) {
                String errorMessage =
                        "ERROR: could not create metadata file." +
                                "\nPath: " + metadataFile.getAbsolutePath();
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR, errorMessage), SVNLogType.CLIENT);
            }

            //Write some text into the file to warn users not to modify it
            FileWriter writer = new FileWriter(metadataFile, false);
            writer.write("DO NOT MODIFY THIS FILE\n");
            writer.write("This file is used by Savana (sav) to store metadata about branches\n");
            writer.close();

        } catch (IOException ioe) {
            String errorMessage =
                    "ERROR: could not write to metadata file: " + ioe +
                            "\nPath: " + metadataFile.getAbsolutePath();
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR, errorMessage), SVNLogType.CLIENT);
        }

        //Add the file to source control
        wcClient.doAdd(metadataFile, false, false, false, SVNDepth.EMPTY, false, false);

        //Set the properties on the file
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_PROJECT_NAME, SVNPropertyValue.create(projectName), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_PROJECT_ROOT, SVNPropertyValue.create(projectRoot), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCH_TYPE, SVNPropertyValue.create(branchType.getKeyword()), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCH_PATH, SVNPropertyValue.create(branchPath), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_SOURCE_PATH, SVNPropertyValue.create(sourcePath), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_TRUNK_PATH, SVNPropertyValue.create(env.getTrunkPath()), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_RELEASE_BRANCHES_PATH, SVNPropertyValue.create(env.getReleaseBranchesPath()), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_USER_BRANCHES_PATH, SVNPropertyValue.create(env.getUserBranchesPath()), false, SVNDepth.EMPTY, null, null);
        wcClient.doSetProperty(metadataFile, MetadataFile.PROP_VERSIONED_SYMLINKS, SVNPropertyValue.create(Boolean.toString(env.isVersionedSymlinksSupported())), false, SVNDepth.EMPTY, null, null);

        //If we aren't on the trunk
        if (branchType != BranchType.TRUNK) {
            //Set the branch point
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_BRANCH_POINT_REVISION,
                    SVNPropertyValue.create(Long.toString(latestRevision)), false, SVNDepth.EMPTY, null, null);

            //Set the last merge revision
            wcClient.doSetProperty(metadataFile, MetadataFile.PROP_LAST_MERGE_REVISION,
                    SVNPropertyValue.create(Long.toString(latestRevision)), false, SVNDepth.EMPTY, null, null);
        }

        env.getOut().println("-------------------------------------------------");
        env.getOut().println("SUCCESS: Created metadata file: " + metadataFile);
        env.getOut().println("-------------------------------------------------");
        env.getOut().println();
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager());
        env.getOut().println(wcInfo);
        env.getOut().println();
        env.getOut().println("Please use 'svn commit' to store the created file in the Subversion repository.");
    }
}
