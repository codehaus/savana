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

import org.codehaus.savana.BranchType;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNNotifyPrinter;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Synchronize extends SAVCommand {

    public Synchronize() {
        super("synchronize", new String[]{"sync", "resync"});
    }

    protected Collection createSupportedOptions() {
        return new ArrayList();
    }

    public void run() throws SVNException {
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
                    "ERROR: Merges can only be performed from user branches." +
                    "\nBranch: " + branchURL +
                    "\nBranch Type: " + wcProps.getBranchType();
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }
        logEnd("Check for user branch");

        //Find the revision for the HEAD of the repository
        //Use this version from here on.  If we were to use the keyword HEAD, the revision that it maps to could change
        //That would give us unpredictable results about which versions are being used.
        logStart("Get latest revision");
        SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);
        SVNRevision latestRevision = SVNRevision.create(repository.getLatestRevision());
        logEnd("Get latest revision");

        //Find out if there are any changes in the source that need to be merged in
        logStart("Do info on source");
        SVNWCClient wcClient = env.getClientManager().getWCClient();
        SVNInfo sourceInfo = wcClient.doInfo(sourceURL, latestRevision, latestRevision);
        SVNRevision sourceLastChange = sourceInfo.getCommittedRevision();
        logEnd("Do info on source");

        if (sourceLastChange.getNumber() > wcProps.getLastMergeRevision().getNumber()) {

            //Merge in differences from [source:LastMergeRevision, source:LatestRevision] into the working copy
            logStart("Do merge");
            SVNDiffClient diffClient = env.getClientManager().getDiffClient();
            diffClient.setDiffGenerator(new DefaultSVNDiffGenerator());
            diffClient.setEventHandler(new SVNNotifyPrinter(env));
            diffClient.doMerge(sourceURL, wcProps.getLastMergeRevision(), sourceURL, latestRevision,
                    wcInfo.getRootDir(), SVNDepth.INFINITY, true, false, false, false);
            logEnd("Do merge");

            //Revert the changes to the metadata file
            logStart("Revert metadata file");
            wcClient.doRevert(new File[] {wcInfo.getMetadataFile()}, SVNDepth.EMPTY, null);
            logEnd("Revert metadata file");

            //Update the last merge revision in the metadata file
            logStart("Update last merge revision");
            wcClient.doSetProperty(wcInfo.getMetadataFile(), MetadataFile.PROP_LAST_MERGE_REVISION,
                    SVNPropertyValue.create(Long.toString(latestRevision.getNumber())),
                    false, SVNDepth.EMPTY, null, null);
            logEnd("Update last merge revision");
        } else {
            env.getOut().println("Branch is up to date.");
        }
    }
}