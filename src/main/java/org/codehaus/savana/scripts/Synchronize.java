package org.codehaus.savana.scripts;

import org.codehaus.savana.ChangeLogEntryHandler;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.command.SVNCommandEventProcessor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.*;

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
public class Synchronize extends SVNScript {
    public Synchronize()
            throws SVNException, SVNScriptException {}

    public void initialize(String[] args) {}

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

        //Find the revision for the HEAD of the repository
        //Use this version from here on.  If we were to use the keyword HEAD, the revision that it maps to could change
        //That would give us unpredictable results about which versions are being used.
        logStart("Get latest revision");
        SVNRevision latestRevision = SVNRevision.create(_repository.getLatestRevision());
        logEnd("Get latest revision");

        //Determine if there have been any changes in the source from [source:LastMergeRevision -> source:LatestRevision]
        //We can't use the revision numbers to figure this out because commits in branches other than the source
        //will cause the revision numbers to be bumped even though nothing changed in the source.
        //Therefore, we will detect the need to merge by doing a log on the source and looking for any commits
        logStart("Do log");
        SVNLogClient logClient = _clientManager.getLogClient();
        ChangeLogEntryHandler logEntryHandler = new ChangeLogEntryHandler();
        logClient.doLog(sourceURL, new String[]{""}, wcInfo.getLastMergeRevision(), wcInfo.getLastMergeRevision(), latestRevision, false, false, 1, logEntryHandler);
        logEnd("Do log");

        if (logEntryHandler.isChanged()) {

            //Merge in differences from [source:LastMergeRevision, source:LatestRevision] into the working copy
            logStart("Do merge");
            SVNDiffClient diffClient = _clientManager.getDiffClient();
            diffClient.setDiffGenerator(new DefaultSVNDiffGenerator());
            diffClient.setEventHandler(new SVNCommandEventProcessor(getOut(), System.err, false));
            diffClient.doMerge(sourceURL, wcInfo.getLastMergeRevision(), sourceURL, latestRevision, wcInfo.getRootDir(), true, true, false, false);
            logEnd("Do merge");

            //Revert the changes to the metadata file
            logStart("Revert metadata file");
            SVNWCClient wcClient = _clientManager.getWCClient();
            wcClient.doRevert(wcInfo.getMetadataFile(), false);
            logEnd("Revert metadata file");

            //Update the last merge revision in the metadata file
            logStart("Update last merge revision");
            wcClient.doSetProperty(wcInfo.getMetadataFile(), MetadataFile.PROP_LAST_MERGE_REVISION, Long.toString(latestRevision.getNumber()), false, false, null);
            logEnd("Update last merge revision");
        } else {
            getOut().println("Branch is up to date.");
        }
    }

    public String getUsageMessage() {
        return "Usage: ss sync";
    }
}
