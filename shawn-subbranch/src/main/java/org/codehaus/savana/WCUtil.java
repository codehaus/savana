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
 * @author Shawn Smith (shawn@bazaarvoice.com)
 */
package org.codehaus.savana;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.admin.ISVNAdminAreaFactorySelector;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;

import java.io.File;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Utilities for subversion working copy info.
 */
public class WCUtil {

    /**
     * Returns the format of the subversion working copy admin area.  See
     * {@link org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory#WC_FORMAT_15}.
     */
    public static int getWorkingCopyFormatVersion(final File versionedDir) throws SVNException {
        SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
        try {
            SVNAdminArea adminArea = wcAccess.open(versionedDir, false, false, false, 0, Level.FINEST);
            return adminArea.getWorkingCopyFormatVersion();
        } finally {
            wcAccess.close();
        }
    }

    /**
     * Configures SVNKit to support only the specified working copy format.
     */
    public static void setSupportedWorkingCopyFormatVersion(final int workingCopyFormat) throws SVNException {
        // configure SVNKit to use file formats that match the installed version of subversion
        SVNAdminAreaFactory.setSelector(new ISVNAdminAreaFactorySelector() {
            public Collection getEnabledFactories(File path, Collection factories, boolean writeAccess) {
                Collection<SVNAdminAreaFactory> enabledFactories = new TreeSet<SVNAdminAreaFactory>();
                for (SVNAdminAreaFactory factory : (Collection<SVNAdminAreaFactory>) factories) {
                    if (factory.getSupportedVersion() == workingCopyFormat) {
                        enabledFactories.add(factory);
                    }
                }
                return enabledFactories;
            }
        });
    }
}
