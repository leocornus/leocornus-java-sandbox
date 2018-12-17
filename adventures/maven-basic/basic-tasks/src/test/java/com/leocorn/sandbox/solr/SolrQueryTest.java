package com.leocorn.sandbox.solr;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery;

import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.leocorn.util.ConfigLoader;

/**
 * test some Solr query.
 */
public class SolrQueryTest extends TestCase {

    /**
     * the configuration file.
     */
    private Properties conf = new Properties();

    /**
     * Solr client object to talk to Solr.
     */
    private SolrClient solr;

    public SolrQueryTest (String testName) {

        super(testName);
        try {
            // load configuration file.
            conf = ConfigLoader.loadConfig(getClass().getClassLoader(),
                                           "conf/basic.properties", 
                                           "conf/local.properties");

            // get ready the Solr client.
            String urlString = conf.getProperty("solr.baseurl");
            solr = new HttpSolrClient.Builder(urlString).build();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {

        return new TestSuite(SolrQueryTest.class);
    }

    /**
     * test case for a simple query.
     */
    public void testSimpleQuery() {

        final Map<String, String> queryParamMap = new HashMap<String, String>();

        queryParamMap.put("q", conf.getProperty("solr.simple.query"));
        System.out.println("Query: " + conf.getProperty("solr.simple.query"));
        //queryParamMap.put("fl", "id,version_schema");
        queryParamMap.put("rows", conf.getProperty("solr.simple.query.rows"));
        queryParamMap.put("sort", conf.getProperty("solr.simple.query.sort"));

        MapSolrParams queryParams = new MapSolrParams(queryParamMap);

        try {
            final QueryResponse response = solr.query(queryParams);
            final SolrDocumentList documents = response.getResults();

            System.out.println("Total Docs: " + documents.getNumFound());
            System.out.println("Documents returned: " + documents.size());

            for(int i = 0; i < documents.size(); i ++) {

                SolrDocument doc = (SolrDocument) documents.get(i);
                System.out.print("Id: " + (String) doc.getFieldValue("id"));
                System.out.println(", File Path: " +
                                   (String) doc.getFieldValue("source_content_file_name"));
            }

        } catch(Exception sse) {

            sse.printStackTrace();
        }
    }
}
