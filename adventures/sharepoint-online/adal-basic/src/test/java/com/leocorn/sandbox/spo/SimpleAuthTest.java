package com.leocorn.sandbox.spo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.codec.digest.DigestUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;

import java.util.Properties;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;

import java.time.LocalDateTime;

import javax.naming.ServiceUnavailableException;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStreamBase;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.xmp.XMPMetadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SimpleAuthTest extends TestCase {

    /**
     * the configuration file.
     */
    private Properties conf = new Properties();
    private Properties fmap = new Properties();
    private Properties tikamap = new Properties();

    /**
     * Solr client object to talk to Solr.
     */
    private SolrClient solr;

    /**
     * folder count.
     */
    private int folderCount = 0;

    public SimpleAuthTest(String testName) {

        super(testName);
        try {
            // load configuration file.
            conf = loadConfig("conf/spo.properties", "conf/local.properties");
            fmap = loadConfig("conf/fmap.properties", 
                              "conf/local.fmap.properties");
            tikamap = loadConfig("conf/tikamap.properties", 
                                 "conf/local.tikamap.properties");
            //System.out.println(fmap);

            // get ready the Solr client.
            String urlString = conf.getProperty("solr.baseurl");
            solr = new HttpSolrClient.Builder(urlString).build();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static Test suite() {

        return new TestSuite(SimpleAuthTest.class);
    }

    /**
     * quick test for iteration.
     */
    public void testIteration() throws Exception {

        //String token = getAuthResult().getAccessToken();
        // starts from group folder.
        //processFolder("Customer Group A");
        String[] folders = conf.getProperty("start.folder").split("==");
        for(int i = 0; i < folders.length; i++) {
            processFolder(folders[i]);
        }

        System.out.println("Total Folders" + folderCount);
    }

    private String accessToken = "";
    private LocalDateTime tokenTimestamp = null;

    /**
     * process one folder.
     */
    public void processFolder(String folderName) 
        throws Exception {

        folderCount ++;

        // get accessToken 
        if(accessToken.isEmpty()) {
            accessToken = getAuthResult().getAccessToken();
            tokenTimestamp = LocalDateTime.now();
        } else {
            LocalDateTime thirtyMins = LocalDateTime.now().minusMinutes(30);
            int age = tokenTimestamp.compareTo(thirtyMins);
            if(age < 0) {
                // crate new access token.
                accessToken = getAuthResult().getAccessToken();
                tokenTimestamp = LocalDateTime.now();
            }
        }

        // build folderUrl.
        String folderUrl = conf.getProperty("target.source") + 
                           conf.getProperty("sharepoint.site") + 
                           "/_api/web/GetFolderByServerRelativeUrl('" +
            URLEncoder.encode(folderName, "utf-8").replace("+", "%20") + "')";

        // STEP One: process files in this folder.
        //indexFiles(accessToken, folderUrl, "solrcell");
        indexFiles(accessToken, folderUrl, "solrj");

        // STEP Two: Process folders in this folder.
        // get all Folders
        String res = getResponse(accessToken, folderUrl + "/Folders");
        if(res == null) {
            // no folders, skip.
            // if no res, most of time are because of WRONG url.
            return;
        }
        JSONObject json = new JSONObject(res);
        // if has Folders, process each folder by call it self.
        JSONArray jsonArray = json.getJSONArray("value");
        // logging...
        System.out.println("== Processing " + jsonArray.length() + " Folders");
        for (int index = 0; index < jsonArray.length(); index++) {
            // get the folder name.
            JSONObject oneFolder = jsonArray.getJSONObject(index);
            String subFolderName = oneFolder.getString("Name");
            // call it self.
            processFolder(folderName + "/" + subFolderName);
        }
    }

    /**
     * download the ingest files into solr.
     */
    private void indexFiles(String accessToken, 
                            String folderUrl,
                            String indexType) throws Exception {

        // TODO: check the folder name!
        // we only process Certificate and Report folder.
        if(folderUrl.indexOf("Certificate") < 0 &&
           folderUrl.indexOf("Report") < 0 &&
           folderUrl.indexOf("Test") < 0) {

            System.out.println("None Ceritificate and Report Folder, Skip...");
            return;
        }

        // get Files.
        String res = getResponse(accessToken, folderUrl + "/Files");
        if(res == null) {
            // no files, skip.
            // if no res, most of time are because of WRONG url.
            return;
        }
        JSONObject json = new JSONObject(res);
        // If has Files, download all files 
        JSONArray filesArray = json.getJSONArray("value");
        // logging...
        System.out.println("==== Downloading " + filesArray.length() + " Files");
        for (int index = 0; index < filesArray.length(); index++) {

            // one file 
            JSONObject oneFile = filesArray.getJSONObject(index);
            String fileName = "NONAME";
            if(oneFile.has("Title") && !oneFile.isNull("Title")) {
                fileName = oneFile.getString("Title");
            } else {
                System.out.println("Could not find file name, skip ...");
                continue;
            }
            if(fileName == "") {
                System.out.println("File name is empty, skip ...");
            }
            // odata.id will have the full URL.
            //String fileUrl = oneItem.getString("odata.id");
            String encodedFileName = 
                URLEncoder.encode(fileName, "utf-8").replace("+", "%20");

            // we need get the metadata for the file.
            String propertyUrl = 
                folderUrl + "/Files('" + encodedFileName + "')/Properties";
            Map props = getProperties(accessToken, propertyUrl);
            System.out.println(props);

            // get ready the URL for download binary.
            String fileUrl = 
                folderUrl + "/Files('" + encodedFileName + "')/$value";

            if(indexType == "solrcell") {
                //System.out.println(fileUrl);
                // the file path to local 
                String filePath = downloadFile(accessToken, fileUrl);

                if(filePath == null || props.isEmpty()) {
                    System.out.println("No file downloaded! Skip ...");
                } else {

                    try {
                        // update Solr to index this file. SolrJ
                        indexFileSolrCell(filePath, props);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    // using Tika and SolrJ
                    // parse file and convert file content.
                    Metadata meta = parseFile(accessToken, fileUrl);
                    // call SolrJ
                    indexFileSolrJ(meta, props);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * test to get metadata SP.PropertyValues for a file.
     */
    public void notestGetProperties() throws Exception {

        String token = getAuthResult().getAccessToken();

        // view a file properties, which will have all metadata.
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Files('0000125314_QIP_0000157406.pdf')/Properties";
        String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer Group A/A.O. Smith Corporation - 0004514004/0002272679/Test Results and Data')/Files('2186447 TEST ALT FLEX CONN.pdf')/Properties";
        String apiUrl = conf.getProperty("target.source") + 
                        conf.getProperty("sharepoint.site") + 
                        URLEncoder.encode(apiUri, "utf-8").replace("+", "%20");
        System.out.println(apiUrl);
        Map props = getProperties(token, apiUrl);
        // Returns Set view
        Set< Map.Entry< String,Integer> > st = props.entrySet();   

        for (Map.Entry< String,Integer> me:st) {
            System.out.print(me.getKey()+": ");
            System.out.println(me.getValue());
        }
    }

    /**
     */
    private Map getProperties(String token, String propertyUrl) 
        throws Exception {

        System.out.println(propertyUrl);

        String res = getResponse(token, propertyUrl);
        Map props = new HashMap();
        if(res == null) {
            return props;
        }
        JSONObject json = new JSONObject(res);
        //System.out.println(json.toString(2));

        if(json.has("CustomerNumber")) {
            props.put("customer_id", json.getString("CustomerNumber"));
        } else {
            props.put("customer_id", "1");
        }
        if(json.has("CustomerName")) {
            props.put("customer_name", json.getString("CustomerName"));
        } else {
            props.put("customer_name", "NONAME");
        }
        if(json.has("ProjectID")) {
            props.put("project_id", json.getString("ProjectID"));
        } else {
            props.put("project_id", "0");
        }
        if(json.has("ProjectStatus")) {
            props.put("project_status", json.getString("ProjectStatus"));
        }
        if(json.has("CertificateNumber")) {
            try {
                props.put("certificate_id", String.valueOf(json.getInt("CertificateNumber")));
            } catch (JSONException je) {
                props.put("certificate_id", json.getString("CertificateNumber"));
            }
        } else {
            props.put("certificate_id", "0");
        }
        if(json.has("MasterContractNumber")) {
            props.put("master_contract_number", json.getString("MasterContractNumber"));
        } else {
            props.put("master_contract_number", "0");
        }
        if(json.has("Order")) {
            props.put("project_order", json.getString("Order"));
        }
        if(json.has("SecurityClassification")) {
            props.put("security_classification", json.getString("SecurityClassification"));
        }
        // TODO: parse this to extract folder names.
        // we only care about customer group and customer foler.
        String fullUrl = json.getString("odata.id");
        props.put("odata_id", fullUrl.substring(0, fullUrl.indexOf("/Properties")));
        String folder = extractFunctionValue(fullUrl, "GetFolderByServerRelativeUrl");
        String[] folders = folder.split("/");
        String fileName = extractFunctionValue(fullUrl, "Files");
        props.put("folder_customer_group", folders[0]);
        props.put("folder_customer", folders[1]);
        props.put("file_name", fileName);
        // the file extension.
        props.put("file_extension",
                  fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase());
        props.put("file_path", folder + "/" + fileName);
        String spoId = "00000000";
        String docId = fileName;
        if(json.has("OData__x005f_dlc_x005f_DocId")) {
            spoId = json.getString("OData__x005f_dlc_x005f_DocId");
            docId = spoId;
        } 
        int projectId = 0;
        try {
            projectId = Integer.parseInt(json.getString("ProjectID"));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        props.put("file_spo_id", spoId);
        // set up c4c_type.
        if(folder.indexOf("Certificate") > 0) {
            props.put("c4c_type", "certificate");
            props.put("id", "c|" + projectId + "|" + docId);
        } else if(folder.indexOf("Report") > 0 || folder.indexOf("Test") > 0) {
            props.put("c4c_type", "test_report");
            props.put("id", "t|" + projectId + "|" + docId);
        } else {
            props.put("c4c_type", "other");
            props.put("id", "o|" + projectId + "|" + docId);
        }

        System.out.println(props);
        return props;
    }

    /**
     * test the extractFunctionValue
     */
    public void notestExtractFunctionValue() throws Exception {

        String fromUrl = "https://csagrporg.sharepoint.com/sites/QADocBoxMig/_api/web/GetFolderByServerRelativeUrl('Customer Group K/Karl Dungs Inc - 0004507796/000070008273')/Files('0000125314_QIP_0000157406.pdf')/Properties";
        String folder = extractFunctionValue(fromUrl, "GetFolderByServerRelativeUrl");
        System.out.println(folder);
        assertEquals(folder, "Customer Group K/Karl Dungs Inc - 0004507796/000070008273");

        String file = extractFunctionValue(fromUrl, "Files");
        assertEquals(file, "0000125314_QIP_0000157406.pdf");
    }

    /**
     * utility method to extract function value
     */
    private String extractFunctionValue(String from, String functionName) throws Exception {

        int start = from.indexOf(functionName + "('");
        int end = from.indexOf("')", start);
        return from.substring(start + functionName.length() + 2, end);
    }

    /**
     * quick test to download a single file.
     */
    public void notestDownloadAFile() throws Exception {

        String token = getAuthResult().getAccessToken();
        // view a file properties, which will have all metadata.
        String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Files('0000125314_QIP_0000157406.pdf')/$value";
        String apiUrl = conf.getProperty("target.source") + 
                        conf.getProperty("sharepoint.site") + apiUri;
        System.out.println(apiUrl);

        downloadFile(token, apiUrl);
    }

    /**
     * quick test to list files.
     */
    public void notestListFiles() throws Exception {

        String token = getAuthResult().getAccessToken();
        // view a file properties, which will have all metadata.
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Files('0000125314_QIP_0000157406.pdf')/Properties";
        // download a file.
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Files('0000125314_QIP_0000157406.pdf')/$value";
        // /0000125314_QIP_0000157406.pdf')";
        // list of files.
        String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K')/Folders";
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273/Work%20Orders')/Files";
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Files";
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Folders";
        // list of folders.
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796')/folders";
        // get metadata for a fodler.
        //String apiUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796')";

        String apiUrl = conf.getProperty("target.source") + 
                        conf.getProperty("sharepoint.site") + apiUri;
        System.out.println(apiUrl);

        String res = getResponse(token, apiUrl);
        //System.out.println(res);
        JSONObject json = new JSONObject(res);
        // print out the JSON with 2 white spaces as indention.
        System.out.println(json.toString(2));
        //System.out.println(json.getString("odata.metadata"));
        JSONArray jsonArray = json.getJSONArray("value");
        for (int index = 0; index < jsonArray.length(); index++) {

            // one item.
            JSONObject oneItem = jsonArray.getJSONObject(index);

            // odata.type tells the
            String type = oneItem.getString("odata.type");
            if (type.equals("SP.Folder")) {
                System.out.println(oneItem.getString("Name"));
            } else {
                // the field name is case sensitive!
                System.out.println(oneItem.getString("Title"));
            }
            String fileName = oneItem.getString("Title");
            // odata.id will have the full URL.
            //String fileUrl = oneItem.getString("odata.id");
            String fileUrl = 
                apiUrl + "('" + URLEncoder.encode(fileName, "utf-8").replace("+", "%20") +
                             "')/$value";
            //System.out.println(fileUrl);
            downloadFile(token, fileUrl);
        }
    }

    /**
     * return the full path for the saved file.
     * if failed to download, we will return Null.
     */
    private String downloadFile(String token, String fileUrl) throws Exception {

        URL url = new URL(fileUrl); 
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept","application/json;odata=verbose;");
        //conn.setRequestProperty("Accept","application/json;");
        //conn.setRequestProperty("ContentType","application/json;odata=verbose;");
        //conn.connect();

        //String saveDir = "/opt/dev/spo-files";
        String saveDir = conf.getProperty("local.temp.folder");
        String saveFilePath = null;

        int httpResponseCode = conn.getResponseCode();
        if(httpResponseCode == 200) {
            // try to find the file name.
            String fileName = "default";
            String disposition = conn.getHeaderField("Content-Disposition");
            String contentType = conn.getContentType();
            long contentLength = conn.getContentLength();
 
            // extracts file name from URL
            fileName = URLDecoder.decode(
                fileUrl.substring(fileUrl.lastIndexOf("Files('") + 7,
                                  fileUrl.lastIndexOf("')/$value")),
                "UTF-8");
            // opens input stream from the HTTP connection
            InputStream inputStream = conn.getInputStream();

            if(contentLength < 0) {
                contentLength = inputStream.available();
            }

            saveFilePath = saveDir + File.separator + fileName;
 
            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);
            System.out.println("localFile = " + saveFilePath);

            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.close();
            inputStream.close();
 
            System.out.println("File downloaded");

        } else {
            System.out.println(String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage()));
            System.out.println(fileUrl);
        }

        conn.disconnect();

        return saveFilePath;
    }

    /**
     * quick test to download a single file.
     */
    public void notestParseFile() throws Exception {

        String token = getAuthResult().getAccessToken();
        // view a file properties, which will have all metadata.
        // large PDF
        //String folder = "Customer Group L/LG Electronics, Inc. - 0004519978/0002599597/Shared Documents/Report Attachments";
        //String file = "1957460 Att 1 Fig. 1 - 34.pdf";

        //String folder = "Customer Group K/Karl Dungs Inc - 0004507796/000070008273";
        //String file = "e125314 - CLE378 - Karl Dungs - More Info Page.xlsm";
        //String file = "0000125314_QIP_0000157406.pdf";

        // doc
        //String folder = "Customer Group F/Fluke Corporation - 0004518553/000070059497/Shared Documents/Report Attachments";
        //String file = "2022012 Att2_IEC_61010-1_3rd_Ed_Checklist.doc";

        // large DOC
        //String folder = "Customer Group F/Fluke Corporation - 0004518553/0002460695/Test Results and Data";
        //String file = "2460695 UL 2054 test data 2010-11-22.doc";

        // large tif file.
        String folder = "Customer Group H/Hubbell Incorporated (Delaware) - 0004722791/0002123482/Test Results and Data";
        String file = "UL_PROJECT_07ME12446_DR20TR.tif";

        String apiUri = "/_api/web/GetFolderByServerRelativeUrl('" + 
                    URLEncoder.encode(folder, "utf-8").replace("+", "%20") +
                    "')/Files('" + 
                    URLEncoder.encode(file, "utf-8").replace("+", "%20") +
                    "')/$value";

        String apiUrl = conf.getProperty("target.source") +
                        conf.getProperty("sharepoint.site") + apiUri;
        System.out.println(apiUrl);

        parseFile(token, apiUrl);
    }

    /**
     * using Tika to parse the file:
     * - extract the metadata and 
     * - convert the structured file to text.
     */
    private Metadata parseFile(String token, String fileUrl) throws Exception {

        // use the token to connect to SPO
        URL url = new URL(fileUrl); 
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept","application/json;odata=verbose;");
        //conn.setRequestProperty("Accept","application/json;");
        //conn.setRequestProperty("ContentType","application/json;odata=verbose;");
        //conn.connect();

        //XMPMetadata.registerNamespace( DublinCore.NAMESPACE_URI_DC_TERMS,
        //                DublinCore.PREFIX_DC_TERMS );
        //XMPMetadata metadata = new XMPMetadata();
        Metadata metadata = new Metadata();

        int httpResponseCode = conn.getResponseCode();
        if(httpResponseCode == 200) {
            // try to find the file name.
            String fileName = "default";
            String disposition = conn.getHeaderField("Content-Disposition");
            String contentType = conn.getContentType();
            long contentLength = conn.getContentLength();
 
            // extracts file name from URL
            fileName = URLDecoder.decode(
                fileUrl.substring(fileUrl.lastIndexOf("Files('") + 7,
                                  fileUrl.lastIndexOf("')/$value")),
                "UTF-8");
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
            System.out.println("Array Size = " + baos.toByteArray().length);

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("fileName = " + fileName);

            // ======================== MD5 Hash =============================
            // generate the MD5 hash for the file content.
            String digest = DigestUtils.md5Hex(new ByteArrayInputStream(baos.toByteArray()));
            System.out.println("Digest = " + digest);

            // get the input stream from SPO connection.
            AutoDetectParser parser = new AutoDetectParser();
            // set the write limit to -1 to make is unlimited.
            BodyContentHandler handler = new BodyContentHandler(-1);
            // metadata is defined at beginning...
            parser.parse(new ByteArrayInputStream(baos.toByteArray()), handler, metadata);

            // ========================== add extra metadata.
            metadata.add("correct_file_size", String.valueOf(size));
            metadata.add("file_hash", digest);

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

            // the content in text format 
            System.out.println("=============== File Content =================");
            //System.out.println(handler.toString());
            metadata.add("file_content", handler.toString());
            metadata.add("file_content_size", 
                         String.valueOf(handler.toString().length()));
            metadata.add("file_content_hash",
                         DigestUtils.md5Hex(handler.toString()));
            System.out.println(handler.toString().length());

            // close the input stream.
            inputStream.close();
 
            System.out.println("File Parsed!");

        } else {
            System.out.println(String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage()));
            System.out.println(fileUrl);
        }

        conn.disconnect();

        return metadata;
    }

    /**
     * get response from the given URL by using the access token.
     * The response will be returned as it is.
     */
    private String getResponse(String token, String apiUrl) throws Exception {

        URL url = new URL(apiUrl); 
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        //conn.setRequestProperty("Accept","application/json;odata=verbose;");
        conn.setRequestProperty("Accept","application/json;");
        //conn.setRequestProperty("ContentType","application/json;odata=verbose;");
        //conn.connect();

        int httpResponseCode = conn.getResponseCode();
        if(httpResponseCode == 200) {
            BufferedReader in = null;
            StringBuilder response;
            try{
                in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            } finally {
                in.close();
            }
        } else {
            System.out.println(String.format("Connection returned HTTP code: %s with message: %s",
                    httpResponseCode, conn.getResponseMessage()));
            return null;
        }
    }

    /**
     * simple test case to login and get access token.
     */
    public void notestGetToken() {

        AuthenticationResult result = getAuthResult();
        assertNotNull(result);
        System.out.println(result.getAccessToken());
    }

    private AuthenticationResult getAuthResult() {

        // load the config file.
        ExecutorService service = null;
        AuthenticationResult result = null;

        try {

            assertEquals("sharepoint online", conf.getProperty("name"));

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
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ix){
            ix.printStackTrace();
        } catch (ExecutionException ix){
            ix.printStackTrace();
        } finally{
            // shutdown the executor!
            service.shutdown();
        }

        return result;
    }

    /**
     * test the index files
     */
    public void notestIndexFilesSolrCell() throws Exception {

        String token = getAuthResult().getAccessToken();
        // view a file properties, which will have all metadata.
        //String fileUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Karl%20Dungs%20Inc%20-%200004507796/000070008273')/Files('0000125314_QIP_0000157406.pdf')";
        String fileUri = "/_api/web/GetFolderByServerRelativeUrl('Customer%20Group%20K/Kiwa-Gastec%20Certification%20-%200004728601/0002326081')/Files('DSCN1097.JPG')";

        // get metadata for the file.
        String propertiesUrl = conf.getProperty("target.source") + 
                        conf.getProperty("sharepoint.site") + 
                        fileUri + "/Properties";
        Map props = getProperties(token, propertiesUrl);
        System.out.println(props);

        // download the file.
        String downloadUrl = conf.getProperty("target.source") + 
                        conf.getProperty("sharepoint.site") + 
                        fileUri + "/$value";
        System.out.println(downloadUrl);
        String localFile = downloadFile(token, downloadUrl);

        indexFileSolrCell(localFile, props);
    }

    /**
     * index file using ExtractingRequestHandler.
     */
    private void indexFileSolrCell(String fileName, Map props) 
      throws IOException, SolrServerException {
      
        ContentStreamUpdateRequest up 
          = new ContentStreamUpdateRequest("/update/extract");

        up.addContentStream(new ContentStreamBase.FileStream(new File(fileName)));

        Set< Map.Entry< String,String> > st = props.entrySet();   

        for (Map.Entry< String,String> me:st) {
            System.out.print(me.getKey()+": ");
            System.out.println(me.getValue());
            up.setParam("literal." + me.getKey(), me.getValue());
            if(me.getValue().trim() == "") {
                up.setParam("literal.category", "no_" + me.getKey());
            }
        }

        //up.setParam("literal.id", solrId);
        //up.setParam("fmap.content", "file_content");
        //up.setParam("fmap.stream_size", "file_size");
        //up.setParam("fmap.content_type", "file_content_type");
        //up.setParam("fmap.doc_type", "file_doc_type");
        //up.setParam("fmap.dcterms_created", "file_created_date");
        //up.setParam("fmap.last_modified", "file_last_modified");
        //up.setParam("uprefix", "attr_");
        //up.setParam("uprefix", "ignored_");

        Enumeration<?> fmapNames = fmap.propertyNames();
        while(fmapNames.hasMoreElements()) {
            String fmapName = (String) fmapNames.nextElement();
            up.setParam(fmapName, fmap.getProperty(fmapName));
        }

        up.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

        solr.request(up);
    }

    /**
     * test index file using SolrJ
     */
    public void notestIndexFileSolrJ() throws Exception {

        String token = getAuthResult().getAccessToken();
        // view a file properties, which will have all metadata.
        // large PDF
        //String folder = "Customer Group L/LG Electronics, Inc. - 0004519978/0002599597/Shared Documents/Report Attachments";
        //String file = "1957460 Att 1 Fig. 1 - 34.pdf";

        //String folder = "Customer Group K/Karl Dungs Inc - 0004507796/000070008273";
        //String file = "e125314 - CLE378 - Karl Dungs - More Info Page.xlsm";
        //String file = "0000125314_QIP_0000157406.pdf";

        // doc
        //String folder = "Customer Group F/Fluke Corporation - 0004518553/000070059497/Shared Documents/Report Attachments";
        //String file = "2022012 Att2_IEC_61010-1_3rd_Ed_Checklist.doc";

        // large DOC
        //String folder = "Customer Group F/Fluke Corporation - 0004518553/0002460695/Test Results and Data";
        String folder = "Customer Group F/Fluke Corporation - 0004518553/0002579396/Test Results and Data";
        String file = "2460695 UL 2054 test data 2010-11-22.doc";

        String apiUri = "/_api/web/GetFolderByServerRelativeUrl('" + 
                    URLEncoder.encode(folder, "utf-8").replace("+", "%20") +
                    "')/Files('" + 
                    URLEncoder.encode(file, "utf-8").replace("+", "%20") +
                    "')/";

        String fileUrl = conf.getProperty("target.source") +
                        conf.getProperty("sharepoint.site") + apiUri + 
                        "$value";
        System.out.println(fileUrl);
        // get tika metadata.
        Metadata meta = parseFile(token, fileUrl);

        String propUrl = conf.getProperty("target.source") +
                        conf.getProperty("sharepoint.site") + apiUri + 
                        "Properties";
        Map props = getProperties(token, propUrl);

        indexFileSolrJ(meta, props);
    }

    /**
     * index file using ExtractingRequestHandler.
     */
    private void indexFileSolrJ(Metadata meta, Map props) 
      throws IOException, SolrServerException {

        SolrInputDocument solrDoc = new SolrInputDocument();

        // add properties, from the SPO metadata.
        Set< Map.Entry< String,String> > st = props.entrySet();   

        for (Map.Entry< String,String> me:st) {
            System.out.print(me.getKey()+": ");
            System.out.println(me.getValue());
            solrDoc.addField(me.getKey(), me.getValue());
            if(me.getValue().trim() == "") {
                solrDoc.addField("category", "no_" + me.getKey());
            }
        }

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

        solr.add(solrDoc);
        solr.commit();
    }

    /**
     * a utility method to load configuration files.
     */
    private Properties loadConfig(String baseFile, 
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
            input = getClass().getClassLoader().getResourceAsStream(baseFile);
            Properties basic = new Properties();
            basic.load(input);
            input.close();
            retConf.putAll(basic);

            // load the local properties.
            input = getClass().getClassLoader().getResourceAsStream(localFile);
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
