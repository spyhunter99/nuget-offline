/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.spyhunter99.nugetparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
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
public class Main {

    static Set<String> jsons = new HashSet<>();
    static Set<String> packages = new HashSet<>();

    public static void main(String[] args) throws Exception {
        
        //TODO start with seed dependency. Package name must be toLower vs wha nuget has in the user interface
        //download and parse all json files
        //download all packages all versions
        
//packageContent registration parent         id

        CloseableHttpClient client = HttpClients.custom()
                .disableContentCompression()
                .build();

        HttpGet request = new HttpGet("http://nugetgallery.blob.core.windows.net/v3-registration1-gz/jquery/index.json");
        //http://nugetgallery.blob.core.windows.net/v3-registration1-gz/jquery/index.json");
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");

        CloseableHttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
//        Header contentEncodingHeader = (Header) entity.getContentEncoding();
        InputStream content = entity.getContent();
        GZIPInputStream gzis = new GZIPInputStream(content);
        //BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        BufferedReader rd = new BufferedReader(new InputStreamReader(gzis, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }

        String output = sb.toString();// EntityUtils.toString(entity, Charset.forName("UTF-8").name());

        JSONObject jsonObj = new JSONObject(output);//readJsonFromUrl("https://api.nuget.org/v3/registration1-gz/audit.wcf/index.json ");

        for (Object key : jsonObj.keySet()) {
            //based on you key types
            String keyStr = (String) key;
            Object keyvalue = jsonObj.get(keyStr);

            //Print key and value
            // System.out.println("key: " + keyStr + " value: " + keyvalue);
            if ("items".equalsIgnoreCase(keyStr)) {
                processItems(keyvalue);

            }

            //JSONObject get = (JSONObject) jsonArray.get(0);
            // JSONArray jsonArray1 = jsonArray.getJSONArray("items");
            //System.out.println(jsonObj.toString());
        }

        print(jsons);
        print(packages);
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();

        //BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream("../sample.json"), Charset.forName("UTF-8")));
        String jsonText = readAll(rd);
        // System.out.println(jsonText);
        JSONObject json = new JSONObject(jsonText);
        is.close();
        return json;
    }

    private static void walkTree(JSONObject obj) {
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

    private static void processItems(Object keyvalue) {

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
}
