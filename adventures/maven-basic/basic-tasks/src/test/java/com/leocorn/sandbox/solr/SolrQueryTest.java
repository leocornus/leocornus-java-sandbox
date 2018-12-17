package com.leocorn.sandbox.solr;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Arrays;
import java.util.Comparator;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.net.URL;
import java.net.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery;

import org.apache.solr.client.solrj.SolrServerException;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.xmp.XMPMetadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import com.leocorn.util.ConfigLoader;

/**
 * test some Solr query.
 */
public class SolrQueryTest extends TestCase {

    /**
     * the configuration file.
     */
    private Properties conf = new Properties();
    private Properties tikamap = new Properties();

    /**
     * Solr client object to talk to Solr.
     */
    private SolrClient solr;
    private SolrClient targetSolr;

    public SolrQueryTest (String testName) {

        super(testName);
        try {
            // load configuration file.
            conf = ConfigLoader.loadConfig(getClass().getClassLoader(),
                                           "conf/basic.properties", 
                                           "conf/local.properties");
            tikamap = ConfigLoader.loadConfig(getClass().getClassLoader(),
                                 "conf/tikamap.properties",
                                 "conf/local.tikamap.properties");

            // get ready the Solr client.
            String urlString = conf.getProperty("solr.baseurl");
            solr = new HttpSolrClient.Builder(urlString).build();

            targetSolr =
                new HttpSolrClient.Builder(conf.getProperty("solr.target.baseurl")).build();

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
    public void notestSimpleQuery() {

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

    /**
     * test case for a simple query.
     */
    public void notestQueryAndPost() {

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
                String id = (String) doc.getFieldValue("id");
                String sku = (String) doc.getFieldValue("sku");
                String filePath = (String) doc.getFieldValue("source_content_file_name");

                System.out.print("Id: " + id);
                System.out.print(", sku: " + sku);
                System.out.println(", File Path: " + filePath);
            }

        } catch(Exception sse) {

            sse.printStackTrace();
        }
    }

    public void notestIndexFileSolrJ() throws Exception {

        // manually set the file for quick testing.
        Metadata metadata = parseFile(conf.getProperty("test.http.input.stream.fileurl"));
        metadata.add("id", "a1E1I000000jXbaUAE");
        metadata.add("sku", "iso_011625_104556");
        indexFileSolrJ(targetSolr, metadata);
    }

    /**
     * index file using SolrJ.
     */
    private void indexFileSolrJ(SolrClient solrj, Metadata meta)
      throws IOException, SolrServerException {

        SolrInputDocument solrDoc = new SolrInputDocument();

        // process tika metadata
        String[] names = meta.names();
        for(int i = 0; i < names.length; i++) {

            String wellName = names[i].toLowerCase().trim().
                replaceAll(" ", "_").
                replaceAll("-", "_").
                replaceAll(":", "_");
            // check the tika metadata mapping configuration.
            String fieldName = tikamap.getProperty(wellName);
            // the == will jjjjjjjjj
            if(fieldName == null) {
                // no mapping! add the prefix.
                solrDoc.addField("tika_" + wellName, meta.get(names[i]));
            } else {
                if(fieldName.equals("IGNORE")) {
                    System.out.println("==== Ignore field: " + wellName);
                } else {
                    // we find the mapping metadata.
                    solrDoc.addField(fieldName, meta.get(names[i]));
                }
            }
        }

        solrj.add(solrDoc);
        solrj.commit();
    }

    /**
     * quick test case for parse file.
     */
    public void notestParseFile() throws Exception {

        Metadata metadata = parseFile(conf.getProperty("test.http.input.stream.fileurl"));

        // check the metadata.
        System.out.println("============== File Metadata =================");
        String[] names = metadata.names();
        // sort the names.
        // add the comparator to compare by the lower case.
        Arrays.sort(names, new Comparator<String>() {
            public int compare(String s1, String s2) {
                return s1.toLowerCase().compareTo(s2.toLowerCase());
            }
        });
        for(int i = 0; i < names.length; i ++) {
            // get ready to well formed name.
            String wellName = names[i].toLowerCase().trim().
                replaceAll(" ", "_").
                replaceAll("-", "_").
                replaceAll(":", "_");
            System.out.print(wellName + " = ");
            System.out.println(metadata.get(names[i]));;
        }
    }

    /**
     * using Tika to parse the file:
     * - extract the metadata and
     * - convert the structured file to text.
     */
    private Metadata parseFile(String fileUrl) throws Exception {

        // use the token to connect to SPO
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Metadata metadata = new Metadata();

        int httpResponseCode = conn.getResponseCode();
        if(httpResponseCode == 200) {

            String disposition = conn.getHeaderField("Content-Disposition");
            String contentType = conn.getContentType();
            long contentLength = conn.getContentLength();

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
            //System.out.println("file_size = " + size);
            //System.out.println("Array Size = " + baos.toByteArray().length);

            //System.out.println("Content-Type = " + contentType);
            //System.out.println("Content-Disposition = " + disposition);

            // ======================== MD5 Hash =============================
            // generate the MD5 hash for the file content.
            String digest = DigestUtils.md5Hex(new ByteArrayInputStream(baos.toByteArray()));
            //System.out.println("Digest = " + digest);

            // get the input stream from SPO connection.
            AutoDetectParser parser = new AutoDetectParser();
            // set the write limit to -1 to make is unlimited.
            BodyContentHandler handler = new BodyContentHandler(-1);
            // metadata is defined at beginning...
            parser.parse(new ByteArrayInputStream(baos.toByteArray()), handler, metadata);

            // ========================== add extra metadata.
            metadata.add("correct_file_size", String.valueOf(size));
            metadata.add("file_hash", digest);

            // the content in text format
            //System.out.println("=============== File Content =================");
            //System.out.println(handler.toString());
            metadata.add("file_content", handler.toString());
            metadata.add("file_content_size",
                         String.valueOf(handler.toString().length()));
            metadata.add("file_content_hash",
                         DigestUtils.md5Hex(handler.toString()));
            //System.out.println(handler.toString().length());

            // close the input stream.
            inputStream.close();

            //System.out.println("File Parsed!");

        } else {
            System.out.println(String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage()));
            System.out.println(fileUrl);
        }

        conn.disconnect();

        return metadata;
    }
}
