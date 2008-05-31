package org.codehaus.savana.util.cli;

import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


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
 * @author Bryon Jacob (bryon@jacob.net)
 */
public class CommandLineProcessor {

    public static final CommandLineProcessor NO_ARGS_PROCESSOR = new CommandLineProcessor();

    private List<ArgumentHandler> argumentHandlers = new ArrayList<ArgumentHandler>();
    private List<OptionHandler> optionHandlers = new ArrayList<OptionHandler>();
    private Options options = new Options();

    /**
     * Construct a new CommandLineProcessor from the given list of handlers.
     *
     * @param handlers an intermixed list of ArgumentHandler and OptionHandler instances
     */
    public CommandLineProcessor(Handler... handlers) {
        // partition the list of handlers into arg and opt handlers
        for (Handler handler : handlers) {
            if (handler instanceof ArgumentHandler) {
                argumentHandlers.add((ArgumentHandler) handler);
            } else if (handler instanceof OptionHandler) {
                optionHandlers.add((OptionHandler) handler);
            }
        }
        // build up the Options instance for the CLI API
        for (OptionHandler optionHandler : optionHandlers) {
            options.addOption(optionHandler.getOption());
        }
    }

    /**
     * Process the given command line arguments and options, calling the appropriate callbacks.
     *
     * @param args the command line arguments and options
     * @return returns the "rest" of the command line, that wasn't handled by the processor
     */
    public String[] processCommandLine(String[] args) {
        // first, parst the command line using apache CLI
        CommandLine line = null;
        try {
            line = new GnuParser().parse(options, args);
        } catch (ParseException e) {
            throw new IllegalArgumentException("could not parse command line : " + e.getMessage(), e);
        }
        // loop through the arg handlers and the args, in order, matching them up - if we have fewer args than
        // handlers, that is okay if all of the "extra" handlers have a default - otherwise, throw an exception.
        Iterator<String> argIterator = line.getArgList().iterator();
        for (ArgumentHandler argumentHandler : argumentHandlers) {
            if (argIterator.hasNext()) {
                argumentHandler.handle(argIterator.next());
            } else {
                if (argumentHandler.getArgument().getDefaultValue() != null) {
                    argumentHandler.handle(argumentHandler.getArgument().getDefaultValue());
                } else {
                    throw new IllegalArgumentException(
                            "expected " + argumentHandlers.size() + " arguments, received " + line.getArgs().length);
                }
            }
        }
        // for each of our options...
        for (OptionHandler handler : optionHandlers) {
            // if the option was set, call the ifSet() callback on the handler
            if (line.hasOption(handler.getOption().getOpt())) {
                handler.ifSet();
                // if the option has an argument, pass it to the withArg() callback on the handler
                if (handler.getOption().hasArg()) {
                    handler.withArg(line.getOptionValue(handler.getOption().getLongOpt()));
                }
            }
        }
        // return the "remainder" of the args (often empty)
        List<String> remainder = new ArrayList<String>();
        while (argIterator.hasNext()) {
            remainder.add(argIterator.next());
        }
        return remainder.toArray(new String[remainder.size()]);
    }

    /**
     * print a usage statement for the command
     *
     * @param commandName the name for the command itself
     * @return a formatted string containing the usage statement
     */
    public String usage(String commandName) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        // the syntax is always "savana foo <args...>" for command foo
        StringBuilder syntax = new StringBuilder("savana " + commandName);
        int longestArgNameLength = 0;
        for (ArgumentHandler argumentHandler : argumentHandlers) {
            syntax.append(MessageFormat.format(
                    // if the default value is not null, then the arg is optional, so enclose in []
                    argumentHandler.getArgument().getDefaultValue() != null ? " [<{0}>]" : " <{0}>",
                    argumentHandler.getArgument().getName()));

            longestArgNameLength = Math.max(longestArgNameLength, argumentHandler.getArgument().getName().length());
        }
        // build up the header - the description of each argument, one per line
        StringBuilder header = new StringBuilder();
        for (ArgumentHandler argumentHandler : argumentHandlers) {
            header.append("<");
            header.append(argumentHandler.getArgument().getName());
            header.append(">");
            for (int i = argumentHandler.getArgument().getName().length();
                 i < longestArgNameLength + HelpFormatter.DEFAULT_DESC_PAD + 2; i++) {
                header.append(" ");
            }
            header.append(argumentHandler.getArgument().getDescription());
            header.append("\n");
        }
        // if there are any options, print the "options:" title
        if (!options.getOptions().isEmpty()) {
            header.append("options:");
        }
        // delegate to CLI for printing the actual message
        new HelpFormatter().printHelp(
                pw, 80,
                syntax.toString(),
                header.toString(),
                options,
                HelpFormatter.DEFAULT_LEFT_PAD,
                HelpFormatter.DEFAULT_DESC_PAD,
                null);
        pw.close();
        return stringWriter.toString();
    }

    /**
     * A "marker" interface to identify objects that are ArgumentHandlers or OptionHandlers.
     */
    public interface Handler {
    }

    /**
     * Base class for handlers that deal with command-line arguments.
     */
    public abstract static class ArgumentHandler implements Handler {
        private SavanaArgument argument;

        public ArgumentHandler(SavanaArgument argument) {
            this.argument = argument;
        }

        public abstract void handle(String arg);

        public SavanaArgument getArgument() {
            return argument;
        }
    }

    /**
     * Base class for handlers that deal with command-line options.
     */
    public abstract static class OptionHandler implements Handler {
        private Option option;

        protected OptionHandler(Option option) {
            this.option = option;
        }

        public void ifSet() {}

        public void withArg(String arg) {}

        public Option getOption() {
            return option;
        }
    }
}
