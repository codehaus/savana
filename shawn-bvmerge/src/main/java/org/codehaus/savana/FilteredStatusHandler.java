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
 * @author Mykola Tunyk
 */
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.util.ArrayList;
import java.util.Collection;

public class FilteredStatusHandler implements ISVNStatusHandler, ISVNEventHandler {
    private final Collection<String> _entries;
    private final SVNStatusType _status;

    public FilteredStatusHandler(SVNStatusType status) {
        _status = status;
        _entries = new ArrayList<String>();
    }

    public Collection<String> getEntries() {
        return _entries;
    }

    public void handleStatus(SVNStatus status) {

        //Status of the file
        SVNStatusType contentsStatus = status.getContentsStatus();

        //Status of the file's properties
        SVNStatusType propertiesStatus = status.getPropertiesStatus();

        //Collect information about entries with specified status
        if ((contentsStatus == _status) || (propertiesStatus == _status)) {
            _entries.add(status.getURL().getPath());
        }
    }

    /*
     * This is an implementation for
     * ISVNEventHandler.handleEvent(SVNEvent event, double progress)
     */
    public void handleEvent(SVNEvent event, double progress) {
    }

    /*
     * Should be implemented to check if the current operation is cancelled. If
     * it is, this method should throw an SVNCancelException.
     */
    public void checkCancelled()
            throws SVNCancelException {
    }
}