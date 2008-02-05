package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
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
public class WorkingCopyInfo {
    private File _rootDir;
    private MetadataFile _metadataFile;
    private String _projectName;
    private String _branchPath;
    private String _sourcePath;
    private String _branchType;
    private String _projectRoot = null;
    private String _trunkPath = MetadataFile.DEFAULT_TRUNK_PATH;
    private String _branchesPath = MetadataFile.DEFAULT_BRANCHES_PATH;
    private String _userBranchesPath = MetadataFile.DEFAULT_USER_BRANCHES_PATH;
    private SVNVersion _svnVersion = SVNVersion.SVN_1_4;

    private SVNRevision _branchPointRevision;
    private SVNRevision _lastMergeRevision;
    private SVNRevision _lastPromoteRevision;

    public WorkingCopyInfo(SVNClientManager clientManager)
            throws SVNException, SVNScriptException {
        this(clientManager, new File(System.getProperty("user.dir")));
    }

    public WorkingCopyInfo(SVNClientManager clientManager, File currentDirectory)
            throws SVNException, SVNScriptException {
        while (currentDirectory != null) {
            MetadataFile metadataFile = new MetadataFile(currentDirectory, MetadataFile.METADATA_FILE_NAME);
            if (metadataFile.exists()) {
                _metadataFile = metadataFile;
                _rootDir = currentDirectory;
                break;
            }

            currentDirectory = currentDirectory.getParentFile();
        }

        if (_metadataFile == null) {
            throw new SVNScriptException("ERROR: Current directory is not part of a working copy.");
        }

        //Find the branch path from the metadata file
        SVNWCClient wcClient = clientManager.getWCClient();
        SVNPropertyData projectNameProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_PROJECT_NAME, null, SVNRevision.WORKING, false);
        SVNPropertyData branchPathProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_BRANCH_PATH, null, SVNRevision.WORKING, false);
        SVNPropertyData sourcePathProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_SOURCE_PATH, null, SVNRevision.WORKING, false);
        SVNPropertyData branchTypeProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_BRANCH_TYPE, null, SVNRevision.WORKING, false);
        SVNPropertyData projectRootProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_PROJECT_ROOT, null, SVNRevision.WORKING, false);
        SVNPropertyData trunkPathProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_TRUNK_PATH, null, SVNRevision.WORKING, false);
        SVNPropertyData branchesPathProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_BRANCHES_PATH, null, SVNRevision.WORKING, false);
        SVNPropertyData userBranchesPathProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_USER_BRANCHES_PATH, null, SVNRevision.WORKING, false);
        SVNPropertyData branchPointRevisionProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_BRANCH_POINT_REVISION, null, SVNRevision.WORKING, false);
        SVNPropertyData lastMergeRevisionProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_LAST_MERGE_REVISION, null, SVNRevision.WORKING, false);
        SVNPropertyData lastPromoteRevisionProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_LAST_PROMOTE_REVISION, null, SVNRevision.WORKING, false);
        SVNPropertyData svnVersionProps = wcClient.doGetProperty(_metadataFile, MetadataFile.PROP_SVN_VERSION, null, SVNRevision.WORKING, false);

        if (projectNameProps != null) {
            _projectName = _projectRoot = projectNameProps.getValue();
        }

        if (branchPathProps != null) {
            _branchPath = branchPathProps.getValue();
        }

        if (sourcePathProps != null) {
            _sourcePath = sourcePathProps.getValue();
        }

        if (branchTypeProps != null) {
            _branchType = branchTypeProps.getValue();
        }

        if (projectRootProps != null) {
            _projectRoot = projectRootProps.getValue();
        }

        if (trunkPathProps != null) {
            _trunkPath = trunkPathProps.getValue();
        }

        if (branchesPathProps != null) {
            _branchesPath = branchesPathProps.getValue();
        }

        if (userBranchesPathProps != null) {
            _userBranchesPath = userBranchesPathProps.getValue();
        }

        if (branchPointRevisionProps != null) {
            _branchPointRevision = SVNRevision.create(Long.parseLong(branchPointRevisionProps.getValue()));
        }

        if (lastMergeRevisionProps != null) {
            _lastMergeRevision = SVNRevision.create(Long.parseLong(lastMergeRevisionProps.getValue()));
        }

        if (lastPromoteRevisionProps != null) {
            _lastPromoteRevision = SVNRevision.create(Long.parseLong(lastPromoteRevisionProps.getValue()));
        }

        if (svnVersionProps != null) {
            _svnVersion = SVNVersion.valueOf(svnVersionProps.getValue());
        }
    }

    public File getRootDir() {
        return _rootDir;
    }

    public MetadataFile getMetadataFile() {
        return _metadataFile;
    }

    public String getProjectName() {
        return _projectName;
    }

    public String getBranchPath() {
        return _branchPath;
    }

    public String getProjectRoot() {
        return _projectRoot;
    }

    public String getTrunkPath() {
        return _trunkPath;
    }

    public String getBranchesPath() {
        return _branchesPath;
    }

    public String getUserBranchesPath() {
        return _userBranchesPath;
    }

    public SVNVersion getSVNVersion() {
        return _svnVersion;
    }

    public String getBranchName() {
        if (_branchPath == null) {
            return null;
        } else {
            return SVNPathUtil.tail(_branchPath);
        }
    }

    public String getSourcePath() {
        return _sourcePath;
    }

    public String getSourceName() {
        if (_sourcePath == null) {
            return null;
        } else {
            return SVNPathUtil.tail(_sourcePath);
        }
    }

    public String getBranchType() {
        return _branchType;
    }

    public SVNRevision getBranchPointRevision() {
        return _branchPointRevision;
    }

    public SVNRevision getLastMergeRevision() {
        return _lastMergeRevision;
    }

    public SVNRevision getLastPromoteRevision() {
        return _lastPromoteRevision;
    }

    public String toString() {
        return
                "---------------------------------------------" +
                "\nBranch Name:           " + getBranchName() +
                "\n---------------------------------------------" +
                "\nProject Name:          " + _projectName +
                "\nBranch Type:           " + _branchType.toLowerCase() +
                "\nSource:                " + ((getSourceName() != null) ? getSourceName() : "none") +
                "\nBranch Point Revision: " + ((_branchPointRevision != null) ? _branchPointRevision : "none") +
                "\nLast Merge Revision:   " + ((_lastMergeRevision != null) ? _lastMergeRevision : "none");
    }

    public String getReleaseBranchPath(String branchName) {
        return SVNPathUtil.append(SVNPathUtil.append(getProjectRoot(), getBranchesPath()), branchName);
    }

    public String getUserBranchPath(String branchName) {
        return SVNPathUtil.append(SVNPathUtil.append(getProjectRoot(), getUserBranchesPath()), branchName);
    }
}
