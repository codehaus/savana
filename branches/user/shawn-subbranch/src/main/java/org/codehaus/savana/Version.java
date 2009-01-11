/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2009  Bazaarvoice Inc.
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
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.codehaus.savana;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

    public static final String VERSION = getVersion();

    public static final String SVNKIT_VERSION =
            org.tmatesoft.svn.util.Version.getMajorVersion() + "." +
            org.tmatesoft.svn.util.Version.getMinorVersion() + "." +
            org.tmatesoft.svn.util.Version.getMicroVersion();

    private static String getVersion() {
        InputStream in = Version.class.getClassLoader().getResourceAsStream("org/codehaus/savana/version.properties");
        if (in != null) {
            Properties props = new Properties();
            try {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException(e); // should never happen
            } finally {
                IOUtils.closeQuietly(in);
            }
            return props.getProperty("savana.version") + " (revision " + props.getProperty("savana.revision") + ")";
        }
        return "UNKNOWN";
    }
}
