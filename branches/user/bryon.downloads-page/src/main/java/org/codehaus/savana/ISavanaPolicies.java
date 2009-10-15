/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2008-2009  Bazaarvoice Inc.
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

import java.util.Properties;

public interface ISavanaPolicies {

    static final String CLASS_KEY = "class";

    void initialize(Properties properties) throws SVNException;

    void validateSavanaVersion() throws SVNException;

    void validateLogMessage(String logMessage, MetadataProperties metadataProperties) throws SVNException;
}
