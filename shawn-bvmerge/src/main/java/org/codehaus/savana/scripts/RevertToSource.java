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

import org.codehaus.savana.MetadataProperties;
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
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RevertToSource extends SAVCommand {

    public RevertToSource() {
        super("reverttosource", new String[]{"rs", "revert"});
    }

    @Override
    protected Collection createSupportedOptions() {
        Collection options = new LinkedList();
        options.add(SVNOption.TARGETS);
        return options;
    }

    public void doRun() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(env.getTargets(), true);
        if (targets.isEmpty()) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_INSUFFICIENT_ARGS), SVNLogType.CLIENT);
        }

        //Get information about the current workspace from the metadata file
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager());
        MetadataProperties wcProps = wcInfo.getMetadataProperties();
        SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);

        //If there is no source (we are in the trunk)
        if (wcProps.getSourcePath() == null) {
            String errorMessage = "Error: No source path found (you are probably in the TRUNK).";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }

        //For each target path...
        for (String target : targets) {
            File path = new File(target);

            logStart("Build paths for " + path);

            //Find the relative file path from the working copy root
            String relativePath = path.getAbsolutePath().substring(wcInfo.getRootDir().getAbsolutePath().length());
            relativePath = SVNPathUtil.validateFilePath(relativePath);

            //Find the relative path of the file in both the source and branch
            String relativeSourcePath = SVNPathUtil.append(wcProps.getSourcePath(), relativePath);
            String relativeBranchPath = SVNPathUtil.append(wcProps.getBranchPath(), relativePath);

            //Create the source URL
            SVNURL sourceURL = wcInfo.getRepositoryURL(relativeSourcePath);

            logEnd("Build paths for " + path);

            //Determine if the file exists in the source and in the branch
            //NOTE: It might exist in the branch even if it doesn't exist in the working copy
            logStart("Check branch exists");
            boolean branchExists = (repository.checkPath(relativeBranchPath, -1) != SVNNodeKind.NONE);
            logEnd("Check branch exists");
            logStart("Check source exists");
            boolean sourceExists = (repository.checkPath(relativeSourcePath, wcProps.getLastMergeRevision().getNumber()) != SVNNodeKind.NONE);
            logEnd("Check source exists");
            logStart("Check source exists");

            //Try to revert the working file.
            //We need to run revert for all of these states
            //1. In HEAD, in WORKING
            //2. In HEAD, not in WORKING (svn delete)
            //3. In HEAD, not in WORKING (manual delete)
            //4. Not in HEAD, in WORKING (svn add)

            //We don't need to run revert for the following cases
            //5. Not in HEAD, in working (manual create, but not svn add'ed)
            //6. Not in HEAD, not in WORKING

            //Unfortunately, there is no easy way to distinguish between cases #4 and #5.  Since the revert command will throw
            //an exception for case #5, we will just always run the revert and catch and ignore any exceptions
            try {
                //Revert any changes to the working copy
                logStart("Do revert");
                SVNWCClient wcClient = env.getClientManager().getWCClient();
                wcClient.doRevert(new File[] {path}, SVNDepth.EMPTY, null);
                logEnd("Do Revert");
            }
            catch (SVNException e) {
                //Ignore: see comment above
            }

            if (sourceExists && branchExists) {
                //Merge in differences from [last merge revision, head] into the working copy
                logStart("Do merge");
                SVNDiffClient diffClient = env.getClientManager().getDiffClient();
                diffClient.setEventHandler(new SVNNotifyPrinter(env));
                diffClient.doMerge(path, SVNRevision.HEAD, sourceURL, wcProps.getLastMergeRevision(), path,
                        SVNDepth.FILES, false, false, false, false);
                logEnd("Do merge");
            } else if (sourceExists && !branchExists) {
                //Copy the file from the source to the branch
                logStart("Do Copy");
                SVNCopyClient copyClient = env.getClientManager().getCopyClient();
                copyClient.doCopy(
                        new SVNCopySource[] {
                                new SVNCopySource(wcProps.getLastMergeRevision(), wcProps.getLastMergeRevision(), sourceURL)
                        }, path.getAbsoluteFile(), false, false, false);
                env.getOut().println("A    " + path);
                logEnd("Do Copy");
            } else if (!sourceExists && branchExists) {
                //Delete the file from the branch
                logStart("Do Delete");
                SVNWCClient wcClient = env.getClientManager().getWCClient();
                wcClient.doDelete(path, false, false, false);
                env.getOut().println("D    " + path);
                logEnd("Do Delete");
            } else if (!sourceExists && !branchExists) {
                //No need to do anything since the file isn't in the source or the branch
                logStart("Do Skip");
                env.getOut().println("Skipped " + path);
                logEnd("Do Skip");
            }
        }
    }
}
