/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.spyhunter99.nugetparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author AO
 */
public class NugetCacher {

    public static void main(String[] args) throws Exception {
        //args = new String[]{"jquery"};
        if (args == null || args.length == 0) {
            System.out.println("java -jar NugetCacher-<VERSION>-jar (artifact)");
            System.out.println("Example: java -jar NugetCacher-<VERSION>-jar jquery");
            System.out.println("Example: java -jar NugetCacher-<VERSION>-jar audit.wcf");
        } else {
            new NugetCacher().run(args[0]);
        }
    }
    LinkedHashSet<String> packages = new LinkedHashSet<>();
    LinkedHashSet<String> jsons = new LinkedHashSet<>();
    Set<String> urlsDownloadedThisSession = new HashSet<>();

    private void run(String url) throws Exception {

        //TODO start with seed dependency. Package name must be toLower vs wha nuget has in the user interface
        //download and parse all json files
        //download all packages all versions
        String seed = "https://api.nuget.org/v3/registration1-gz/" + url + "/index.json";
        jsons.add(seed);

        while (!jsons.isEmpty()) {
            if (jsons.iterator().hasNext()) {
                String jsonUrl = jsons.iterator().next();
                if (!urlsDownloadedThisSession.contains(jsonUrl)) {
                    downloadAndParse(jsonUrl);
                    urlsDownloadedThisSession.add(jsonUrl);
                }
                jsons.remove(jsonUrl);
            }
        }
        System.out.println("Downloading packages " + packages.size());
        Iterator<String> iterator = packages.iterator();
        while (iterator.hasNext()) {
            download(iterator.next(), false);
        }
    }

    private void downloadAndParse(String url) throws Exception {

        System.out.println("Downloading JSON " + url);
        URL urllocal = new URL(url);
        String outputLocation = "output/" + urllocal.getPath();

        File output = new File(outputLocation);
        output.getParentFile().mkdirs();

        CloseableHttpClient client = HttpClients.custom()
                .disableContentCompression()
                .build();

        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");

        CloseableHttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();

        boolean isGzipped = false;
        Header firstHeader = response.getFirstHeader(HttpHeaders.CONTENT_ENCODING);
        if (firstHeader != null) {
            if ("gzip".equalsIgnoreCase(firstHeader.getValue())) {
                isGzipped = true;
            }
        } else {

        }
        InputStream content = entity.getContent();
        BufferedReader rd = null;
        if (isGzipped) {
            GZIPInputStream gzis = new GZIPInputStream(content);
            rd = new BufferedReader(new InputStreamReader(gzis, Charset.forName("UTF-8")));
        } else {

            rd = new BufferedReader(new InputStreamReader(content, Charset.forName("UTF-8")));
        }

        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }

        content.close();
        //gzis.close();
        client.close();

        String outputContent = sb.toString();
        FileUtils.writeStringToFile(output, outputContent, "UTF-8");

        System.out.println("Parsing JSON " + url);
        JSONObject jsonObj = new JSONObject(outputContent);

