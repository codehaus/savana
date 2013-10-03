/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006-2013  Bazaarvoice Inc.
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
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SVNEditorHelper {

    private ISVNEditor _editor;
    private Set<String> _openDirs;
    private Set<String> _openFiles;

    public SVNEditorHelper(ISVNEditor editor) {
        _editor = editor;
        _openDirs = new HashSet<String>();
        _openFiles = new HashSet<String>();

        //Add an open directory for the root
        _openDirs.add("");
    }

    public void openDir(String dirPath)
            throws SVNException {
        List<String> subpaths = PathUtil.getAllSubpaths(dirPath);

        //Open all the dirs
        for (String dir : subpaths) {
            if (!_openDirs.contains(dir)) {
                _editor.openDir(dir, -1);
                _openDirs.add(dir);
            }
        }
    }

    public void addOpenedDir(String dirPath) {
        _openDirs.add(dirPath);
    }

    public void openFile(String filePath)
            throws SVNException {
        //If the file isnt' already open
        if (!_openFiles.contains(filePath)) {
            String dirPath = SVNPathUtil.removeTail(filePath);
            openDir(dirPath);

            _editor.openFile(filePath, -1);
            _openFiles.add(filePath);
        }
    }

    public void addOpenedFile(String filePath) {
        _openFiles.add(filePath);
    }

    public SVNCommitInfo closeAll()
            throws SVNException {
        for (String filePath : _openFiles) {
            _editor.closeFile(filePath, null);
        }

        for (int i = 0; i < _openDirs.size(); i++) {
            _editor.closeDir();
        }

        //Close the editor
        return _editor.closeEdit();
    }
}
