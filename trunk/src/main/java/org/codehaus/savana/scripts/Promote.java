package org.codehaus.savana.scripts;

import org.codehaus.savana.ChangeLogEntryHandler;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.util.cli.CommandLineProcessor;
import org.codehaus.savana.util.cli.SavanaArgument;
import org.tmatesoft.svn.cli.command.SVNCommandEventProcessor;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.*;

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
 * @author Brian Showers (brian@bazaarvoice.com)
 * @author Bryon Jacob (bryon@jacob.net)
 */
public class Promote extends SVNScript {
    private static final SavanaArgument MESSAGE = new SavanaArgument(
            "message", "the SVN commit message for the check-in to trunk");

    private String _commitMessage;

    public Promote()
            throws SVNException, SVNScriptException {}

    public CommandLineProcessor constructCommandLineProcessor() {
        return new CommandLineProcessor(
                new CommandLineProcessor.ArgumentHandler(MESSAGE) {
                    public void handle(String arg) {
                        _commitMessage = arg;
                    }
                });
    }

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //Find the source and branch URLs
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL sourceURL = repositoryURL.appendPath(wcInfo.getSourcePath(), false);
        SVNURL branchURL = repositoryURL.appendPath(wcInfo.getBranchPath(), false);

        //Make sure that we are in a user branch
        logStart("Check for user branch");
        if (!MetadataFile.BRANCH_TYPE_USER_BRANCH.equals(wcInfo.getBranchType())) {
            String errorMessage =
                    "ERROR: Merges can only be performed from user branches." +
                    "\nBranch: " + branchURL +
                    "\nBranch Type: " + wcInfo.getBranchType();
            throw new SVNScriptException(errorMessage);
        }
        logEnd("Check for user branch");

        //Commit the changes in the branch
        //TODO: Make sure that we are still really in the user branch.  If a previous promote failed, we might be in the trunk but with all of the files from the branch.
        logStart("Do commit");
        SVNCommitClient commitClient = _clientManager.getCommitClient();
        commitClient.doCommit(new File[]{wcInfo.getRootDir()}, false, _commitMessage, false, true);
        logEnd("Do commit");

        //Find the last version that was changed in the branch
        SVNRevision lastBranchCommitRevision = SVNRevision.create(_repository.getLatestRevision());

        //Find the last version that was sync'd in
        SVNRevision lastMergeRevision = wcInfo.getLastMergeRevision();

        //Find out if there are any changes in the source that need to be merged in
        logStart("Do log");
        SVNLogClient logClient = _clientManager.getLogClient();
        ChangeLogEntryHandler logEntryHandler = new ChangeLogEntryHandler();
        logClient.doLog(sourceURL, new String[]{""}, lastMergeRevision, lastMergeRevision, SVNRevision.HEAD, false, false, 1, logEntryHandler);
        logEnd("Do log");

        //Don't allow the merge if there are source changes that need to be merged in
        //In addition to looking for log entries, also check the oldest log entry with the latest revision.  If they are
        //the same there is nothing to sync.
        logStart("Check for changes");
        if (logEntryHandler.isChanged() && _repository.getLatestRevision() != wcInfo.getLastMergeRevision().getNumber()) {
            String errorMessage =
                    "ERROR: There are unmerged changes in the source." +
                    "\nRun 'sav sync' to pull in unmerged changes";
            throw new SVNScriptException(errorMessage);
        }
        logEnd("Check for changes");

        //Switch the working copy to the source
        logStart("Do switch to source");
        SVNUpdateClient updateClient = _clientManager.getUpdateClient();
        updateClient.doSwitch(wcInfo.getRootDir(), sourceURL, lastMergeRevision, true);
        logEnd("Do switch to source");

        //Merge in differences from [source:LAST_MERGE, branch:LAST_BRANCH_COMMIT] into the working copy
        logStart("Do merge");
        SVNDiffClient diffClient = _clientManager.getDiffClient();
        diffClient.setDiffGenerator(new DefaultSVNDiffGenerator());
        diffClient.setEventHandler(new SVNCommandEventProcessor(getOut(), System.err, false));
        diffClient.doMerge(sourceURL, lastMergeRevision, branchURL, lastBranchCommitRevision, wcInfo.getRootDir(), true, true, false, false);
        logEnd("Do merge");

        logStart("Revert metadata file");
        //Revert the changes to the metadata file
        SVNWCClient wcClient = _clientManager.getWCClient();
        wcClient.doRevert(wcInfo.getMetadataFile(), false);
        logEnd("Revert metadata file");

        //Commit the changes to the source
        logStart("Commit changes");
        SVNCommitInfo commitInfo = commitClient.doCommit(new File[]{wcInfo.getRootDir()}, false, _commitMessage, false, true);
        logEnd("Commit changes");

        //Delete the user branch
        logStart("Perform Delete");
        commitClient.doDelete(new SVNURL[]{branchURL}, "Deleting branch: " + branchURL);
        logEnd("Perform Delete");

        //Print the new working copy info
        wcInfo = new WorkingCopyInfo(_clientManager);
        getOut().println("");
        getOut().println(wcInfo);
        getOut().println("");
        getOut().println("Promotion Changeset:   [" + commitInfo.getNewRevision() + "]");
    }

    public String getUsageMessage() {
        return _commandLineProcessor.usage("promote");
    }
}
