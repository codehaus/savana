package org.codehaus.savana.scripts;

import junit.framework.TestCase;
import org.tmatesoft.svn.cli.AbstractSVNCommand;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.io.File;

/**
 * Base test case for Savana unit tests - defines useful methods for testing savana components.
 *
 * @author Bryon Jacob (bryon@jacob.net)
 */
public abstract class AbstractSavanaScriptsTestCase extends TestCase {

    protected static final SVNClientManager SVN = TestRepoUtil.SVN;

    /**
     * execute the given savana script with the given arguments, returning script output as a string.
     *
     * @param scriptClass the script class to run
     * @param args        the arguments to the script
     * @return the script output
     * @throws Exception on error
     */
    protected String savana(Class<? extends AbstractSVNCommand> scriptClass, String... args) throws Exception {
        return TestSavanaUtil.savana(scriptClass, args);
    }

    /**
     * set the current working directory to the given directory.
     *
     * @param dir the directory to which we want to change
     */
    protected void cd(File dir) {
        TestDirUtil.cd(dir);
    }

    /**
     * create a new temporary directory with the given name, deleting any existing directory with that name first.
     *
     * @param tempDirName the name of the temporary directory to create
     * @return the directory
     */
    protected File createTempDir(String tempDirName) {
        return TestDirUtil.createTempDir(tempDirName);
    }

    public static void assertEqualsNormalized(java.lang.String expected, java.lang.String actual) {
        assertEquals(normalize(expected), normalize(actual));
    }

    public static String normalize(String s) {
        return s.replace("\\", "/");
    }
}
