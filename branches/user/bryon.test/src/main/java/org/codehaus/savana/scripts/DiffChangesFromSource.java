package org.codehaus.savana.scripts;

import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

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
public class DiffChangesFromSource extends SVNScript {
    public DiffChangesFromSource()
            throws SVNException, SVNScriptException {}

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //If there is no source (we are in the trunk)
        if (wcInfo.getSourcePath() == null) {
            String errorMessage = "Error: No source path found.";
            throw new SVNScriptException(errorMessage);
        }

        //Create the source URL
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL sourceURL = repositoryURL.appendPath(wcInfo.getSourcePath(), false);

        //Diff [source:HEAD, branch:HEAD] to see what has changes
        logStart("Get Diff Client");
        SVNDiffClient diffClient = _clientManager.getDiffClient();
        diffClient.setDiffGenerator(new DefaultSVNDiffGenerator());
        logEnd("Get Diff Client");

        logStart("Do Diff");
        diffClient.doDiff(sourceURL, wcInfo.getLastMergeRevision(), wcInfo.getRootDir(), SVNRevision.WORKING, true, false, getOut());
        logEnd("Do Diff");
    }

    public String getUsageMessage() {
        return _commandLineProcessor.usage("diff");
    }
}
