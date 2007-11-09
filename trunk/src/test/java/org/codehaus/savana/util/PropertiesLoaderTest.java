package org.codehaus.savana.util;

import junit.framework.TestCase;

import java.util.Properties;

public class PropertiesLoaderTest extends TestCase {

    public void testSuccess() throws Exception {
        // load a properties file that exists
        Properties properties = PropertiesLoader.getInstance().getProperties(
                "org/codehaus/savana/util/success.properties");

        // check that the properties we set have the correct values, and properties we
        // didn't set are null
        assertEquals("bar", properties.getProperty("foo"));
        assertEquals("delicious", properties.getProperty("chocolate"));
        assertEquals(null, properties.getProperty("monkeys"));
    }

    public void testMissingPropertiesFile() throws Exception {
        // check that a missing properties file returns null
        assertEquals(null, PropertiesLoader.getInstance().getProperties("failure.properties"));
    }
}
