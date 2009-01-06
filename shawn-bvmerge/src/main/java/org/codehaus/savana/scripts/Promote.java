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
package org.codehaus.savana.scripts;

import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.BranchType;
import org.codehaus.savana.FilteredStatusHandler;
import org.codehaus.savana.LocalChangeStatusHandler;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.PreCommitValidator;
import org.codehaus.savana.WorkingCopyInfo;
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
import java.util.List;

public class Promote extends SAVCommand {

    public Promote() {
        super("promote", new String[]{});
    }

    public boolean isCommitter() {
        return true;
    }

    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options = SVNOption.addLogMessageOptions(options);
        return options;
    }

    public void run() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(null, false);
        if (targets.size() > 0) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }
        String commitMessage = env.getCommitMessage(null, null);
        if (StringUtils.isEmpty(commitMessage)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CL_INSUFFICIENT_ARGS,
                    "Commit log message argument is required");
            SVNErrorManager.error(err, SVNLogType.CLIENT);
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

        //Validate the commit comment against the branch name before we make any changes
        logStart("Validate commit comment");
        new PreCommitValidator().validate(wcProps.getSourceName(), wcProps.getSourceBranchType(), commitMessage);
        logEnd("Validate commit comment");

        //Make sure all changes are committed first
        logStart("Check for local changes");
        LocalChangeStatusHandler statusHandler = new LocalChangeStatusHandler();
        SVNStatusClient statusClient = env.getClientManager().getStatusClient();
        statusClient.doStatus(wcInfo.getRootDir(), SVNRevision.HEAD, SVNDepth.INFINITY,
                false, true, false, false, statusHandler, null);
        if (statusHandler.isChanged()) {
            //TODO: Just list the changes here rather than making the user run 'svn status'
            String errorMessage =
                    "ERROR: Cannot promote while the working copy has local changes." +
                            "\nRun 'svn status' to find changes";
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
        logEnd("Revert metadata file");

        //Don't allow the promote if there are replaced files
        logStart("Check for replaced files");
        FilteredStatusHandler filteredStatusHandler = new FilteredStatusHandler(SVNStatusType.STATUS_REPLACED);
        statusClient.doStatus(wcInfo.getRootDir(), SVNRevision.HEAD, SVNDepth.INFINITY,
                false, true, false, false, filteredStatusHandler, null);
        if (!filteredStatusHandler.getEntries().isEmpty()) {
            StringBuilder errorMessageBuilder = new StringBuilder();
            errorMessageBuilder.append(MessageFormat.format("ERROR: Cannot promote branch {0} while there are replaced files:", wcProps.getBranchName()));
            for (String entryName : filteredStatusHandler.getEntries()) {
                errorMessageBuilder.append("\n- ").append(entryName);
            }
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CLIENT_NOT_READY_TO_MERGE, errorMessageBuilder.toString()), SVNLogType.CLIENT);
        }
        logEnd("Check for replaced files");

        //Commit the changes to the source
        logStart("Commit changes");
        SVNCommitClient commitClient = env.getClientManager().getCommitClient();
        SVNCommitInfo commitInfo = commitClient.doCommit(new File[] {wcInfo.getRootDir()}, false, commitMessage,
                null, null, false, false, SVNDepth.INFINITY);
        logEnd("Commit changes");

        //Delete the user branch
        logStart("Perform Delete");
        commitClient.doDelete(new SVNURL[] {branchURL}, wcProps.getBranchName() + " - deleting promoted branch");
        logEnd("Perform Delete");

        //Print the new working copy info
        wcInfo = new WorkingCopyInfo(env.getClientManager());
        env.getOut().println("");
        env.getOut().println(wcInfo);
        env.getOut().println("");
        env.getOut().println("Promotion Changeset:   [" + commitInfo.getNewRevision() + "]");
    }
}