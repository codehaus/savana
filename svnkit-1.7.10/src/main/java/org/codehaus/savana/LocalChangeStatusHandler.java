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
 * @author Shawn Smith (shawn@bazaarvoice.com)
 */
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.io.File;

import static org.tmatesoft.svn.core.wc.SVNStatusType.MERGED;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_ADDED;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_CONFLICTED;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_DELETED;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_INCOMPLETE;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_MISSING;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_MODIFIED;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_OBSTRUCTED;
import static org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_REPLACED;

public class LocalChangeStatusHandler implements ISVNStatusHandler, ISVNEventHandler {
    private final File _wcRoot;
    private boolean _changed;
    private boolean _switched;
    private boolean _outOfDate;

    public LocalChangeStatusHandler() {
        this(null);
    }

    public LocalChangeStatusHandler(File wcRoot) {
        _wcRoot = wcRoot;
    }

    public boolean isChanged() {
        return _changed;
    }

    public boolean isSwitched() {
        return _switched;
    }

    public boolean isOutOfDate() {
        return _outOfDate;
    }

    public void handleStatus(SVNStatus status) {
        //Check the status of the file and its properties
        if (isChanged(status.getCombinedNodeAndContentsStatus()) || isChanged(status.getPropertiesStatus())) {
            _changed = true;
        }

        //Check whether the file belongs to a different branch
        if (status.isSwitched() && !status.getFile().equals(_wcRoot)) {
            _switched = true;
        }

        //Check whether there are updates that haven't been pulled down by an 'svn update'
        if (isChanged(status.getCombinedRemoteNodeAndContentsStatus()) || isChanged(status.getRemotePropertiesStatus())) {
            _outOfDate = true;
        }
    }

    private boolean isChanged(SVNStatusType contentsStatus) {
        // TODO: make sure we are covering all possible status values
        return STATUS_MODIFIED.equals(contentsStatus) ||
            STATUS_CONFLICTED.equals(contentsStatus) ||
            MERGED.equals(contentsStatus) ||
            STATUS_DELETED.equals(contentsStatus) ||
            STATUS_ADDED.equals(contentsStatus) ||
            STATUS_MISSING.equals(contentsStatus) ||
            STATUS_INCOMPLETE.equals(contentsStatus) ||
            STATUS_OBSTRUCTED.equals(contentsStatus) ||
            STATUS_REPLACED.equals(contentsStatus);
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
