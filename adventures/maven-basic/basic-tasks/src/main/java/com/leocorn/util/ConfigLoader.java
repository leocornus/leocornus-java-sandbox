package com.leocorn.util;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * utility class to laod configuration files.
 */
public class ConfigLoader {

    /**
     * a utility method to load configuration files.
     */
    public static Properties loadConfig(ClassLoader classLoader, String baseFile, 
                                  String localFile) throws IOException {

        /**
         * file basic.properties will have the following content:
         */
        //String filename = "conf/spo.properties";
        //String localFilename = "conf/local.properties";
        Properties retConf = new Properties();
        InputStream input = null;

        try {
            // load the basic properties.
            input = classLoader.getResourceAsStream(baseFile);
            Properties basic = new Properties();
            basic.load(input);
            input.close();
            retConf.putAll(basic);

            // load the local properties.
            input = classLoader.getResourceAsStream(localFile);
            if(input != null) {
                Properties local = new Properties();
                local.load(input);
                input.close();
                retConf.putAll(local);
            } else {
                // null input stream means the file is not exist.
                // just skip it!
            }

            //assertEquals("sharepoint online", retConf.getProperty("name"));

            return retConf;
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
