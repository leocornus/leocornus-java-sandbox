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
        // it will be stored in folder
        // - src/main/resources
        String filename = "conf/basic.properties";
        Properties conf = new Properties();
        InputStream input = null;

        try {
            input = getClass().getClassLoader().getResourceAsStream(filename);
            conf.load(input);

            //System.out.println(conf.getProperty("keyOne"));
            assertEquals("valueOne", conf.getProperty("keyOne"));
            assertEquals("valueTwo", conf.getProperty("key.two"));
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

    /**
     * test the merge function for configuration files.
     * The simple case is we will use local.properties to overwrite the 
     * values in all other properties files.
     */
    public void testMergeConfig() {

        /**
         * file basic.properties will have the following content:
         *
         * keyOne = valueOne
         * key.two = valueTwo
         */
        String filename = "conf/basic.properties";
        /**
         * file local.properties will have the following content:
         *
         * key.two = localTwo
         *
         * we will not save local.properties in the Git repository.
         */
        String localFilename = "conf/local.properties";
        Properties conf = new Properties();
        InputStream input = null;

        try {
            // load the basic properties.
            input = getClass().getClassLoader().getResourceAsStream(filename);
            Properties basic = new Properties();
            basic.load(input);
            input.close();
            conf.putAll(basic);

            // verify
            assertEquals("valueTwo", conf.getProperty("key.two"));

            // load the local properties.
            input = getClass().getClassLoader().getResourceAsStream(localFilename);
            if(input != null) {
                Properties local = new Properties();
                local.load(input);
                input.close();
                conf.putAll(local);
                // verify.
                assertEquals("localTwo", conf.getProperty("key.two"));
            } else {
                // null input stream means the file is not exist.
                // just skip it!
            }
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
