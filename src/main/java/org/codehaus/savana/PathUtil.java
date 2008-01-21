package org.codehaus.savana;

import org.tmatesoft.svn.core.internal.util.SVNPathUtil;

import java.util.LinkedList;
import java.util.List;

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
public class PathUtil {
    public static List<String> getAllSubpaths(String path) {
        LinkedList<String> subPaths = new LinkedList<String>();

        //Find all subpaths in reverse order
        while (path != null && path.length() > 0) {
            subPaths.addFirst(path);
            path = SVNPathUtil.removeTail(path);
        }

        return subPaths;
    }
}