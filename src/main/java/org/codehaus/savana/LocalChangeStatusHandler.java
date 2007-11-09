package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.*;

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
public class LocalChangeStatusHandler implements ISVNStatusHandler, ISVNEventHandler {
    private boolean _changed;

    public LocalChangeStatusHandler() {
        _changed = false;
    }

    public boolean isChanged() {
        return _changed;
    }

    public void handleStatus(SVNStatus status) {

        //Check the status of the file
        SVNStatusType contentsStatus = status.getContentsStatus();
        if (contentsStatus == SVNStatusType.STATUS_MODIFIED ||
            contentsStatus == SVNStatusType.STATUS_CONFLICTED ||
            contentsStatus == SVNStatusType.STATUS_MERGED ||
            contentsStatus == SVNStatusType.STATUS_DELETED ||
            contentsStatus == SVNStatusType.STATUS_ADDED ||
            contentsStatus == SVNStatusType.STATUS_MISSING ||
            contentsStatus == SVNStatusType.STATUS_INCOMPLETE ||
            contentsStatus == SVNStatusType.STATUS_OBSTRUCTED ||
            contentsStatus == SVNStatusType.STATUS_REPLACED) {

            _changed = true;
        }

        //Check the status of the file's properties
        SVNStatusType propertiesStatus = status.getPropertiesStatus();
        if (propertiesStatus == SVNStatusType.STATUS_MODIFIED ||
            propertiesStatus == SVNStatusType.STATUS_CONFLICTED) {

            _changed = true;
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
    public void checkCancelled() throws SVNCancelException {
    }
}
