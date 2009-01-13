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

import org.codehaus.savana.scripts.admin.CreateMetadataFile;
import org.tmatesoft.svn.cli.AbstractSVNCommand;
import org.tmatesoft.svn.cli.AbstractSVNCommandEnvironment;
import org.tmatesoft.svn.cli.AbstractSVNLauncher;
import org.tmatesoft.svn.cli.SVNCommandLine;
import org.tmatesoft.svn.cli.svn.SVNOption;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SAV extends AbstractSVNLauncher {
    private PrintStream _out = System.out;
    private PrintStream _err = System.err;

    public static void main(String[] args) {
        new SAV().run(args);
    }

    @Override
    public void run(String[] args) {
        // change the protection so run() can be called from test cases
        super.run(args);
    }

    @Override
    protected String getProgramName() {
        return "sav";
    }

    @Override
    protected AbstractSVNCommandEnvironment createCommandEnvironment() {
        return new SAVCommandEnvironment(getProgramName(), _out, _err, System.in);
    }

    @Override
    protected void registerCommands() {
        AbstractSVNCommand.registerCommand(new CreateReleaseBranch());
        AbstractSVNCommand.registerCommand(new CreateUserBranch());
        // there's no DeleteReleaseBranch command--it's dangerous, almost never useful,
        // and has an easy workaround: svn delete <release branch url>
        AbstractSVNCommand.registerCommand(new DeleteUserBranch());
        AbstractSVNCommand.registerCommand(new DiffChangesFromSource());
        AbstractSVNCommand.registerCommand(new Help());
        AbstractSVNCommand.registerCommand(new ListChangesFromSource());
        AbstractSVNCommand.registerCommand(new ListReleaseBranches());
        AbstractSVNCommand.registerCommand(new ListUserBranches());
        AbstractSVNCommand.registerCommand(new ListWorkingCopyInfo());
        AbstractSVNCommand.registerCommand(new Promote());
        AbstractSVNCommand.registerCommand(new RevertToSource());
        AbstractSVNCommand.registerCommand(new SetBranch());
        AbstractSVNCommand.registerCommand(new Synchronize());

        //Admin commands
        AbstractSVNCommand.registerCommand(new CreateMetadataFile());
    }

    @Override
    protected void registerOptions() {
        //Never upgrade the working copy format from one version of svn to another.  Use other apps to do so (for example: 'svn update').
        SVNAdminAreaFactory.setUpgradeEnabled(false);

        //Options shared with svn/jsvn
        SVNCommandLine.registerOption(SVNOption.VERSION);
        SVNCommandLine.registerOption(SVNOption.QUIET);
        SVNCommandLine.registerOption(SVNOption.DIFF_CMD);
        SVNCommandLine.registerOption(SVNOption.EXTENSIONS);
        SVNCommandLine.registerOption(SVNOption.FORCE);
        SVNCommandLine.registerOption(SVNOption.NO_DIFF_DELETED);
        SVNCommandLine.registerOption(SVNOption.RECURSIVE);
        SVNCommandLine.registerOption(SVNOption.DEPTH);
        SVNCommandLine.registerOption(SVNOption.TARGETS);

        //Global options
        SVNCommandLine.registerOption(SVNOption.USERNAME);
        SVNCommandLine.registerOption(SVNOption.PASSWORD);
        SVNCommandLine.registerOption(SVNOption.NO_AUTH_CACHE);
        SVNCommandLine.registerOption(SVNOption.NON_INTERACTIVE);
        SVNCommandLine.registerOption(SVNOption.CONFIG_DIR);

        //Savana-specific options
        SVNCommandLine.registerOption(SAVOption.CHANGE_ROOT);
        SVNCommandLine.registerOption(SAVOption.PROJECT_NAME);
        SVNCommandLine.registerOption(SAVOption.TRUNK_PATH);
        SVNCommandLine.registerOption(SAVOption.RELEASE_BRANCHES_PATH);
        SVNCommandLine.registerOption(SAVOption.USER_BRANCHES_PATH);
        SVNCommandLine.registerOption(SAVOption.SAVANA_POLICIES_FILE);

        //Options for specifying a commit message (shared with svn/jsvn)
        List<SVNOption> logMessageOptions = new ArrayList<SVNOption>();
        SVNOption.addLogMessageOptions(logMessageOptions);
        for (SVNOption logMessageOption : logMessageOptions) {
            SVNCommandLine.registerOption(logMessageOption);
        }
    }

    @Override
    protected boolean needArgs() {
        return true;
    }

    @Override
    protected boolean needCommand() {
        return true;
    }

    /** For testing. */
    public void setOut(PrintStream out) {
        _out = out;
    }
    public void setErr(PrintStream err) {
        _err = err;
    }
}
