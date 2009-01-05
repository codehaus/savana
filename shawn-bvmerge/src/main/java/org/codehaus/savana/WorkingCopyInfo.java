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
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;

public class WorkingCopyInfo {
    private File _rootDir;
    private MetadataFile _metadataFile;
    private SVNURL _repositoryUrl;
    private MetadataProperties _metadataProperties;

    public WorkingCopyInfo(SVNClientManager clientManager)
            throws SVNException {
        this(clientManager, new File(System.getProperty("user.dir")));
    }

    public WorkingCopyInfo(SVNClientManager clientManager, File currentDirectory)
            throws SVNException {
        while (currentDirectory != null) {
            MetadataFile metadataFile = new MetadataFile(currentDirectory, MetadataFile.METADATA_FILE_NAME);
            if (!metadataFile.exists()) {
                metadataFile = new MetadataFile(currentDirectory, MetadataFile.METADATA_FILE_NAME_BACKWARD_COMPATIBLE);
            }
            if (metadataFile.exists()) {
                _metadataFile = metadataFile;
                _rootDir = currentDirectory;
                break;
            }

            currentDirectory = currentDirectory.getParentFile();
        }

        if (_metadataFile == null) {
            String errorMessage = "ERROR: Current directory is not part of a working copy.";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.CLIENT_VERSIONED_PATH_REQUIRED, errorMessage), SVNLogType.CLIENT);
        }

        //Load all the metadata properties from the metadata file
        _metadataProperties = new MetadataProperties(clientManager, _metadataFile);

        //Configure whether symbolic links can be stored inside subversion repositories.  Detecting
        //symbolic links can be extremely slow on a non-Windows OS, although using jna.jar helps.
        //It's turned off by default in SVN.java, so if any working copy needs it then enable it
        if (!SVNFileType.isSymlinkSupportEnabled()) {
            SVNFileType.setSymlinkSupportEnabled(_metadataProperties.isVersionedSymlinksSupported());
        }
        
        //Make sure that the actual repository location of the working copy and the location of the .svnscripts file match
        //One way for these to not match is if a promotion script failed after the merge to the source but before the commit
        //In this case, the working copy will be pointed at the source in the repository, but the metadata file will still
        //be the copy from the branch.
        //If this condition ever happens, the scripts should fail since the working copy info will be inaccurate

        //Make sure that the path in the repository matches the path from the metadata file.
        //Get the working copy location according to subversion
        String branchPath = _metadataProperties.getBranchPath();
        SVNWCClient wcClient = clientManager.getWCClient();
        SVNInfo info = wcClient.doInfo(_metadataFile.getParentFile(), SVNRevision.WORKING);
        String pathUrl = info.getURL().toString();
        String actualPath = PathUtil.getPathTail(pathUrl, info.getRepositoryRootURL().toString());
        if (!actualPath.equals(branchPath)) {
            String errorMessage = "ERROR: The working copy does not match the repository location. [actualPath: " + actualPath + "] [branchPath: " + branchPath + "]";
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OP_ON_CWD, errorMessage), SVNLogType.CLIENT);
        }

        //Remember the repository URL
        _repositoryUrl = info.getRepositoryRootURL();
    }

    public File getRootDir() {
        return _rootDir;
    }

    public MetadataFile getMetadataFile() {
        return _metadataFile;
    }

    public MetadataProperties getMetadataProperties() {
        return _metadataProperties;
    }

    public SVNURL getRepositoryURL(String... pathParts) throws SVNException {
        SVNURL url = _repositoryUrl;
        for (String pathPart : pathParts) {
            if (pathPart != null) {
                url = url.appendPath(pathPart, false);
            }
        }
        return url;
    }

    public String toString() {
        return _metadataProperties.toString();
    }
}
