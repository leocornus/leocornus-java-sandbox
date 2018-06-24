package com.leocorn.sandbox.core;

import java.time.LocalDateTime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class DateTimeTest
    extends TestCase
{
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
        for(int index = 1; true; index++) {
            // 
            System.out.println("-->" + index);
            LocalDateTime stopTime = LocalDateTime.now().minusMinutes(1);
            int diff = startTime.compareTo(stopTime);
            System.out.println("====>" + diff);
            if(diff < 0 ) {
                break;
            }
        }
    }
}
