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
import org.codehaus.savana.FilteredStatusHandler;
import org.codehaus.savana.LocalChangeStatusHandler;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.SVNCommandUtil;
import org.tmatesoft.svn.cli.svn.SVNNotifyPrinter;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitItem;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Promote extends SAVCommand {

    public Promote() {
        super("promote", new String[]{});
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
        if (targets.size() > 0) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }

        //Get information about the current workspace from the metadata file
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager());
        MetadataProperties wcProps = wcInfo.getMetadataProperties();

        //Find the source and branch URLs
        SVNURL sourceURL = wcInfo.getRepositoryURL(wcProps.getSourcePath());
        SVNURL branchURL = wcInfo.getRepositoryURL(wcProps.getBranchPath());

        //Make sure that we are in a user branch
        logStart("Check for user branch");
        if (wcProps.getBranchType() != BranchType.USER_BRANCH) {
            String errorMessage =
                    "ERROR: Promotes can only be performed from user branches." +
                            "\nBranch: " + branchURL +
                            "\nBranch Type: " + wcProps.getBranchType().getKeyword();
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check for user branch");

        //Make sure all changes are committed first
        logStart("Check for local changes");
        LocalChangeStatusHandler statusHandler = new LocalChangeStatusHandler();
        SVNStatusClient statusClient = env.getClientManager().getStatusClient();
        statusClient.doStatus(wcInfo.getRootDir(), SVNRevision.HEAD, SVNDepth.INFINITY,
                true, true, false, false, statusHandler, null);
        if (statusHandler.isChanged()) {
            String errorMessage =
                    "ERROR: Cannot promote while the working copy has local changes." +
                            "\nRun 'svn status' to find changes";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        if (statusHandler.isSwitched()) {
            String errorMessage =
                    "ERROR: Cannot promote while a subdirectory or file is switched relative to the root." +
                            "\nRun 'sav info -R' to find nested workspaces";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        if (statusHandler.isOutOfDate()) {
            String errorMessage =
                    "ERROR: Cannot promote while the working copy is out-of-date." +
                            "\nRun 'svn update' to update the working copy";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check for local changes");

        //Find the last version that was changed in the branch
        SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);
        SVNRevision lastBranchCommitRevision = SVNRevision.create(repository.getLatestRevision());

        //Find the last version that was sync'd in
        SVNRevision lastMergeRevision = wcProps.getLastMergeRevision();

        //Find out if there are any changes in the source that need to be merged in
        logStart("Do info on source");
        SVNWCClient wcClient = env.getClientManager().getWCClient();
        SVNInfo sourceInfo = wcClient.doInfo(sourceURL, SVNRevision.HEAD, SVNRevision.HEAD);
        SVNRevision sourceLastChange = sourceInfo.getCommittedRevision();
        logEnd("Do info on source");

        //Don't allow the merge if there are source changes that need to be merged in
        //In addition to looking for log entries, also check the oldest log entry with the latest revision.  If they are
        //the same there is nothing to sync.
        logStart("Check for changes");
        if (sourceLastChange.getNumber() > wcProps.getLastMergeRevision().getNumber()) {
            String errorMessage =
                    "ERROR: There are unmerged changes in the source." +
                            "\nRun 'sav sync' to pull in unmerged changes";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check for changes");

        //Get metadata properties on the source so we can get its savana policies object
        logStart("Get metadata for the source branch");
        MetadataProperties sourceProps = new MetadataProperties(repository, wcProps.getSourceMetadataFilePath(), sourceInfo.getRevision().getNumber());
        logEnd("Get metadata for the source branch");

        //Get the commit message (may launch an external editor).  We don't want to diff branches yet, so pass a dummy string as the commit item.
        //Do this as late as we can so it's likely the promote will succeed after we launch the editor and the user enters the commit comment.
        logStart("Get commit comment");
        String message = "promote branch " + wcProps.getBranchName() + " to " + wcProps.getSourceName() + " in project " + wcProps.getProjectName();
        String commitMessage = env.getCommitMessage(null, new SVNCommitItem[]{createDummyCommitItem(message)});
        logEnd("Get commit comment");

        //Validate the commit comment against the branch name before we make any changes
        if (sourceProps.getSavanaPolicies() != null) {
            logStart("Validate commit comment");
            sourceProps.getSavanaPolicies().validateLogMessage(commitMessage, sourceProps, _sLog);
            logEnd("Validate commit comment");
        }

        ///////// All changes to the working copy and to the repository happen below ///////// 

        //Switch the working copy to the source
        logStart("Do switch to source");
        SVNUpdateClient updateClient = env.getClientManager().getUpdateClient();
        updateClient.doSwitch(wcInfo.getRootDir(), sourceURL, SVNRevision.UNDEFINED, lastMergeRevision,
                SVNDepth.INFINITY, false, false);
        logEnd("Do switch to source");

        //Merge in differences from [source:LAST_MERGE, branch:LAST_BRANCH_COMMIT] into the working copy
        logStart("Do merge");
        SVNDiffClient diffClient = env.getClientManager().getDiffClient();
        diffClient.setDiffGenerator(new DefaultSVNDiffGenerator());
        diffClient.setEventHandler(new SVNNotifyPrinter(env));
        diffClient.doMerge(sourceURL, lastMergeRevision, branchURL, lastBranchCommitRevision,
                wcInfo.getRootDir(), SVNDepth.INFINITY, true, false, false, false);
        logEnd("Do merge");

        logStart("Revert metadata file");
        //Revert the changes to the metadata file
        wcClient.doRevert(new File[] {wcInfo.getMetadataFile()}, SVNDepth.EMPTY, null);
        //Delete a subbranch metadata file
        if (wcProps.getSourceSubpath().length() > 0) {
            wcInfo.getMetadataFile().delete();
        }
        logEnd("Revert metadata file");

        //Don't allow the promote if there are replaced files
        logStart("Check for replaced files");
        FilteredStatusHandler filteredStatusHandler = new FilteredStatusHandler(SVNStatusType.STATUS_REPLACED);
        statusClient.doStatus(wcInfo.getRootDir(), SVNRevision.HEAD, SVNDepth.INFINITY,
                false, true, false, false, filteredStatusHandler, null);
        if (!filteredStatusHandler.getEntries().isEmpty()) {
            StringBuilder errorMessageBuilder = new StringBuilder();
            errorMessageBuilder.append(MessageFormat.format("ERROR: Cannot promote branch {0} while there are replaced files:", wcProps.getBranchName()));
            List<File> entries = filteredStatusHandler.getEntries();
            Collections.sort(entries);
            for (File file : entries) {
                errorMessageBuilder.append("\n- ").append(SVNCommandUtil.getLocalPath(env.getRelativePath(file)));
            }
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, errorMessageBuilder.toString()), SVNLogType.CLIENT);
        }
        logEnd("Check for replaced files");

        //Commit the changes to the source
        logStart("Commit changes");
        SVNCommitClient commitClient = env.getClientManager().getCommitClient();
        SVNCommitInfo commitInfo = commitClient.doCommit(new File[] {wcInfo.getRootDir()}, false, commitMessage,
                env.getRevisionProperties(), null, false, false, SVNDepth.INFINITY);
        logEnd("Commit changes");

        //Delete the user branch
        logStart("Perform Delete");
        commitClient.doDelete(new SVNURL[] {branchURL}, wcProps.getBranchName() + " - deleting promoted branch");
        logEnd("Perform Delete");

        //Print the new working copy info
        wcInfo = new WorkingCopyInfo(env.getClientManager());
        env.getOut().println("");
        wcInfo.println(env.getOut());
        env.getOut().println("");
        env.getOut().println("Promotion Changeset:   [" + commitInfo.getNewRevision() + "]");
    }

    private SVNCommitItem createDummyCommitItem(String message) {
        SVNCommitItem commitItem = new SVNCommitItem(null, null, null, null, null, null, false, false, false, false, false, false);
        commitItem.setPath(message);
        return commitItem;
    }
}
