package org.codehaus.savana.scripts;

public class ListChangesFromTrunkTest extends AbstractSavanaScriptsTestCase {

    /**
     * tests the case where "list changes from source" is run in trunk.
     * <p/>
     * This scenario is always erroneous, so this unit test checks for the appropriate exception to be thrown from
     * the savana command.
     *
     * @throws Exception on error
     */
    public void testListChangesFromTrunk() throws Exception {
        // setup a test project with a working directory and import the 'test-project' files
        String projectName = getClass().getSimpleName().toLowerCase();
        TestRepoUtil.setupProjectWithWC(TestRepoUtil.DEFAULT_REPO, projectName, true, true, null);

        try {
            // ListChangesFromSource from trunk should never work
            savana(ListChangesFromSource.class);
            assertTrue("we expected an exception to be thrown", false);

        } catch (SavanaScriptsTestException e) {
            // we expect this exception to be thrown, with this error message
            assertEquals("svn: Error: No source path found (you are probably in the TRUNK).\n", e.getErr());
        }
    }

}