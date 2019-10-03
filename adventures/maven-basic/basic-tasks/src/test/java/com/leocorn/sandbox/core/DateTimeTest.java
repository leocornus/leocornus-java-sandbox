package com.leocorn.sandbox.core;

import java.time.LocalDateTime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for simple App.
 */
public class DateTimeTest
    extends TestCase
{

    // class name will the logger name.
    private static final Logger logger =
        LoggerFactory.getLogger(DateTimeTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DateTimeTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DateTimeTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testLocalDateTime() {

        LocalDateTime startTime = LocalDateTime.now();

        // try to run 1 minutes.
	    // run forever.
        //for(int index = 1; true; index++) {
        for(int index = 1; index < 10; index++) {
            // 
            logger.info("--> " + index);
            LocalDateTime stopTime = LocalDateTime.now().minusMinutes(1);
            int diff = startTime.compareTo(stopTime);
            logger.info("====> " + diff);
            if(diff < 0 ) {
                break;
            }
        }
    }
}
