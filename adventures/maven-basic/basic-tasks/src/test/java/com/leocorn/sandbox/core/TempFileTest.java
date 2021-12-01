package com.leocorn.sandbox.core;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for simple App.
 *
 * execute using maven:
 * > mvn -Dtest=TempFileTest test
 */
public class TempFileTest extends TestCase
{

    // class name will the logger name.
    private static final Logger logger =
        LoggerFactory.getLogger(TempFileTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TempFileTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( TempFileTest.class );
    }

    /**
     * Quick test for the create temp file method.
     * the absolute path will provide the full path to the temp file created!
     */
    public void testCreateTempFile() throws IOException {

        File file = File.createTempFile("test-create-temp-file", ".xml");

        System.out.println("File name: " + file.getName());
        System.out.println("File absolute path: " + file.getAbsolutePath());
    }
}
