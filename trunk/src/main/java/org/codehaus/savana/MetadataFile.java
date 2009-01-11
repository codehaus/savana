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
 * @author Brian Showers (brian@bazaarvoice.com)
 * @author Bryon Jacob (bryon@jacob.net)
 * @author Shawn Smith (shawn@bazaarvoice.com)
 */
package org.codehaus.savana;

import java.io.File;

public class MetadataFile extends File {
    public static final String METADATA_FILE_NAME = ".savana";
    public static final String METADATA_FILE_NAME_BACKWARD_COMPATIBLE = ".svnscripts";

    public static final String PROP_PROJECT_ROOT = "PROJECT_ROOT";
    public static final String PROP_PROJECT_NAME = "PROJECT_NAME";
    public static final String PROP_SOURCE_PATH = "SOURCE_PATH";
    public static final String PROP_BRANCH_PATH = "BRANCH_PATH";
    public static final String PROP_BRANCH_TYPE = "BRANCH_TYPE";
    public static final String PROP_BRANCH_POINT_REVISION = "BRANCH_POINT_REVISION";
    public static final String PROP_LAST_MERGE_REVISION = "LAST_MERGE_REVISION";

    public static final String PROP_TRUNK_PATH = "TRUNK_PATH";
    public static final String PROP_RELEASE_BRANCHES_PATH = "BRANCHES_PATH";
    public static final String PROP_USER_BRANCHES_PATH = "USER_BRANCHES_PATH";
    public static final String PROP_SAVANA_POLICIES = "SAVANA_POLICIES";

    public MetadataFile(File parent, String child) {
        super(parent, child);
    }
}
