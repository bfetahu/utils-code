package utils;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class WebUtils {
    public static HttpClient client = new HttpClient();

    public static String getURLContent(String url_str) throws IOException {
        URL url = new URL(url_str);

        InputStream is = (InputStream) url.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        StringBuffer sb = new StringBuffer();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        String htmlContent = sb.toString();
        br.close();
        return htmlContent;
    }

    public static String httpPOST(String url_str, List<Map.Entry<String, String>> params) throws IOException {
        URL url = new URL(url_str);

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params) {
            if (postData.length() != 0) postData.append('&');
            postData.append(param.getKey()).append("=");
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setRequestProperty("Accept", "application/json");

        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String line = null;
        StringBuffer sb = new StringBuffer();
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }

        in.close();

        return sb.toString();
    }

    /*
     * Does the actual call of the Web Service, using a HttpClient which executes the GetMethod.
     */
    public static String request(String url) {
        Client client1 = Client.create();
        System.out.println(url);

        WebResource wbres = client1.resource(url);
        ClientResponse cr = wbres.accept("application/json").get(ClientResponse.class);
        int status = cr.getStatus();
        String response = cr.getEntity(String.class);

        if (status == 200)
            return response;

        return "";
    }

    public static String post(String url, List<Map.Entry<String, String>> urlParameters) {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        try {
            // add header
            for (Map.Entry<String, String> name_val : urlParameters) {
                method.addParameter(name_val.getKey(), name_val.getValue());
            }

            //Set the results type, which will be JSON.
            method.addRequestHeader(new Header("Accept", "application/json"));
            method.addRequestHeader(new Header("content-type", "application/x-www-form-urlencoded"));

            int response = client.executeMethod(method);
            if (response != HttpStatus.SC_OK) {
                System.out.println("Method failed: " + method.getStatusText());
            }
            // Read the response body.
            BufferedReader br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            // Use caution: ensure correct character encoding and is not binary data
            method.releaseConnection();
            client.getHttpConnectionManager().closeIdleConnections(0);
            method = null;
            client = null;
            br.close();

            return sb.toString();
        } catch (Exception e) {
            method.releaseConnection();
            client.getHttpConnectionManager().closeIdleConnections(0);
            method = null;
            client = null;
            System.out.println(e.getMessage());
        }
        return "";
    }

    /**
     * Crawl a specific set of URLs.
     *
     * @param output_dir
     * @param index_file
     * @param crawl_urls
     * @param thread_no
     * @param crawl_filter
     * @throws InterruptedException
     */
    public static void crawlURLs(String output_dir, String index_file, String[] crawl_urls, int thread_no, String crawl_filter) throws InterruptedException {
        FileUtils.checkDir(output_dir);

        //write the crawl index
        Map<String, String> url_indexes = new HashMap<>();
        int url_index = 0;
        if (FileUtils.fileExists(index_file, true)) {
            url_indexes = FileUtils.readIntoStringMap(index_file, "\t", false);

            //get the max url entry
            for (String url : url_indexes.keySet()) {
                try {
                    int index = Integer.valueOf(url_indexes.get(url).replace("file_", "").replace(".html", ""));
                    if (url_index < index) {
                        url_index = index;
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing file index " + url_indexes.get(url));
                }
            }
        }
        url_index++;

        StringBuffer sb = new StringBuffer();
        for (String event_line : crawl_urls) {
            String[] tmp = event_line.split("\t");
            String url = tmp[tmp.length - 1];
            if (url_indexes.containsKey(url)) {
                continue;
            }

            url_indexes.put(url, "file_" + url_index + ".html");
            sb.append(url).append("\t").append("file_" + url_index + ".html").append("\n");
            url_index++;
        }
        FileUtils.saveText(sb.toString(), index_file, true);


        //crawl the URLs.
        String crawl_dir = output_dir + "/raw_files/";
        FileUtils.checkDir(crawl_dir);
        crawlURLs(url_indexes, thread_no, crawl_dir, crawl_filter);
    }

    /**
     * Crawls a set of URLs as given by a URL index.
     *
     * @param crawl
     * @param threads
     * @param out_dir
     * @throws InterruptedException
     */
    private static void crawlURLs(Map<String, String> crawl, int threads, String out_dir, String crawl_filter) throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        crawl.keySet().parallelStream().forEach(url_to_index -> {
            Runnable r = () -> {
                String url_file = crawl.get(url_to_index);
                if (FileUtils.fileExists(out_dir + "/" + url_file + ".gz", false) || FileUtils.fileExists(out_dir + "/" + url_file, false)) {
                    return;
                }

                //check if the URL is not allowed to be crawled.
                if (url_file.lastIndexOf(".") != -1) {
                    String suffix = url_file.substring(url_file.lastIndexOf(".")).toLowerCase().trim();
                    if (crawl_filter.contains(suffix)) {
                        return;
                    }
                }

                try {
                    Process p = Runtime.getRuntime().exec("wget -O " + out_dir + "/" + url_file + " " + url_to_index);
                    p.waitFor(5, TimeUnit.SECONDS);
                    p.destroy();

                    p = Runtime.getRuntime().exec("gzip " + out_dir + "/" + url_file);
                    p.waitFor();
                    p.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Finished " + url_file);
            };
            threadPool.submit(r);
        });

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }


    /**
     * For the crawled files from a specific index consisting of URLs and file indexes.
     *
     * @throws InterruptedException
     */
    public static void boilerPipeCleanCrawledURLs(String out_file, String input_dir, String index_file) throws InterruptedException, IOException {
        //read first the index file containing the mapping of files to the respective folders
        Map<String, String> crawl_index = FileUtils.readIntoStringMap(index_file, "\t", false);

        System.out.println("Cleaning " + crawl_index.size() + " articles.");
        //in this case the cleaning of files has finished, hence, move to the next step.
        StringBuffer sb = new StringBuffer();

        for (String url : crawl_index.keySet()) {
            String file = crawl_index.get(url);
            String file_name = input_dir + "/" + file + ".gz";
            if (!FileUtils.fileExists(file_name, true)) {
                continue;
            }
            try {
                String json_line = cleanANDConvertJSON(file_name, url);
                if (json_line.isEmpty()) {
                    continue;
                }
                sb.append(json_line);
                System.out.printf("Finished processing file %s\n", file_name);
            } catch (Exception e) {
                System.out.printf("Error at file %s with message %s\n", file, e.getMessage());
            }
            if (sb.length() > 10000) {
                FileUtils.saveText(sb.toString(), out_file, true);
                sb.delete(0, sb.length());
            }
        }
        FileUtils.saveText(sb.toString(), out_file, true);
    }


    /**
     * Converts to JSON the extracted content from the web page. The fields of the JSON object are:
     * url, date, doc_content, title.
     *
     * @param url
     * @param file_name
     * @return
     * @throws BoilerpipeProcessingException
     */
    private static String cleanANDConvertJSON(String file_name, String url) throws BoilerpipeProcessingException {
        String doc_content = FileUtils.readText(file_name);
        String text = ArticleExtractor.getInstance().getText(doc_content);
        String title = Jsoup.parse(doc_content).title();

        if (text.isEmpty()) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("{\"url\":\"").append(StringEscapeUtils.escapeJson(url)).
                append("\",\"content\":\"").append(StringEscapeUtils.escapeJson(text)).
                append("\",\"title\":\"").append(StringEscapeUtils.escapeJson(title)).append("\"}\n");
        return sb.toString();
    }

}