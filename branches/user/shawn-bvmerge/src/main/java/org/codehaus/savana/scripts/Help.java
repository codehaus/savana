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
package org.codehaus.savana.scripts;

import org.codehaus.savana.Version;
import org.tmatesoft.svn.cli.AbstractSVNCommand;
import org.tmatesoft.svn.cli.SVNCommandUtil;
import org.tmatesoft.svn.cli.svn.SVNCommandEnvironment;
import org.tmatesoft.svn.core.SVNException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 */
public class Help extends SAVCommand {

    private static final String GENERIC_HELP_HEADER =
        "usage: {0} <subcommand> [options] [args]\n" +
        "Savana command-line client, version %s (SVNKit version {1}).\n" +
        "Type ''{0} help <subcommand>'' for help on a specific subcommand.\n" +
        "Type ''{0} --version'' to see the program version and description.\n" +
        "  or ''{0} --version --quiet'' to see just the version number.\n" +
        "\n" +
        "Available subcommands:";

    private static final String GENERIC_HELP_FOOTER =
        "Savana adds Transactional Workspaces on top of Subversion.\n" +
        "For additional information, see http://savana.codehaus.org/\n";

    private static final String VERSION_HELP_BODY =
            "%s, version %s (SVNKit version %s)" +
            "\n" +
            "\nCopyright (c) 2006-2009 Bazaarvoice Inc." +
            "\nSavana is open source (GPL) software, see http://savana.codehaus.org/ for more" +
            "\ninformation. Savana adds workspace creation, merging and promotion on top of" +
            "\nSubversion. See http://subversion.tigris.org/ for information about Subversion.";

    public Help() {
        super("help", new String[] {"?", "h"});
    }

    @Override
    protected Collection createSupportedOptions() {
        return new ArrayList();
    }

    public void doRun() throws SVNException {
        SVNCommandEnvironment env = getSVNEnvironment();
        if (!env.getArguments().isEmpty()) {
            for (Iterator commands = env.getArguments().iterator(); commands.hasNext();) {
                String commandName = (String) commands.next();
                AbstractSVNCommand command = AbstractSVNCommand.getCommand(commandName);
                if (command == null) {
                    env.getErr().println("\"" + commandName + "\": unknown command.\n");
                    continue;
                }
                String help = SVNCommandUtil.getCommandHelp(command, getEnvironment().getProgramName(), true);
                env.getOut().println(help);
            }

        } else if (env.isVersion()) {
            if (env.isQuiet()) {
                env.getOut().println(Version.VERSION);
            } else {
                env.getOut().println(String.format(VERSION_HELP_BODY, env.getProgramName(), Version.VERSION, Version.SVNKIT_VERSION));
            }

        } else if (env.getArguments().isEmpty()) {
            String helpHeader = String.format(GENERIC_HELP_HEADER, Version.VERSION);
            String help = SVNCommandUtil.getGenericHelp(getEnvironment().getProgramName(), helpHeader, GENERIC_HELP_FOOTER, null);
            env.getOut().print(help);

        } else {
            env.getOut().println(String.format("Type ''%s help'' for usage.", env.getProgramName()));
        }
    }
}