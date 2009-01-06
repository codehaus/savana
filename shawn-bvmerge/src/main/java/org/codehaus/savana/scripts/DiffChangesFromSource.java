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

import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiffChangesFromSource extends SAVCommand {

    public DiffChangesFromSource() {
        super("diffchangesfromsource", new String[]{"diff"});
    }

    protected Collection createSupportedOptions() {
        ArrayList options = new ArrayList();
        options.add(SVNOption.DIFF_CMD);
        options.add(SVNOption.EXTENSIONS);
        options.add(SVNOption.NO_DIFF_DELETED);
        options.add(SVNOption.FORCE);        
        return options;
    }

    public void run() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(null, false);
        if (targets.size() > 0) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }

        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager());
        MetadataProperties wcProps = wcInfo.getMetadataProperties();

        //If there is no source (we are in the trunk)
        if (wcProps.getSourcePath() == null) {
            String errorMessage = "Error: No source path found (you are probably in the TRUNK).";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
        }

        //Create the source URL
        SVNURL sourceURL = wcInfo.getRepositoryURL(wcProps.getSourcePath());

        //Diff [source:HEAD, branch:HEAD] to see what has changes
        logStart("Get Diff Client");
        SVNDiffClient diffClient = env.getClientManager().getDiffClient();
        final String metadataFile = wcInfo.getMetadataFile().getAbsolutePath();
        DefaultSVNDiffGenerator diffGenerator = new DefaultSVNDiffGenerator() {
            @Override
            public void displayPropDiff(String path, SVNProperties baseProps, SVNProperties diff, OutputStream result) throws SVNException {
                // ignore changes in the metadata file
                if (!new File(path).getAbsolutePath().equals(metadataFile)) {
                    super.displayPropDiff(path, baseProps, diff, result);
                }
            }
        };
        if (env.getDiffCommand() != null) {
            diffGenerator.setExternalDiffCommand(env.getDiffCommand());
            diffGenerator.setRawDiffOptions(env.getExtensions());
        } else {
            diffGenerator.setDiffOptions(env.getDiffOptions());
        }
        diffGenerator.setDiffDeleted(!env.isNoDiffDeleted());
        diffGenerator.setForcedBinaryDiff(env.isForce());
        diffGenerator.setBasePath(new File("").getAbsoluteFile());
        diffGenerator.setFallbackToAbsolutePath(true);
        diffGenerator.setOptions(diffClient.getOptions());
        diffClient.setDiffGenerator(diffGenerator);
        logEnd("Get Diff Client");

        logStart("Do Diff");
        diffClient.doDiff(sourceURL, wcProps.getLastMergeRevision(), wcInfo.getRootDir(),
                SVNRevision.WORKING, SVNDepth.INFINITY, false, env.getOut(), null);
        logEnd("Do Diff");
    }
}
