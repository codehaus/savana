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
import org.codehaus.savana.PathUtil;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListWorkingCopyInfo extends SAVCommand {

    public ListWorkingCopyInfo() {
        super("listworkingcopyinfo", new String[]{"wci", "info"});
    }

    @Override
    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options.add(SVNOption.RECURSIVE);
        options.add(SVNOption.TARGETS);
        options.add(SVNOption.DEPTH);
        return options;
    }

    @Override
    protected boolean getWorkingCopyFormatFromCurrentDirectory() {
        return false; // set the working copy format separately for each target
    }

    public void doRun() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(env.getTargets(), false);
        if (targets.isEmpty()) {
            targets = Arrays.asList("");
        }

        boolean first = true;
        for (String target : targets) {
            File targetDir = PathUtil.getValidatedAbsoluteFile(target);

            //Allow running "sav info" on multiple directories that may be from a mix of old and new versions of svn
            configureWorkingCopyFormat(targetDir);

            //Print a blank line between workspaces
            if (first) {
                first = false;
            } else {
                env.getOut().println();
            }

            //Print information about the current workspace from the metadata file
            WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager(), targetDir);
            wcInfo.println(env.getOut());

            //If the recursive flag was specified, print info for workspaces underneath this one
            if (env.getDepth() != SVNDepth.UNKNOWN) {
                printNestedWorkspaces(wcInfo);

            }
        }
    }

    private void printNestedWorkspaces(WorkingCopyInfo wcInfo) throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        // look for all subdirectories that are switched relative to the working copy root
        SwitchedDirectoriesHandler statusHandler = new SwitchedDirectoriesHandler();
        SVNStatusClient statusClient = env.getClientManager().getStatusClient();
        statusClient.doStatus(wcInfo.getRootDir(), SVNRevision.HEAD, env.getDepth(),
                false, true, false, false, statusHandler, null);
        List<File> switchedDirectories = statusHandler.getSwitchedDirectories();

        Collections.sort(switchedDirectories);
        for (File switchedDirectory : switchedDirectories) {
            // if a switched directory contains a Savana metadata file, then it's a workspace.  print out its info.
            File switchedMetadataFile = new File(switchedDirectory, wcInfo.getMetadataProperties().getMetadataFileName());
            if (switchedMetadataFile.exists()) {
                MetadataProperties switchedProperties =
                        new MetadataProperties(env.getClientManager(), switchedMetadataFile);
                env.getOut().println();
                env.getOut().println(switchedDirectory + ":");
                env.getOut().println(switchedProperties);
            }
        }
    }

    private static class SwitchedDirectoriesHandler implements ISVNStatusHandler {
        private final List<File> _switchedDirectories = new ArrayList<File>();

        public List<File> getSwitchedDirectories() {
            return _switchedDirectories;
        }

        public void handleStatus(SVNStatus status) throws SVNException {
            if (status.isSwitched() && status.getKind() == SVNNodeKind.DIR) {
                _switchedDirectories.add(status.getFile());
            }
        }
    }
}
