package com.leocorn.sandbox.stream;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

import java.util.Properties;

import java.net.URL;
import java.net.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.codec.digest.DigestUtils;

import com.leocorn.util.ConfigLoader;

/**
 * test for the properties util class.
 */
public class InputStreamTest extends TestCase {

    /**
     * the configuration file.
     */
    private Properties conf = new Properties();

    public InputStreamTest (String testName) {

        super(testName);
        try {
            // load configuration file.
            conf = ConfigLoader.loadConfig(getClass().getClassLoader(),
                                           "conf/basic.properties", 
                                           "conf/local.properties");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {

        return new TestSuite(InputStreamTest.class);
    }

    /**
     * quick test to make properies are loaded.
     */
    public void testProperties() {

        assertEquals("localTwo", conf.getProperty("key.two"));
    }

    /**
     * test input stream from http url.
     */
    public void testHttpInputStream() throws Exception {

        String fileUrl = conf.getProperty("test.http.input.stream.fileurl");
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int httpResponseCode = conn.getResponseCode();

        if(httpResponseCode == 200) {

            // opens input stream from the HTTP connection
            InputStream inputStream = conn.getInputStream();

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream("/tmp/output.test.pdf");

            int bytesRead = -1;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        }
    }

    /**
     * test byte array input and output stream.
     */
    public void testByteArrayIO() throws Exception {

        String fileUrl = conf.getProperty("test.http.input.stream.fileurl");
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int httpResponseCode = conn.getResponseCode();

        if(httpResponseCode == 200) {
            // opens input stream from the HTTP connection
            InputStream inputStream = conn.getInputStream();
            //InputStream sizeIS = inputStream.clone();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // ======================== file size ===========================
            // calculate the size.
            int size = 0;
            int chunk = 0;
            byte[] buffer = new byte[1024];
            while((chunk = inputStream.read(buffer)) > -1){
                size += chunk;
                baos.write(buffer, 0, chunk);
            }
            baos.flush();
            System.out.println("file_size = " + size);
            //System.out.println("Array Size = " + baos.toByteArray().length);

            // ======================== MD5 Hash =============================
            // generate the MD5 hash for the file content.
            String digest = DigestUtils.md5Hex(new ByteArrayInputStream(baos.toByteArray()));
            System.out.println("file_hash = " + digest);

            inputStream.close();
        }
    }
}
