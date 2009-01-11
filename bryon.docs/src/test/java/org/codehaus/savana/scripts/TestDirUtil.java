package org.codehaus.savana.scripts;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class TestDirUtil {

    public static final File INITIAL_DIRECTORY = new File("").getAbsoluteFile();

    public static final File TESTDATA_DIRECTORY = new File(INITIAL_DIRECTORY, "target/testdata");

    /** Location of the {project}/src/main/svn-hooks directory, set by the test runner in pom.xml. */
    public static final File SVN_HOOKS_DIR = new File(System.getProperty("savana.svn-hooks")).getAbsoluteFile();
    
    /** Location of the suggested savana policies file in the svn-hooks directory. */
    public static final File POLICIES_FILE = new File(SVN_HOOKS_DIR, "savana-policies.properties");

    /**
     * Set the current working directory to the given directory.  All calls
     * to File.getAbsolutePath() made after the cd() will resolve relative
     * to the new current working directory.
     *
     * @param dir the directory to which we want to change
     */
    public static void cd(File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
    }


    /**
     * create a new temporary directory with the given name, deleting any existing directory with that name first.
     *
     * @param tempDirName the name of the temporary directory to create
     * @return the directory
     */
    public static File createTempDir(String tempDirName) {
        File dir = new File(TESTDATA_DIRECTORY, tempDirName);
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
