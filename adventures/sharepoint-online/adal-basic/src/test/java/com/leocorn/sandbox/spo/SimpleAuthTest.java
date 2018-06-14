package com.leocorn.sandbox.spo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import javax.naming.ServiceUnavailableException;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SimpleAuthTest extends TestCase {

    public SimpleAuthTest(String testName) {

        super(testName);
    }

    public static Test suite() {

        return new TestSuite(SimpleAuthTest.class);
    }

    public void testGetToken() {

        // load the config file.
        Properties conf = new Properties();
        String fileName = "conf/spo.properties";
        InputStream input = null;
        ExecutorService service = null;

        try {
            input = getClass().getClassLoader().getResourceAsStream(fileName);
            conf.load(input);

            assertEquals("sharepoint online", conf.getProperty("name"));

            AuthenticationResult result;
            AuthenticationContext context;

            // try to authenicate and acquire token.
            service = Executors.newFixedThreadPool(1);

            context = new AuthenticationContext(conf.getProperty("authority"),
                                                false, service);
            Future<AuthenticationResult> future = 
                context.acquireToken(conf.getProperty("target.source"),
                                     conf.getProperty("application.id"),
                                     conf.getProperty("username"),
                                     conf.getProperty("password"), null);
            result = future.get();
            // verify the token.
            assertNotNull(result);
            System.out.println(result.getAccessToken());
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ix){
            ix.printStackTrace();
        } catch (ExecutionException ix){
            ix.printStackTrace();
        } finally{
            if(input != null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // shutdown the executor!
            service.shutdown();
        }
    }
}
