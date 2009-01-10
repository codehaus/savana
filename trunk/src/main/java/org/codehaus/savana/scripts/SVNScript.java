package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.savana.SVNScriptException;
import org.codehaus.savana.SVNVersion;
import org.codehaus.savana.WorkingCopyInfo;
import org.codehaus.savana.util.cli.CommandLineProcessor;
import org.codehaus.savana.util.cli.CommandAliases;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.admin.ISVNAdminAreaFactorySelector;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;

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
 * Third party components of this software are provided or made available only subject
 * to their respective licenses. The relevant components and corresponding
 * licenses are listed in the "licenses" directory in this distribution. In any event,
 * the disclaimer of warranty and limitation of liability provision in this Agreement
 * will apply to all Software in this distribution.
 *
 * @author Brian Showers (brian@bazaarvoice.com)
 * @author Bryon Jacob (bryon@jacob.net)
 */
public abstract class SVNScript {
    private static final String USERS_FILE = ".savana-userinfo";

    private static final Set<String> HELP_ARGUMENTS = new HashSet<String>(Arrays.asList(
            "help", "-help", "/help", "?", "-?", "/?"));

    private static final CommandAliases ALIASES = new CommandAliases();

    protected final Log _sLog = LogFactory.getLog(getClass());
    protected SVNRepository _repository;
    protected SVNClientManager _clientManager;
    protected CommandLineProcessor _commandLineProcessor;

    private static PrintStream OUT = System.out;

    public static PrintStream getOut() {
        return OUT;
    }

    public static void setOut(PrintStream out) {
        SVNScript.OUT = out;
    }

    public SVNScript()
            throws SVNException, SVNScriptException {
        //set up the command line processor
        logStart("setting up the command line processor");
        _commandLineProcessor = constructCommandLineProcessor();
        logEnd("setting up the command line processor");

        //for DAV (over http and https)
        logStart("Setup DAV Repository");
        DAVRepositoryFactory.setup();
        logEnd("Setup DAV Repository");

        //for svn (over svn and svn+ssh)
        logStart("Setup SVN Repository");
        SVNRepositoryFactoryImpl.setup();
        logEnd("Setup SVN Repository");

        logStart("Setup File-Based Repository Support");
        FSRepositoryFactory.setup();
        logEnd("Setup File-Based Repository Support");

        //Figure out the Repository's URL
        logStart("Getting Repository URL and User Info");

        //Create the authorization manager
        logStart("Create Authentication Manager");
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
        logEnd("Create Authentication Manager");

        //Create a client manager
        logStart("Create Client Manager");
        _clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authManager);
        logEnd("Create Client Manager");

        //Set the working copy format
        logStart("Load SVN Version");
        try {
            SVNVersion svnVersion = new WorkingCopyInfo(_clientManager).getSVNVersion();
            setWorkingCopyFormat(svnVersion);
        } catch (SVNScriptException e) {
            _sLog.warn("the current directory does not appear to be a savana working copy (yet) - assuming that" +
                       "we are using SVN version 1.4");
        }
        logEnd("Load SVN Version");

