package org.codehaus.savana.scripts;

import org.apache.commons.lang.StringUtils;
import org.codehaus.savana.ListDirEntryHandler;
import org.codehaus.savana.MetadataFile;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;

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
public class ListBranches extends SVNScript {
    private String _projectName;
    private String _branchNameFilter;
    private boolean _userBranch;

    public ListBranches()
            throws SVNException, SVNScriptException {
        this(false);
    }

    public ListBranches(boolean userBranch)
            throws SVNException, SVNScriptException {
        _userBranch = userBranch;
    }

    public void initialize(String[] args)
            throws IllegalArgumentException {
        for (String arg : args) {
            if (_projectName == null) {
                _projectName = arg;
                continue;
            }

            if (_branchNameFilter == null) {
                _branchNameFilter = arg;
                continue;
            }

            //If extra parameters were specified
            throw new IllegalArgumentException();
        }

        //Make sure project and branch names aren't null
        if (_projectName == null) {
            _projectName = "";
            _sLog.warn("no project name given - assuming that repository contains one project at /");
        }

        //Find everything if no filter was provided
        if (_branchNameFilter == null) {
            _branchNameFilter = "*";
        }
    }

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);

        //Find the source of the branch
        String branchesRootPath = (_userBranch) ?
                                  wcInfo.getUserBranchPath(null) :
                                  wcInfo.getReleaseBranchPath(null);

        //Make sure the branch exists
        logStart("Check if path exists");
        if (_repository.checkPath(_repository.getRepositoryPath(branchesRootPath), -1) != SVNNodeKind.DIR) {
            String errorMessage =
                    "ERROR: Could not find project." +
                    "\nProject: " + _projectName;
            throw new SVNScriptException(errorMessage);
        }
        logEnd("Check if branch exists");

        //Find all of the directories at that path
        logStart("List branches");
        SVNURL repositoryURL = getRepositoryURL();
        SVNURL branchesRootURL = repositoryURL.appendPath(branchesRootPath, false);
        ListDirEntryHandler listDirEntryHandler = new ListDirEntryHandler();
        _clientManager.getLogClient().doList(branchesRootURL, SVNRevision.HEAD, SVNRevision.HEAD, false, listDirEntryHandler);
        logEnd("List branches");


        logStart("Get branch names");
        SortedSet<String> branchNames = listDirEntryHandler.getNames();
        logEnd("Get branch names");

        logStart("Filter branch names");
        //Create a regular expression from the branch name filter
        Pattern branchNamePattern = getBranchNamePattern(_branchNameFilter);

        //Remove any branch names that don't match the pattern
        for (Iterator it = branchNames.iterator(); it.hasNext();) {
            String branchName = (String) it.next();

            if (!branchNamePattern.matcher(branchName).matches()) {
                it.remove();
            }
        }
        logEnd("Filter branch names");

        //For each branch name
        logStart("Print branch info");
        if (branchNames.isEmpty()) {
            getOut().println("No branches were found.");
        } else {
            getOut().println("-----------------------------------------------------------------------------");
            getOut().println(
                    pad("Branch Name", 20) +
                    pad("Source", 15) +
                    pad("Branch-Point", 15) +
                    pad("Last-Merge", 15) +
                    pad("Last-Promote", 0));

            getOut().println("-----------------------------------------------------------------------------");

            for (String branchName : branchNames) {
                logStart("Get branch metadata info");
                //Get the metadata file
                String branchPath = SVNPathUtil.append(branchesRootPath, branchName);
                String metadataFilePath = SVNPathUtil.append(branchPath, MetadataFile.METADATA_FILE_NAME);
                Map metadataFileProperties = new HashMap();
                _repository.getFile(metadataFilePath, -1, metadataFileProperties, null);
                logEnd("Get branch metadata info");

                //Print the branch information
                logStart("Print branch metadata info");
                String sourcePath = (String) metadataFileProperties.get(MetadataFile.PROP_SOURCE_PATH);
                String branchPointRevision = (String) metadataFileProperties.get(MetadataFile.PROP_BRANCH_POINT_REVISION);
                String lastMergeRevision = (String) metadataFileProperties.get(MetadataFile.PROP_LAST_MERGE_REVISION);
                String lastPromoteRevision = (String) metadataFileProperties.get(MetadataFile.PROP_LAST_PROMOTE_REVISION);

                getOut().println(
                        pad(branchName, 20) +
                        pad(SVNPathUtil.tail(sourcePath), 15) +
                        pad(branchPointRevision, 15) +
                        pad(lastMergeRevision, 15) +
                        pad(lastPromoteRevision, 0));
                logEnd("Print branch metadata info");
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

        while (s.length() < length) {
            s = s + " ";
        }
        return s;
    }

    public String getUsageMessage() {
        return
                "Usage: ss listbranches (<project name>)" +
                "\n  project name:  name of the project" +
                "\nif no project name is given, this script assumes that there is one project in" +
                "\nthe repository, rooted at \"/\"";
    }
}
