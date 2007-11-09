package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;

import java.io.File;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Map;
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
public class FileListDiffGenerator extends DefaultSVNDiffGenerator {
    private Set<String> _changedFilePaths;
    private Set<String> _addedFilePaths;
    private Set<String> _deletedFilePaths;

    public FileListDiffGenerator() {
        _changedFilePaths = new LinkedHashSet<String>();
        _addedFilePaths = new LinkedHashSet<String>();
        _deletedFilePaths = new LinkedHashSet<String>();
    }

    public Set<String> getChangedFilePaths() {
        return _changedFilePaths;
    }

    public Set<String> getAddedFilePaths() {
        return _addedFilePaths;
    }

    public Set<String> getDeletedFilePaths() {
        return _deletedFilePaths;
    }

    public void displayPropDiff(String path, Map baseProps, Map diff, OutputStream result)
            throws SVNException {
        if (!_addedFilePaths.contains(path) && !_deletedFilePaths.contains(path)) {
            _changedFilePaths.add(path);
        }
    }

    public void displayFileDiff(String path, File file1, File file2, String rev1, String rev2, String mimeType1, String mimeType2, OutputStream result)
            throws SVNException {
        if (file1 == null && file2 != null) {
            _addedFilePaths.add(path);
            _changedFilePaths.remove(path);
        } else if (file1 != null && file2 == null) {
            _deletedFilePaths.add(path);
            _changedFilePaths.remove(path);
        }

        if (!_addedFilePaths.contains(path) && !_deletedFilePaths.contains(path)) {
            _changedFilePaths.add(path);
        }
    }
}
