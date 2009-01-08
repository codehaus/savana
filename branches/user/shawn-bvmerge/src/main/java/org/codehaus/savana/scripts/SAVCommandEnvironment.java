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

import org.codehaus.savana.BranchType;
import org.tmatesoft.svn.cli.AbstractSVNOption;
import org.tmatesoft.svn.cli.SVNOptionValue;
import org.tmatesoft.svn.cli.svn.SVNCommandEnvironment;
import org.tmatesoft.svn.core.SVNException;

import java.io.InputStream;
import java.io.PrintStream;

public class SAVCommandEnvironment extends SVNCommandEnvironment {

    private boolean _changeRoot;
    private String _projectName;
    private String _trunkPath = BranchType.TRUNK.getDefaultPath();
    private String _releaseBranchesPath = BranchType.RELEASE_BRANCH.getDefaultPath();
    private String _userBranchesPath = BranchType.USER_BRANCH.getDefaultPath();

    public SAVCommandEnvironment(String programName, PrintStream out, PrintStream err, InputStream in) {
        super(programName, out, err, in);
    }

    @Override
    protected void initOption(SVNOptionValue optionValue) throws SVNException {
        AbstractSVNOption option = optionValue.getOption();
        if (option == SAVOption.CHANGE_ROOT) {
            _changeRoot = true;
        } else if (option == SAVOption.PROJECT_NAME) {
            _projectName = optionValue.getValue();
        } else if (option == SAVOption.TRUNK_PATH) {
            _trunkPath = optionValue.getValue();
        } else if (option == SAVOption.RELEASE_BRANCHES_PATH) {
            _releaseBranchesPath = optionValue.getValue();
        } else if (option == SAVOption.USER_BRANCHES_PATH) {
            _userBranchesPath = optionValue.getValue();
        } else {
            super.initOption(optionValue);
        }
    }

    public boolean isChangeRoot() {
        return _changeRoot;
    }

    public String getProjectName() {
        return _projectName;
    }

    public String getTrunkPath() {
        return _trunkPath;
    }

    public String getReleaseBranchesPath() {
        return _releaseBranchesPath;
    }

    public String getUserBranchesPath() {
        return _userBranchesPath;
    }
}
