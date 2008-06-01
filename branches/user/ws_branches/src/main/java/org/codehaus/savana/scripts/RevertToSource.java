package org.codehaus.savana.scripts;

import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.util.cli.CommandLineProcessor;
import org.codehaus.savana.util.cli.SavanaArgument;
import org.tmatesoft.svn.cli.command.SVNCommandEventProcessor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
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
public class RevertToSource extends SVNScript {
    private static final SavanaArgument PATH = new SavanaArgument(
            "path", "the path to be reverted");

    private File _path;

    public RevertToSource()
            throws SVNException, SVNScriptException {}

    public CommandLineProcessor constructCommandLineProcessor() {
        return new CommandLineProcessor(
                new CommandLineProcessor.ArgumentHandler(PATH) {
                    public void handle(String arg) {
                        _path = new File(arg);
                    }
                });
    }

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //If there is no source (we are in the trunk)
        if (wcInfo.getSourcePath() == null) {
            String errorMessage = "Error: No source path found.";
            throw new SVNScriptException(errorMessage);
        }

        logStart("Build paths");

        //Find the relative file path from the working copy root
        String relativePath = _path.getAbsolutePath().substring(wcInfo.getRootDir().getAbsolutePath().length());
        relativePath = SVNPathUtil.validateFilePath(relativePath);

        //Find the relative path of the file in both the source and branch
        String relativeSourcePath = SVNPathUtil.append(wcInfo.getSourcePath(), relativePath);
        String relativeBranchPath = SVNPathUtil.append(wcInfo.getBranchPath(), relativePath);

        //Create the source URL
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL sourceURL = repositoryURL.appendPath(relativeSourcePath, false);

        logEnd("Build paths");

        //Determine if the file exists in the source and in the branch
        //NOTE: It might exist in the branch even if it doesn't exist in the working copy
        logStart("Check branch exists");
        boolean branchExists = (_repository.checkPath(relativeBranchPath, -1) != SVNNodeKind.NONE);
        logEnd("Check branch exists");
        logStart("Check source exists");
        boolean sourceExists = (_repository.checkPath(relativeSourcePath, wcInfo.getLastMergeRevision().getNumber()) != SVNNodeKind.NONE);
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
            SVNWCClient wcClient = _clientManager.getWCClient();
            wcClient.doRevert(_path, false);
            logEnd("Do Revert");
        }
        catch (SVNException e) {
            //Ignore: see comment above
        }

        if (sourceExists && branchExists) {
            //Merge in differences from [last merge revision, head] into the working copy
            logStart("Do merge");
            SVNDiffClient diffClient = _clientManager.getDiffClient();
            diffClient.setEventHandler(new SVNCommandEventProcessor(getOut(), System.err, false));
            diffClient.doMerge(_path, SVNRevision.WORKING, sourceURL, wcInfo.getLastMergeRevision(), _path, false, false, false, false);
            logEnd("Do merge");
        } else if (sourceExists && !branchExists) {
            //Copy the file from the source to the branch
            logStart("Do Copy");
            SVNCopyClient copyClient = _clientManager.getCopyClient();
            copyClient.doCopy(sourceURL, wcInfo.getLastMergeRevision(), _path.getAbsoluteFile());
            getOut().println("A    " + _path);
            logEnd("Do Copy");
        } else if (!sourceExists && branchExists) {
            //Delete the file from the branch
            logStart("Do Delete");
            SVNWCClient wcClient = _clientManager.getWCClient();
            wcClient.doDelete(_path, false, false, false);
            getOut().println("D    " + _path);
            logEnd("Do Delete");
        } else if (!sourceExists && !branchExists) {
            //No need to do anything since the file isn't in the source or the branch
            logStart("Do Skip");
            getOut().println("Skipped " + _path);
            logEnd("Do Skip");
        }
    }

    public String getUsageMessage() {
        return _commandLineProcessor.usage("revert");
    }
}
