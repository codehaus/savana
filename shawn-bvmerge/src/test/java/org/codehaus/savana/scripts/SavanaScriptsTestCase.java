package org.codehaus.savana.scripts;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.tmatesoft.svn.cli.AbstractSVNCommand;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Base test case for Savana unit tests - defines useful methods for testing savana components.
 *
 * @author Bryon Jacob (bryon@jacob.net)
 */
public class SavanaScriptsTestCase  extends TestCase {

    protected static final SVNClientManager SVN =
            SVNClientManager.newInstance(new DefaultSVNOptions(), "savana-user", "");

    protected static final File SUBVERSION_CONFIG_DIR = tempDir("subversion-config");

    protected static final String TEST_PROJECT_NAME = "test-project";

    static {
        FSRepositoryFactory.setup();        
    }

    /**
     * execute the given savana script with the given arguments, returning script output as a string.
     *
     * @param scriptClass the script class to run
     * @param args        the arguments to the script
     * @return the script output
     * @throws Exception on error
     */
    protected static String savana(Class<? extends AbstractSVNCommand> scriptClass, String... args) throws Exception {
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
        args = (String[]) ArrayUtils.addAll(args, new String[]{"--config-dir", SUBVERSION_CONFIG_DIR.getPath()});

        //Run the command 
        savana.setOut(new PrintStream(bufOut, true));
        savana.setErr(new PrintStream(bufErr, true));
        savana.run(args);

        //Throw an exception if the script failed or printed anything to stderr.
        String out = bufOut.toString();
        String err = bufErr.toString();
        if (!success[0] || err.length() > 0) {
            throw new SavanaScriptsTestException(out, err);
        }

        //Success.  Return whatever was printed to stdout.
        return out.toString().trim();
    }

    /**
     * set the current working directory to the given directory.
     *
     * @param dir the directory to which we want to change
     */
    protected static void cd(File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
    }

    /**
     * create a new temporary directory with the given name, deleting any existing directory with that name first.
     *
     * @param tempDirName the name of the temporary directory to create
     * @return the directory
     */
    protected static File tempDir(String tempDirName) {
        File tmpDir = new File("target/testdata").getAbsoluteFile();
        File dir = new File(tmpDir, tempDirName);
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dir;
    }
}
