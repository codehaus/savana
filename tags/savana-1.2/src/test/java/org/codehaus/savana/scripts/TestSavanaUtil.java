package org.codehaus.savana.scripts;

import org.apache.commons.lang.ArrayUtils;
import org.tmatesoft.svn.cli.AbstractSVNCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public abstract class TestSavanaUtil {

    /**
     * execute the given savana script with the given arguments, returning script output as a string.
     *
     * @param scriptClass the script class to run
     * @param args        the arguments to the script
     * @return the script output
     * @throws Exception on error
     */
    public static String savana(Class<? extends AbstractSVNCommand> scriptClass, String... args) throws Exception {
        final boolean[] success = new boolean[1];
        final ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream bufErr = new ByteArrayOutputStream();

        SAV savana = new SAV() {
            @Override
            public void success() {
                // suppress System.exit(0)
                success[0] = true;
            }
            @Override
            public void failure() {
                // suppress System.exit(1)
                success[0] = false;
            }
        };

        //Add the command name as the first argument
        String scriptName = scriptClass.newInstance().getName();
        args = (String[]) ArrayUtils.addAll(new String[]{scriptName}, args);
        //Ignore the user's local subversion configuration files
        args = (String[]) ArrayUtils.addAll(args, new String[]{
                "--config-dir", TestRepoUtil.SUBVERSION_CONFIG_DIR.getPath(),
                "--non-interactive",
        });

        //Run the command
        savana.setOut(new PrintStream(bufOut, true));
        savana.setErr(new PrintStream(bufErr, true));
        savana.run(args);

        //Throw an exception if the script failed or printed anything to stderr.
        //Normalize to unix-style eol so test cases see the same thing on *nix and Windows.
        String out = bufOut.toString().replace(System.getProperty("line.separator"), "\n");
        String err = bufErr.toString().replace(System.getProperty("line.separator"), "\n");
        if (!success[0] || err.length() > 0) {
            throw new SavanaScriptsTestException(out, err);
        }

        //Success.  Return whatever was printed to stdout.
        return out.toString().trim();
    }
}
