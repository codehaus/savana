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
 * @author Brian Showers (brian@bazaarvoice.com)
 * @author Bryon Jacob (bryon@jacob.net)
 * @author Shawn Smith (shawn@bazaarvoice.com)
 */
package org.codehaus.savana.scripts;

import org.tmatesoft.svn.cli.AbstractSVNOption;

/**
 */
public class SAVOption extends AbstractSVNOption {

    public static final SAVOption CHANGE_ROOT = new SAVOption("changeRoot");

    public static final SAVOption PROJECT_NAME = new SAVOption("projectName", false);
    public static final SAVOption TRUNK_PATH = new SAVOption("trunkPath", false);
    public static final SAVOption RELEASE_BRANCHES_PATH = new SAVOption("releaseBranchesPath", false);
    public static final SAVOption USER_BRANCHES_PATH = new SAVOption("userBranchesPath", false);

    private SAVOption(String name) {
        this(name, null, true);
    }

    private SAVOption(String name, boolean unary) {
        this(name, null, unary);
    }

    private SAVOption(String name, String alias) {
        this(name, alias, true);
    }

    private SAVOption(String name, String alias, boolean unary) {
        super(name, alias, unary);
    }

    @Override
    protected String getResourceBundleName() {
        return "org.codehaus.savana.scripts.options";
    }
}
