package com.leocorn.sandbox.util;

import java.io.InputStream;
import java.io.IOException;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * test for the properties util class.
 */
public class PropertiesTest extends TestCase {

    public PropertiesTest(String testName) {

        super(testName);
    }

    public static Test suite() {

        return new TestSuite(PropertiesTest.class);
    }

    // all methods start with test will be executed.

    /**
     * unit test to read content from a properties file.
     * we weill test the key and value pairs from the properties file.
     * This post has some simple and easy samples.
     * - http://www.mkyong.com/java/java-properties-file-examples/
     */
    public void testReadConfig() {

        // we will read the properties file from classpath.
        String filename = "conf/basic.properties";
        Properties conf = new Properties();
        InputStream input = null;

        try {
            input = getClass().getClassLoader().getResourceAsStream(filename);
            conf.load(input);

            System.out.println(conf.getProperty("keyOne"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally{
            if(input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