        //Create the repository
        logStart("Create Repository");
        SVNURL repositoryURL = getRepositoryURL();
        _repository = SVNRepositoryFactory.create(repositoryURL);
        _repository.setAuthenticationManager(authManager);
        logEnd("Create Repository");
    }

    private void setupScriptWithUsersFile() throws SVNScriptException, SVNException {
        SVNURL repositoryURL = getRepositoryURL();
        String username;
        String password;
        try {
            File usersFile = new File(System.getProperty("savana.home"), USERS_FILE);
            // if the file doesn't exist, touch it.
            if (!usersFile.exists()) {
                FileUtils.touch(usersFile);
            }
            List<String> lines = FileUtils.readLines(usersFile, "UTF-8");
            username = null;
            password = null;
            for (String line : lines) {
                if (!"".equals(line.trim())) {
                    String[] parts = line.split("\\|", 3);
                    if (parts[0].equals(repositoryURL.toString())) {
                        username = parts[1];
                        password = parts[2];
                    }
                }
            }

            if (username == null) {
                getOut().println("using SVN repository " + repositoryURL.toString());
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                getOut().print("SVN user name : ");
                username = reader.readLine();
                getOut().print("SVN password : ");
                password = reader.readLine();
                FileUtils.writeStringToFile(usersFile, MessageFormat.format("{0}|{1}|{2}\n{3}",
                                                                            repositoryURL.toString(),
                                                                            username,
                                                                            password,
                                                                            FileUtils.readFileToString(usersFile)));
            }
        } catch (IOException e) {
            String errorMessage =
                    "ERROR: Could not find " + USERS_FILE + " and failed to get credentials from user. " +
                    "\nSee the installation documentation for more information";
            throw new SVNScriptException(errorMessage);
        }

        logEnd("Getting Repository URL and User Info");

        //Create the authorization manager
        logStart("Create Authentication Manager");
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
        logEnd("Create Authentication Manager");

        //Create a client manager
        logStart("Create Client Manager");
        _clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), username, password);
        logEnd("Create Client Manager");

        //Create the repository
        logStart("Create Repository");
        _repository = SVNRepositoryFactory.create(repositoryURL);
        _repository.setAuthenticationManager(authManager);
        logEnd("Create Repository");
    }

    private void setWorkingCopyFormat(SVNVersion version) {
        if (version == SVNVersion.SVN_1_3) {
            logStart("Set SVN Version to 1.3");
            SVNAdminAreaFactory.setSelector(new ISVNAdminAreaFactorySelector() {
                //Only allow 1.3 working copy factories
                public Collection getEnabledFactories(File path, Collection factories, boolean writeAccess) {
                    Collection<SVNAdminAreaFactory> enabledFactories = new TreeSet<SVNAdminAreaFactory>();
                    //noinspection unchecked
                    for (SVNAdminAreaFactory factory : (Collection<SVNAdminAreaFactory>) factories) {
                        if (factory.getSupportedVersion() == SVNAdminAreaFactory.WC_FORMAT_13) {
                            enabledFactories.add(factory);
                        }
                    }
                    return enabledFactories;
                }
            });
            logEnd("Set SVN Version to 1.3");
        }
    }

    public CommandLineProcessor constructCommandLineProcessor() {
        return CommandLineProcessor.NO_ARGS_PROCESSOR;
    }

    public void initialize(String[] args)
            throws IllegalArgumentException {
        _commandLineProcessor.processCommandLine(args);
    }

    public abstract void run()
            throws SVNException, SVNScriptException;

    public abstract String getUsageMessage();

    public void logStart(String message) {
        _sLog.debug("Start: " + message);
    }

    public void logEnd(String message) {
        _sLog.debug("End:   " + message);
    }

    protected SVNURL _repositoryUrl = null;

    protected SVNURL getRepositoryURL() throws SVNException {
        if (_repositoryUrl == null) {
            try {
                _repositoryUrl = SVNClientManager.newInstance().getWCClient()
                        .doInfo(new File(System.getProperty("user.dir")),
                                SVNRevision.WORKING).getRepositoryRootURL();
            } catch (SVNException e) {
                _sLog.warn("not running from a working copy - prompting user for repository info");
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    getOut().print("SVN repository root : ");
                    _repositoryUrl = SVNURL.parseURIDecoded(reader.readLine());
                } catch (IOException ioe) {
                    _sLog.error("user entered invalid SVN URL", ioe);
                    throw e;
                }
            }
        }
        return _repositoryUrl;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        }
        String scriptName = args[0];
        String[] scriptArgs = new String[args.length - 1];
        System.arraycopy(args, 1, scriptArgs, 0, scriptArgs.length);

        //Load the script class
        SVNScript script = null;
        try {
            script = (SVNScript) ALIASES.loadCommandClassByNameOrAlias(scriptName).newInstance();
        }
        catch (ClassNotFoundException e) {
            System.err.println("ERROR: Unknown subcommand: " + scriptName);
            usage();
        }
        catch (InstantiationException e) {
            System.err.println("ERROR: Could not instantiate script: " + scriptName);
            usage();
        }
        catch (IllegalAccessException e) {
            System.err.println("ERROR: Could not access script: " + scriptName);
            usage();
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
            usage();
        }

        //See if the user is asking for help
        if (scriptArgs.length > 0 && HELP_ARGUMENTS.contains(scriptArgs[0])) {
            getOut().println(script.getUsageMessage());
            System.exit(0);
        }

        //Initialize the script with any arguments
        try {
            script.logStart("SCRIPT.initialize()");
            script.initialize(scriptArgs);
        }
        catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.err.println(script.getUsageMessage());
            System.exit(1);
        }
        finally {
            script.logEnd("SCRIPT.initialize()");
        }

        //Run the script
        try {
            script.logStart("SCRIPT.run()");
            try {
                script.run();
            } catch (SVNAuthenticationException e) {
                rerunScriptWithUsersFile( script );
            } catch (SVNScriptException e) {
                if ( e.getCause() instanceof SVNAuthenticationException ) {
                    rerunScriptWithUsersFile( script );
                } else {
                    throw e;
                }
            }
        }
        catch (Exception e) {
            script._sLog.error(e.getMessage(), e);
            System.err.println(e.getMessage());
            System.exit(1);
        }
        finally {
            script.logEnd("SCRIPT.run()");
        }
    }

    private static void rerunScriptWithUsersFile( SVNScript script ) throws Exception {
        // reconfigure and run again.
        script.setupScriptWithUsersFile();
        script.run();
    }    

    private static void usage() {
        System.err.println("usage:  sav <subcommand> <args>");
        System.err.println();
        System.err.println("Available subcommands:");
        for (String command : ALIASES.getCommandList()) {
            System.err.println("\t" + command);
        }
        System.exit(1);
    }
}