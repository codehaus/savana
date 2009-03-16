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

import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.ListDirEntryHandler;
import org.codehaus.savana.MetadataProperties;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.regex.Pattern;

public class ListBranches extends SAVCommand {

    private final boolean _userBranch;

    public ListBranches(String name, String[] aliases, boolean userBranch) {
        super(name, aliases);
        _userBranch = userBranch;
    }

    @Override
    protected Collection createSupportedOptions() {
        Collection options = new ArrayList();
        options.add(SVNOption.QUIET);
        return options;
    }

    public void doRun() throws SVNException {
        SAVCommandEnvironment env = getSVNEnvironment();

        //Parse command-line arguments
        List<String> targets = env.combineTargets(null, false);
        if (targets.size() > 1) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CL_ARG_PARSING_ERROR), SVNLogType.CLIENT);
        }
        String branchNameFilter = targets.isEmpty() ? "*" : targets.get(0);

        //Get information about the current workspace from the metadata file
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(env.getClientManager());
        MetadataProperties wcProps = wcInfo.getMetadataProperties();

        //Find the source of the branch
        String branchesRootPath = _userBranch ?
                wcProps.getUserBranchPath(null) :
                wcProps.getReleaseBranchPath(null);
        SVNURL branchesRootURL = wcInfo.getRepositoryURL(branchesRootPath);

        //Find all of the directories at that path
        logStart("List branches");
        ListDirEntryHandler listDirEntryHandler = new ListDirEntryHandler(branchesRootURL);
        try {
            env.getClientManager().getLogClient().doList(branchesRootURL, SVNRevision.HEAD, SVNRevision.HEAD, false,
                    SVNDepth.IMMEDIATES, SVNDirEntry.DIRENT_ALL, listDirEntryHandler);
        } catch (SVNException e) {
            //Rethrow with a different error message if the branch root doesn't exist
            if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                String errorMessage =
                        "ERROR: Could not find project." +
                        "\nURL: " + branchesRootURL;
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, errorMessage), SVNLogType.CLIENT);
            }
        }
        logEnd("List branches");

        logStart("Get branch names");
        SortedSet<String> branchNames = listDirEntryHandler.getNames();
        logEnd("Get branch names");

        logStart("Filter branch names");
        //Create a regular expression from the branch name filter
        Pattern branchNamePattern = getBranchNamePattern(branchNameFilter);
        //Get the location of the user and release branches parent
        String userBranchesPath = wcProps.getUserBranchPath(null);
        String releaseBranchesPath = wcProps.getReleaseBranchPath(null);

        for (Iterator<String> it = branchNames.iterator(); it.hasNext();) {
            String branchName = it.next();
            //Remove any branch names that don't match the pattern
            if (!branchNamePattern.matcher(branchName).matches()) {
                it.remove();
                continue;
            }
            //Remove the release and user branches top-level directory, if they're here
            String branchPath = SVNPathUtil.append(branchesRootPath, branchName);
            if (branchPath.equals(releaseBranchesPath) || branchPath.equals(userBranchesPath)) {
                it.remove();
            }
        }
        logEnd("Filter branch names");

        //For each branch name...
        logStart("Print branch info");
        if (env.isQuiet()) {
            for (String branchName : branchNames) {
                env.getOut().println(branchName);
            }
        } else if (branchNames.isEmpty()) {
            env.getOut().println("No branches were found.");
        } else {
            SVNRepository repository = env.getClientManager().createRepository(wcInfo.getRepositoryURL(), false);

            env.getOut().println("------------------------------------------------------------------------------");
            env.getOut().println(
                    pad("Branch Name", 22) + " " +
                    pad("Source", 13) + " " +
                    pad("Branch-Point", 13) + " " +
                    pad("Last-Merge", 13) + " " +
                    pad("Subpath", 0));

            env.getOut().println("------------------------------------------------------------------------------");

            for (String branchName : branchNames) {
                String branchPath = SVNPathUtil.append(branchesRootPath, branchName);

                //Get the metadata file
                String metadataFilePath = SVNPathUtil.append(branchPath, wcProps.getMetadataFileName());
                MetadataProperties metadataFileProperties;
                try {
                    metadataFileProperties = new MetadataProperties(repository, metadataFilePath, -1);
                } catch (SVNException e) {
                    // branch doesn't have a .savana file
                    env.getOut().println(branchName);
                    continue;
                }

                //Print the branch information
                SVNRevision branchPointRevision = metadataFileProperties.getBranchPointRevision();
                SVNRevision lastMergeRevision = metadataFileProperties.getLastMergeRevision();

                env.getOut().println(
                        pad(branchName, 22) + " " +
                        pad(metadataFileProperties.getSourceName(), 13) + " " +
                        pad(branchPointRevision != null ? branchPointRevision.toString() : "", 13) + " " +
                        pad(lastMergeRevision != null ? lastMergeRevision.toString() : "", 13) + " " +
                        pad(metadataFileProperties.getSourceSubpath(), 0));
            }
        }
        logEnd("Print branch info");
    }

    private Pattern getBranchNamePattern(String branchNameFilter) {
        //Convert '*' characters to the '.*' regular expression pattern

        //Split the URI out by the '*' character
        String[] splitURI = branchNameFilter.split("\\*");

        //Escape each part of the URI
        for (int i = 0; i < splitURI.length; i++) {
            if (StringUtils.isNotEmpty(splitURI[i])) {
                splitURI[i] = Pattern.quote(splitURI[i]);
            }
        }

        //Join the parts together using the unescaped '.*' regular expression as the separator
        String pattern = StringUtils.join(splitURI, ".*");

        //If the original string ended in a '*' we need to add an extra '.*' to the end of the pattern
        if (branchNameFilter.endsWith("*")) {
            pattern += ".*";
        }

        //Create a regular expression pattern and map it to the servlet name
        return Pattern.compile(pattern);
    }

    private String pad(String s, int length) {
        if (s == null) {
            s = "";
        }
        StringBuilder buf = new StringBuilder(s);
        while (buf.length() < length) {
            buf.append(' ');
        }
        return buf.toString();
    }
}
