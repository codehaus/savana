package org.codehaus.savana.scripts;

import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.WorkingCopyInfo;
import org.tmatesoft.svn.core.SVNException;

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
public class ListWorkingCopyInfo extends SVNScript {
    public ListWorkingCopyInfo()
            throws SVNException, SVNScriptException {}

    public void initialize(String[] args) {}

    public void run()
            throws SVNException, SVNScriptException {
        WorkingCopyInfo wcInfo = new WorkingCopyInfo(_clientManager);
        getOut().println(wcInfo);
    }

    public String getUsageMessage() {
        return "Usage: ss listworkingcopyinfo";
    }
}
