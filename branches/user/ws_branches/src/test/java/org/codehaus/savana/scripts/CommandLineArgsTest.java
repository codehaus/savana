package org.codehaus.savana.scripts;

import junit.framework.TestCase;
import org.apache.commons.cli.Option;
import org.codehaus.savana.util.cli.CommandLineProcessor;
import org.codehaus.savana.util.cli.SavanaArgument;

import java.util.*;

public class CommandLineArgsTest extends TestCase {
    public void testCommandLineArgs() throws Exception {

        final Option x = new Option("x", "XXX", true, "x");
        final Option y = new Option("y", "why", true, "y");
        final Option z = new Option("Z", "zee", false, "z");
        final Option w = new Option("w", "dubya", false, "w");

        final List<String> argsSeen = new ArrayList<String>();
        final List<Option> optionsSeen = new ArrayList<Option>();
        final Map<Option, String> optionArgs = new HashMap<Option, String>();

        final SavanaArgument fu = new SavanaArgument(
                "fu", "argument fu");
        final SavanaArgument bar = new SavanaArgument(
                "bar", "argument bar", "baz");

        CommandLineProcessor processor = new CommandLineProcessor(
                new CommandLineProcessor.ArgumentHandler(fu) {
                    public void handle(String arg) {
                        argsSeen.add(arg);
                    }
                },
                new CommandLineProcessor.ArgumentHandler(bar) {
                    public void handle(String arg) {
                        argsSeen.add(arg);
                    }
                },
                new CommandLineProcessor.OptionHandler(w) {
                    public void ifSet() {
                        assertTrue(false);
                    }

                    public void withArg(String arg) {
                        assertTrue(false);
                    }
                },
                new CommandLineProcessor.OptionHandler(x) {
                    public void ifSet() {
                        optionsSeen.add(x);
                    }

                    public void withArg(String arg) {
                        optionArgs.put(x, arg);
                    }
                },
                new CommandLineProcessor.OptionHandler(y) {
                    public void ifSet() {
                        optionsSeen.add(y);
                    }

                    public void withArg(String arg) {
                        optionArgs.put(y, arg);
                    }
                },
                new CommandLineProcessor.OptionHandler(z) {
                    public void ifSet() {
                        optionsSeen.add(z);
                    }

                    public void withArg(String arg) {
                        assertTrue(false);
                    }
                });
        processor.processCommandLine(new String[]{"-x", "ecks", "--why", "YYYY", "--zee", "foo", "bar"});

        assertEquals(Arrays.asList("foo", "bar"), argsSeen);
        assertEquals(Arrays.asList(x, y, z), optionsSeen);
        assertEquals("ecks", optionArgs.get(x));
        assertEquals("YYYY", optionArgs.get(y));
        assertNull(optionArgs.get(z));

        assertEquals("usage: savana test <fu> [<bar>]\n" +
                     "<fu>      argument fu\n" +
                     "<bar>     argument bar\n" +
                     "options:\n" +
                     " -Z,--zee         z\n" +
                     " -w,--dubya       w\n" +
                     " -x,--XXX <arg>   x\n" +
                     " -y,--why <arg>   y",
                     processor.usage("test").trim());
    }
}