        for (Object key : jsonObj.keySet()) {
            String keyStr = (String) key;
            Object keyvalue = jsonObj.get(keyStr);
            if ("items".equalsIgnoreCase(keyStr)) {
                processItems(keyvalue);
            }
        }

    }

    private void walkTree(JSONObject obj) {
        if (obj.has("registration")) {
            System.out.println(obj.get("registration"));
        } else {
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object o = obj.get(key);
                if (o instanceof JSONObject) {
                    walkTree(obj);
                } else if (o instanceof JSONArray) {
                    JSONArray array = (JSONArray) o;
                    array.forEach(new Consumer<Object>() {
                        @Override
                        public void accept(Object t) {
                            if (t instanceof JSONObject) {
                                walkTree((JSONObject) t);
                            }
                        }
                    });
                }
            }
        }
    }

    private void processItems(Object keyvalue) {

        JSONArray array = (JSONArray) keyvalue;
        // System.out.println("items " + array.length());
        Iterator<Object> arrayit = array.iterator();
        while (arrayit.hasNext()) {
            JSONObject obj = (JSONObject) arrayit.next();
            for (Object key2 : obj.keySet()) {
                //based on you key types
                String keyStr2 = (String) key2;
                Object keyvalue2 = obj.get(keyStr2);
                if ("@id".equalsIgnoreCase(keyStr2)) {
                    jsons.add(keyvalue2.toString());
                } else if ("parent".equalsIgnoreCase(keyStr2)) {
                    jsons.add(keyvalue2.toString());
                } else if ("registration".equalsIgnoreCase(keyStr2)) {
                    jsons.add(keyvalue2.toString());
                } else if ("packageContent".equalsIgnoreCase(keyStr2)) {
                    packages.add(keyvalue2.toString());
                } else if ("items".equalsIgnoreCase(keyStr2)) {
                    //these should be dependencies of dependencies
                    processItems(keyvalue2);

                } else if ("catalogEntry".equals(keyStr2)) {
                    JSONObject cat = (JSONObject) keyvalue2;
                    for (Object key3 : cat.keySet()) {
                        String keyStr3 = (String) key3;
                        Object keyvalue3 = cat.get(keyStr3);
                        if ("@id".equalsIgnoreCase(keyStr3)) {
                            jsons.add(keyvalue3.toString());
                        } else if ("packageContent".equalsIgnoreCase(keyStr3)) {
                            packages.add(keyvalue3.toString());
                        } else if ("dependencyGroups".equalsIgnoreCase(keyStr3)) {
                            JSONArray groups = (JSONArray) keyvalue3;
                            Iterator<Object> iterator = groups.iterator();
                            while (iterator.hasNext()) {
                                Object next = iterator.next();
                                if (next instanceof JSONObject) {
                                    JSONObject item = (JSONObject) next;
                                    for (Object key4 : item.keySet()) {
                                        String keyStr4 = (String) key4;
                                        Object keyvalue4 = item.get(keyStr4);
                                        //System.out.println(keyStr4);
                                        if ("dependencies".equalsIgnoreCase(keyStr4)) {
                                            JSONArray deps = (JSONArray) keyvalue4;
                                            Iterator<Object> it2 = deps.iterator();
                                            while (it2.hasNext()) {
                                                Object next1 = it2.next();
                                                if (next1 instanceof JSONObject) {
                                                    JSONObject item1 = (JSONObject) next1;
                                                    for (Object key5 : item1.keySet()) {
                                                        String keyStr5 = (String) key5;
                                                        Object keyvalue5 = item1.get(keyStr5);
                                                        //System.out.println(keyStr5);
                                                        if ("registration".equalsIgnoreCase(keyStr5)) {
                                                            jsons.add(keyvalue5.toString());
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }

                            }
                            //JSONArray jsonArray = groups.getJSONArray("dependencies");
                            // System.out.println(groups);

                        }
                        //looking for dependencyGroups
                        //dependencies[]
                        //registration
                    }

                }
                //Print key and value
                // System.out.println("key: " + keyStr2 + " value: " + keyvalue2);
                //for nested objects iteration if required
                //if (keyvalue instanceof JSONObject)
                //printJsonObject((JSONObject)keyvalue);
            }
        }
    }

    private static void print(Set<String> jsons) {
        Iterator<String> iterator = jsons.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }

    private File download(String remoteUrl, boolean isJson) throws Exception {
        System.out.println("Downloading " + remoteUrl);
        URL obj = new URL(remoteUrl);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setReadTimeout(5000);

        System.out.println("Request URL ... " + remoteUrl);

        boolean redirect = false;

        // normally, 3xx is redirect
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                redirect = true;
            }
        }

        URL url = new URL(remoteUrl);
        String outputLocation = "output/" + url.getPath();

        File output = new File(outputLocation);
        output.getParentFile().mkdirs();

        if (redirect) {

            // get redirect url from "location" header field
            String newUrl = conn.getHeaderField("Location");

            // get the cookie if need, for login
            String cookies = conn.getHeaderField("Set-Cookie");

            // open the new connnection again
            conn = (HttpURLConnection) new URL(newUrl).openConnection();

            System.out.println("Redirect to URL : " + newUrl);

        }
        if (!isJson && output.exists()) {
            System.out.println(output.getAbsolutePath() + " exists, skipping");
            return null;
        }

        byte[] buffer = new byte[2048];

        FileOutputStream baos = new FileOutputStream(output);
        InputStream inputStream = conn.getInputStream();
        int totalBytes = 0;
        int read = inputStream.read(buffer);
        while (read > 0) {
            totalBytes += read;
            baos.write(buffer, 0, read);
            read = inputStream.read(buffer);
        }
        System.out.println("Retrieved " + totalBytes + "bytes");

        return output;
    }
}
