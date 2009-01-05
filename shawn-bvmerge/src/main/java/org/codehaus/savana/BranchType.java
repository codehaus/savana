/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006-2008  Bazaarvoice Inc.
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

public enum BranchType {

    /**
     * The trunk branch is the main branch for ongoing development work.  There is one of these per project.
     */
    TRUNK("TRUNK", "trunk"),

    /**
     * A release branch is a branch off the trunk corresponding to a particular major version of the project.
     * Work on patches to the release will occur off the release branch.
     */
    RELEASE_BRANCH("RELEASE BRANCH", "branches"),

    /**
     * A user branch is a private workspace where changes to the trunk or to a release branch can be developed
     * before being promoted to a trunk or release branch. 
     */
    USER_BRANCH("USER BRANCH", "branches/user");

    private final String _keyword;
    private final String _defaultPath;

    private BranchType(String keyword, String defaultPath) {
        _keyword = keyword;
        _defaultPath = defaultPath;
    }

    public String getKeyword() {
        return _keyword;
    }

    public String getDefaultPath() {
        return _defaultPath;
    }

    public static BranchType fromKeyword(String keyword) {
        for (BranchType branchType : BranchType.values()) {
            if (branchType.name().equalsIgnoreCase(keyword) || branchType.getKeyword().equalsIgnoreCase(keyword)) {
                return branchType;
            }
        }
        throw new IllegalArgumentException("Unknown branch type: " + keyword);
    }
}
