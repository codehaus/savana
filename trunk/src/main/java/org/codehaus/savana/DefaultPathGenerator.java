package org.codehaus.savana;

import org.codehaus.savana.util.PropertiesLoader;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;

import java.util.Properties;

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
public class DefaultPathGenerator implements IPathGenerator {
    private static final String PATHS_PROPS_FILE = "paths.properties";

    private static final String PROP_REPOSITORY_TRUNK_PATH = "repository.trunk.path";
    private static final String PROP_REPOSITORY_BRANCHES_PATH = "repository.branches.path";
    private static final String PROP_REPOSITORY_USER_BRANCHES_PATH = "repository.userbranches.path";

    private static final String PROP_TRUNK_KEYWORD = "trunk.keyword";

    private String _repositoryTrunkPath;
    private String _repositoryBranchesPath;
    private String _repositoryUserBranchesPath;
    private String _trunkKeyword;

    public DefaultPathGenerator() {
        //Load the properties file
        Properties paths_props = PropertiesLoader.getInstance().getProperties(PATHS_PROPS_FILE);
        _repositoryTrunkPath = paths_props.getProperty(PROP_REPOSITORY_TRUNK_PATH, "");
        _repositoryBranchesPath = paths_props.getProperty(PROP_REPOSITORY_BRANCHES_PATH, "");
        _repositoryUserBranchesPath = paths_props.getProperty(PROP_REPOSITORY_USER_BRANCHES_PATH, "");

        _trunkKeyword = paths_props.getProperty(PROP_TRUNK_KEYWORD, "trunk");
    }

    public String getProjectPath(String projectName) {
        return projectName;
    }

    public String getTrunkPath(String projectName) {
        return SVNPathUtil.append(getProjectPath(projectName), _repositoryTrunkPath);
    }

    public String getReleaseBranchPath(String projectName, String branchName) {
        if (_trunkKeyword.equalsIgnoreCase(branchName)) {
            return getTrunkPath(projectName);
        } else {
            //Create a path to the directory where branches are located
            String path = SVNPathUtil.append(getProjectPath(projectName), _repositoryBranchesPath);
            path = SVNPathUtil.append(path, branchName);
            return path;
        }
    }

    public String getUserBranchPath(String projectName, String branchName) {
        if (_trunkKeyword.equalsIgnoreCase(branchName)) {
            return getTrunkPath(projectName);
        }
        //Create a path to the directory where user branches are located
        String path = SVNPathUtil.append(getProjectPath(projectName), _repositoryUserBranchesPath);
        path = SVNPathUtil.append(path, branchName);
        return path;
    }

    public String getTrunkKeyword(String projectName) {
        return _trunkKeyword;
    }
}
