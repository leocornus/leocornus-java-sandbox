package com.leocorn.sandbox.core;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class KafkaBasicTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public KafkaBasicTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( KafkaBasicTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testSendMessage() {

        Properties props = new Properties();

        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        Producer<String, String> producer = null;
        try {
          producer = new KafkaProducer(props);
          for (int i = 0; i < 100; i++) {
            String msg = "Message " + i;
            producer.send(new ProducerRecord<String, String>("test", msg));
            System.out.println("Sent:" + msg);
          }
        } catch (Exception e) {
          e.printStackTrace();
 
        } finally {
          producer.close();
        }
    }
}
