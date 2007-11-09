package org.codehaus.savana.scripts;

import org.codehaus.savana.FileListDiffGenerator;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.util.Set;

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
public class ListChangesFromSource extends SVNScript {
    public ListChangesFromSource()
            throws SVNException, SVNScriptException {}

    public void initialize(String[] args) {}

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //If there is no source (we are in the trunk)
        if (wcInfo.getSourcePath() == null) {
            String errorMessage = "Error: No source path found (you are probably in the TRUNK).";
            throw new SVNScriptException(errorMessage);
        }

        //Create the source URL
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL sourceURL = repositoryURL.appendPath(wcInfo.getSourcePath(), false);

        //Diff [source:HEAD, branch:HEAD] to see what has changes
        logStart("Get Diff Client");
        SVNDiffClient diffClient = _clientManager.getDiffClient();
        FileListDiffGenerator diffGenerator = new FileListDiffGenerator();
        diffClient.setDiffGenerator(diffGenerator);
        logEnd("Get Diff Client");
        logStart("Do Diff");
        diffClient.doDiff(sourceURL, wcInfo.getLastMergeRevision(), wcInfo.getRootDir(), SVNRevision.WORKING, true, false, getOut());
        logEnd("Do Diff");

        String workingCopyPath = wcInfo.getRootDir().getAbsolutePath();
        printFileList("Added Files:", diffGenerator.getAddedFilePaths(), workingCopyPath);
        printFileList("Modified Files:", diffGenerator.getChangedFilePaths(), workingCopyPath);
        printFileList("Deleted Files:", diffGenerator.getDeletedFilePaths(), workingCopyPath);
    }

    private void printFileList(String title, Set<String> paths, String workingCopyPath) {
        if (!paths.isEmpty()) {
            getOut().println(title);
            getOut().println("-------------------------------------------------");
            for (String path : paths) {
                if (!workingCopyPath.equals(path)) {
                    getOut().println(path.substring(workingCopyPath.length() + 1));
                }
            }
            getOut().println("");
        }
    }

    public String getUsageMessage() {
        return
                "Usage: ss listchangesfromsource";
    }
}
