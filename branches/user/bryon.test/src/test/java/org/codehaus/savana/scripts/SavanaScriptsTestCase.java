package org.codehaus.savana.scripts;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;

/**
 * Base test case for Savana unit tests - defines useful methods for testing savana components.
 *
 * @author Bryon Jacob (bryon@jacob.net)
 */
public class SavanaScriptsTestCase  extends TestCase {

    protected static final SVNClientManager SVN =
            SVNClientManager.newInstance(new DefaultSVNOptions(), "savana-user", "");

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
    protected static String savana(Class scriptClass, String... args) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SVNScript script = ((SVNScript) scriptClass.newInstance());
        SVNScript.setOut(new PrintStream(outputStream));
        script.initialize(args);
        script.run();
        return outputStream.toString("UTF-8").trim();
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
        String tmpDir = new File("target/testdata").getAbsolutePath();
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
