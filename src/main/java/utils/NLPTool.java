package utils;

import opennlp.tools.sentdetect.SentenceModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Created by besnik on 10/1/14.
 */
public class NLPTool {
    public static String TAGME_API_KEY = "API_KEY";
    public static String TAGME_API_URL = "http://tagme.d4science.org/tagme/tag";
    public static String DBPEDIA_SPOTLIGHT_URL = "http://localhost:2222/rest/annotate/";

    /**
     * Loads the sentence model that is used to generate the sentence splitter.
     *
     * @param sentence_model
     * @return
     */
    public SentenceModel loadSentenceDetector(String sentence_model) {
        try {
            InputStream modelIn = new FileInputStream(sentence_model);
            SentenceModel model = new SentenceModel(modelIn);
            modelIn.close();
            return model;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Performs the NER process on the body of NYT articles using the TagMe NER
     * tool. The named entities are in the form of Wikipedia entity URIs.
     *
     * @param doc
     * @return
     */
    public static Map<String, Double> performDBpediaSpotlightNED(String doc, double threshold) {
        if (DBPEDIA_SPOTLIGHT_URL.isEmpty()) {
            System.out.println("Please set the DBpedia Spotlight URL before using this function");
            return null;
        }
        //store the annotation in the map data structure.
        Map<String, Double> map = new HashMap<String, Double>();

        try {
            List<Map.Entry<String, String>> urlParameters = new ArrayList<Map.Entry<String, String>>();
            urlParameters.add(new AbstractMap.SimpleEntry<>("text", doc));
            urlParameters.add(new AbstractMap.SimpleEntry<>("confidence", "" + threshold));

            String response = WebUtils.httpPOST(DBPEDIA_SPOTLIGHT_URL, urlParameters);

            //parse the json output from TagMe.
            JSONObject resultJSON = new JSONObject(response);
            if (resultJSON.has("Resources")) {
                JSONArray annotations = resultJSON.getJSONArray("Resources");

                //store the annotations
                for (int i = 0; i < annotations.length(); i++) {
                    JSONObject annotation = annotations.getJSONObject(i);

                    String title_wiki_page = annotation.getString("@URI");
                    title_wiki_page = title_wiki_page.replace("http://dbpedia.org/resource/", "");
                    double rho = annotation.getDouble("@similarityScore");

                    String entity_url = "http://en.wikipedia.org/wiki/" + title_wiki_page.replaceAll(" ", "_");
                    map.put(entity_url, rho);
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return map;
    }

    /**
     * Performs the NER process on the body of NYT articles using the TagMe NER
     * tool. The named entities are in the form of Wikipedia entity URIs.
     *
     * @param doc
     * @return
     */
    public static Map<String, Double> performTagMeNED(String doc, double threshold) {
        if (TAGME_API_URL.isEmpty() || TAGME_API_KEY.isEmpty()) {
            System.out.println("Please set the following variables before using this function:\n\t(1) NLPTool.TAGME_CERT_FILE = path to the TagMe certificate,\n\t " +
                    "(2) NLPTool.TAGME_API_URL = \"https://tagme.d4science.org/tagme/tag\", and\n\t");
            return null;
        }
        //store the annotation in the map data structure.
        Map<String, Double> map = new HashMap<String, Double>();

        try {
            List<Map.Entry<String, String>> urlParameters = new ArrayList<Map.Entry<String, String>>();
            urlParameters.add(new AbstractMap.SimpleEntry<>("gcube-token", TAGME_API_KEY));
            urlParameters.add(new AbstractMap.SimpleEntry<>("epsilon", threshold + ""));
            urlParameters.add(new AbstractMap.SimpleEntry<>("text", doc));
            urlParameters.add(new AbstractMap.SimpleEntry<>("include_abstract", "false"));
            urlParameters.add(new AbstractMap.SimpleEntry<>("include_categories", "false"));
            urlParameters.add(new AbstractMap.SimpleEntry<>("lang", "en"));

            String response = WebUtils.httpPOST(TAGME_API_URL, urlParameters);

            //parse the json output from TagMe.
            JSONObject resultJSON = new JSONObject(response);
            if (resultJSON.has("annotations")) {
                JSONArray annotations = resultJSON.getJSONArray("annotations");

                //store the annotations
                for (int i = 0; i < annotations.length(); i++) {
                    JSONObject annotation = annotations.getJSONObject(i);

                    if (!annotation.has("title")) {
                        continue;
                    }

                    String title_wiki_page = annotation.getString("title");
                    double rho = annotation.getDouble("rho");

                    String entity_url = "http://en.wikipedia.org/wiki/" + title_wiki_page.replaceAll(" ", "_");
                    map.put(entity_url, rho);
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return map;
    }


    /**
     * Performs the NER process on the body of NYT articles using the TagMe NER
     * tool. The named entities are in the form of Wikipedia entity URIs.
     *
     * @param doc
     * @return
     */
    public static Map<String, Map<String, Double>> performTagMeNEDSpots(String doc, double threshold) {
        if (TAGME_API_URL.isEmpty() || TAGME_API_KEY.isEmpty()) {
            System.out.println("Please set the following variables before using this function:\n\t(1) NLPTool.TAGME_CERT_FILE = path to the TagMe certificate,\n\t " +
                    "(2) NLPTool.TAGME_API_URL = \"https://tagme.d4science.org/tagme/tag\", and\n\t");
            return null;
        }
        //store the annotation in the map data structure.
        Map<String, Map<String, Double>> map = new HashMap<>();

        try {
            List<Map.Entry<String, String>> urlParameters = new ArrayList<Map.Entry<String, String>>();
            urlParameters.add(new AbstractMap.SimpleEntry<>("gcube-token", TAGME_API_KEY));
            urlParameters.add(new AbstractMap.SimpleEntry<>("epsilon", threshold + ""));
            urlParameters.add(new AbstractMap.SimpleEntry<>("text", doc));
            urlParameters.add(new AbstractMap.SimpleEntry<>("include_abstract", "false"));
            urlParameters.add(new AbstractMap.SimpleEntry<>("include_categories", "false"));
            urlParameters.add(new AbstractMap.SimpleEntry<>("lang", "en"));

            String response = WebUtils.httpPOST(TAGME_API_URL, urlParameters);

            //parse the json output from TagMe.
            JSONObject resultJSON = new JSONObject(response);
            if (resultJSON.has("annotations")) {
                JSONArray annotations = resultJSON.getJSONArray("annotations");

                //store the annotations
                for (int i = 0; i < annotations.length(); i++) {
                    JSONObject annotation = annotations.getJSONObject(i);

                    if (!annotation.has("title")) {
                        continue;
                    }

                    String title_wiki_page = annotation.getString("title");
                    String spot = annotation.getString("spot");
                    double rho = annotation.getDouble("rho");

                    String entity_url = "http://en.wikipedia.org/wiki/" + title_wiki_page.replaceAll(" ", "_");
                    if (!map.containsKey(entity_url)) {
                        map.put(entity_url, new HashMap<>());
                    }
                    map.get(entity_url).put(spot, rho);
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return map;
    }

}
