package utils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import gnu.trove.THashMap;
import gnu.trove.TIntIntHashMap;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by besnik on 10/09/2014.
 */
public class Utils {
    public static String sURL = "http://asev.l3s.uni-hannover.de:1987/solr/collection1/select?q=headline:SEARCH_PHRASE&wt=json&indent=true";
    public static String sURL_body = "http://localhost:8983/solr/wiki_ext_docs/select?q=SEARCH_PHRASE&wt=json&indent=true";

    /*
     * Does the actual call of the Web Service, using a HttpClient which executes the GetMethod.
     */
    public static String request(String search_phrase) {
        try {
            String sURL_tmp = sURL.replace("SEARCH_PHRASE", search_phrase);
            Client client1 = Client.create();
            System.out.println(sURL_tmp);

            WebResource wbres = client1.resource(sURL_tmp);
            ClientResponse cr = wbres.accept("application/json").get(ClientResponse.class);
            int status = cr.getStatus();
            String response = cr.getEntity(String.class);

            if (status == 200)
                return response;

            return "";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String queryNYT(String entity_name, String section_name) {
        try {
            section_name = section_name.replaceAll("_", " ");

            String search_phase = "text:ENTITY_NAME AND text:SECTION_NAME";
            search_phase = search_phase.replace("ENTITY_NAME", entity_name.trim().toLowerCase());
            search_phase = search_phase.replace("SECTION_NAME", section_name.trim().toLowerCase());

            String sURL_tmp = sURL_body.replace("SEARCH_PHRASE", URLEncoder.encode(search_phase, "UTF-8"));
            return WebUtils.request(sURL_tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String queryNYTArticle(String article) {
        try {
            String search_phase = "guid:" + article;

            String sURL_tmp = sURL_body.replace("SEARCH_PHRASE", URLEncoder.encode(search_phase, "UTF-8"));

            return WebUtils.request(sURL_tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * Returns the offset regions of the main sections in a Wikipedia entity page.
     *
     * @param text
     * @return
     */
    public static Map<String, Map.Entry<Integer, Integer>> extractEntityOffsetRegions(String text) {
        Map<String, Map.Entry<Integer, Integer>> rst = new HashMap<>();
        Pattern section_pattern = Pattern.compile("==.*==");
        Matcher section_matcher = section_pattern.matcher(text);

        int prev_start = 0, prev_end = 0;
        String prev_section = "";
        while (section_matcher.find()) {
            String section = section_matcher.group();
            section = section.replaceAll("=", "").trim();

            int start = section_matcher.start();

            if (prev_section.isEmpty()) {
                prev_section = section;
                prev_start = start;
            } else {
                prev_end = start;
                Map.Entry<Integer, Integer> entry = new AbstractMap.SimpleEntry<Integer, Integer>(prev_start, prev_end);
                rst.put(prev_section, entry);
                prev_section = section;
            }

            if (section_matcher.hitEnd()) {
                Map.Entry<Integer, Integer> entry = new AbstractMap.SimpleEntry<Integer, Integer>(start, text.length());
                rst.put(section, entry);
            }
        }
        return rst;
    }

    /**
     * Transforms a JSON array into a map datastructure consisting of the keys and the JSON objects. It is done so for faster access.
     *
     * @param sub_key
     * @return
     */
    public static Map<String, JSONObject> loadIntoMap(JSONArray obj_arr, String sub_key) {
        Map<String, JSONObject> map = new HashMap<>();

        for (int i = 0; i < obj_arr.length(); i++) {
            try {
                JSONObject sub_obj = obj_arr.getJSONObject(i);
                if (sub_obj.has(sub_key)) {
                    map.put(sub_obj.getString(sub_key), sub_obj);
                }
            } catch (Exception e) {
                System.out.printf("Error at method loadIntoMap with error: %s for JSON %s.\n", e.getMessage(), obj_arr.get(i).toString());
            }
        }
        return map;
    }

    /**
     * Transforms a JSON array into a map datastructure consisting of the keys and the JSON objects. It is done so for faster access.
     *
     * @param sub_key
     * @return
     */
    public static Map<String, JSONObject> loadIntoMap(JSONArray obj_arr, String sub_key, boolean lower_key) {
        Map<String, JSONObject> map = new HashMap<>();

        for (int i = 0; i < obj_arr.length(); i++) {
            try {
                JSONObject sub_obj = obj_arr.getJSONObject(i);
                if (sub_obj.has(sub_key)) {
                    String key = sub_obj.getString(sub_key);
                    if (lower_key) {
                        key = key.toLowerCase().trim();
                    }
                    map.put(key, sub_obj);
                }
            } catch (Exception e) {
                System.out.printf("Error at method loadIntoMap with error: %s for JSON %s.\n", e.getMessage(), obj_arr.get(i).toString());
            }
        }
        return map;
    }


    /**
     * Load the external references.
     *
     * @param data_file
     * @return
     */
    public static Map<String, Set<String>> loadEntityDocuments(String data_file) {
        Map<String, Set<String>> rst = new HashMap<>();
        String[] lines = FileUtils.readText(data_file).split("\n");
        for (String line : lines) {
            String[] tmp = line.split("\t");
            String url = tmp[2];
            String entity = tmp[0];


            Set<String> sub_rst = rst.get(url);
            sub_rst = sub_rst == null ? new HashSet<String>() : sub_rst;
            rst.put(url, sub_rst);
            sub_rst.add(entity);
        }
        return rst;
    }


    /**
     * Load the set of internal named entities from Wikipedia pages. They are in the form of anchors.
     *
     * @param text
     * @return
     */
    public static Set<String> getInternalNamedEntitis(String text) {
        Set<String> entities = new HashSet<>();
        Pattern p = Pattern.compile("<a [^>]+>([^<]+)<\\/a>");
        Matcher m = p.matcher(text);

        while (m.find()) {
            String entity = m.group();
            entity = entity.substring(entity.indexOf("href=\"") + "href=\"".length(), entity.indexOf(">")).trim();
            entity = entity.replaceAll("\"", "");
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Load the entity section label clusters.
     *
     * @param section_clusters_dir
     * @return
     */
    public static Map<String, Map<String, String>> loadDomainSectionClusters(String section_clusters_dir) {
        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(section_clusters_dir, files);

        Map<String, Map<String, String>> domain_section_clusters = new HashMap<>();
        for (String file : files) {
            String[] clusters = FileUtils.readText(file).split("\n");
            int start_index = file.lastIndexOf("/") + 1;
            String domain_name = file.substring(start_index, file.indexOf("_", start_index));
            Map<String, String> domain_clusters = domain_section_clusters.get(domain_name);
            domain_clusters = domain_clusters == null ? new HashMap<String, String>() : domain_clusters;
            domain_section_clusters.put(domain_name, domain_clusters);

            for (String cluster_line : clusters) {
                if (cluster_line.trim().isEmpty()) {
                    continue;
                }
                String[] tmp = cluster_line.split("\t");
                if (tmp.length == 2) {
                    String section_label = tmp[1];
                    String[] sections = tmp[0].split(";");

                    for (String section : sections) {
                        String section_tmp = section.replaceAll("_", " ").toLowerCase().trim();
                        domain_clusters.put(section_tmp, section_label);
                    }
                } else {
                    String section = tmp[0].replaceAll("_", " ").trim();
                    if (!section.isEmpty()) {
                        continue;
                    }
                    domain_clusters.put(section, section);
                }
            }
        }
        return domain_section_clusters;
    }

    public static String parseDate(String date_str) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat df2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        SimpleDateFormat df11 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df3 = new SimpleDateFormat("EEE, MMM d, yyyy");
        SimpleDateFormat df4 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat df5 = new SimpleDateFormat("EEE, MMM. dd, yyyy");
        SimpleDateFormat df6 = new SimpleDateFormat("EEE, MMM dd, yyyy");
        SimpleDateFormat df7 = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat df8 = new SimpleDateFormat("EEEEE d MMM yyyy");
        SimpleDateFormat df9 = new SimpleDateFormat("EEEEE, MMM. dd, yyyy");
        SimpleDateFormat df10 = new SimpleDateFormat("dd MMM yyyy");
        SimpleDateFormat df12 = new SimpleDateFormat("MMM dd, yyyy, HH:mm a");
        SimpleDateFormat df14 = new SimpleDateFormat("MMM dd, yyyy, H:mm a");
        SimpleDateFormat df13 = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat df15 = new SimpleDateFormat("MMMMM dd, yyyy, HH:mm a");
        SimpleDateFormat df16 = new SimpleDateFormat("MMMMM dd, yyyy, H:mm a");
        SimpleDateFormat df17 = new SimpleDateFormat("MMM. dd, yyyy");
        SimpleDateFormat df18 = new SimpleDateFormat("MMMMM dd, yyyy");
        SimpleDateFormat df19 = new SimpleDateFormat("EEEEE, MMM. dd, yyyy");
        SimpleDateFormat df_20 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        SimpleDateFormat df21 = new SimpleDateFormat("MMMM dd, yyyy");
        SimpleDateFormat df22 = new SimpleDateFormat("MMMM dd, yyyy HH:mm a");
        SimpleDateFormat df23 = new SimpleDateFormat("EEE MMMM dd HH:mm:ss z yyyy");
        List<SimpleDateFormat> lst = new ArrayList<>();
        lst.add(df);
        lst.add(df1);
        lst.add(df2);
        lst.add(df3);
        lst.add(df4);
        lst.add(df5);
        lst.add(df6);
        lst.add(df7);
        lst.add(df8);
        lst.add(df9);
        lst.add(df10);
        lst.add(df11);
        lst.add(df12);
        lst.add(df13);
        lst.add(df14);
        lst.add(df15);
        lst.add(df16);
        lst.add(df17);
        lst.add(df18);
        lst.add(df19);
        lst.add(df_20);
        lst.add(df21);
        lst.add(df22);
        lst.add(df23);

        SimpleDateFormat df_simple = new SimpleDateFormat("yyyy-MM-dd");
        for (SimpleDateFormat df_tmp : lst) {
            try {
                Date dt = df_tmp.parse(date_str);
                return df_simple.format(dt);
            } catch (Exception e) {
                continue;
            }
        }
        return "";

    }


    /**
     * Counts the different n-grams of different granularity on a given text.
     *
     * @param ngrams
     * @param text
     * @return
     */
    public static Set<String> getNGrams(int ngrams, String text, Set<String> stop_words) {
        Set<String> rst = new HashSet<>();
        String[] sentence_text = text.split("\\s+");

        switch (ngrams) {
            case 1:
                for (String token : sentence_text) {
                    String token_tmp = token.replaceAll("[^A-Za-z0-9]", "").intern();
                    if (token_tmp.isEmpty() || stop_words.contains(token_tmp) || !StringUtils.isAlpha(token_tmp)) {
                        continue;
                    }
                    rst.add(token_tmp);
                }

                break;
            case 2:
                for (int k = 0; k < sentence_text.length; k++) {
                    if ((k + 1) >= sentence_text.length) {
                        break;
                    }
                    String token_1 = sentence_text[k].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_2 = sentence_text[k + 1].replaceAll("[^A-Za-z0-9]", "").intern();

                    if (token_1.isEmpty() || token_2.isEmpty() || !StringUtils.isAlpha(token_1) || !StringUtils.isAlpha(token_2)) {
                        continue;
                    }

                    String token = (token_1 + " " + token_2).intern();
                    rst.add(token);
                }

                break;
            case 3:
                for (int k = 0; k < sentence_text.length; k++) {
                    if ((k + 2) >= sentence_text.length) {
                        break;
                    }
                    String token_1 = sentence_text[k].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_2 = sentence_text[k + 1].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_3 = sentence_text[k + 2].replaceAll("[^A-Za-z0-9]", "").intern();

                    if (token_1.isEmpty() || token_2.isEmpty() || token_3.isEmpty() ||
                            !StringUtils.isAlpha(token_1) || !StringUtils.isAlpha(token_2) || !StringUtils.isAlpha(token_3)) {
                        continue;
                    }
                    String token = (token_1 + " " + token_2 + " " + token_3).intern();
                    rst.add(token);
                }
                break;
        }
        return rst;
    }

    /**
     * Counts the different n-grams of different granularity on a given text.
     *
     * @param ngrams
     * @param text
     * @return
     */
    public static List<String> getNGramsList(int ngrams, String text, Set<String> stop_words) {
        List<String> rst = new ArrayList<>();
        String[] sentence_text = text.split("\\s+");

        switch (ngrams) {
            case 1:
                for (String token : sentence_text) {
                    String token_tmp = token.replaceAll("[^A-Za-z0-9]", "").intern();
                    if (token_tmp.isEmpty() || stop_words.contains(token_tmp) || !StringUtils.isAlpha(token_tmp)) {
                        continue;
                    }
                    rst.add(token_tmp);
                }

                break;
            case 2:
                for (int k = 0; k < sentence_text.length; k++) {
                    if ((k + 1) >= sentence_text.length) {
                        break;
                    }
                    String token_1 = sentence_text[k].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_2 = sentence_text[k + 1].replaceAll("[^A-Za-z0-9]", "").intern();

                    if (token_1.isEmpty() || token_2.isEmpty() || !StringUtils.isAlpha(token_1) || !StringUtils.isAlpha(token_2)) {
                        continue;
                    }

                    String token = (token_1 + " " + token_2).intern();
                    rst.add(token);
                }

                break;
            case 3:
                for (int k = 0; k < sentence_text.length; k++) {
                    if ((k + 2) >= sentence_text.length) {
                        break;
                    }
                    String token_1 = sentence_text[k].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_2 = sentence_text[k + 1].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_3 = sentence_text[k + 2].replaceAll("[^A-Za-z0-9]", "").intern();

                    if (token_1.isEmpty() || token_2.isEmpty() || token_3.isEmpty() ||
                            !StringUtils.isAlpha(token_1) || !StringUtils.isAlpha(token_2) || !StringUtils.isAlpha(token_3)) {
                        continue;
                    }
                    String token = (token_1 + " " + token_2 + " " + token_3).intern();
                    rst.add(token);
                }
                break;
        }
        return rst;
    }


    /**
     * Counts the different n-grams of different granularity on a given text.
     *
     * @param ngrams
     * @param text
     * @return
     */
    public static Map<String, Integer> countNGramsFiltered(int ngrams, String text, Set<String> stop_words) {
        Map<String, Integer> rst = new HashMap<>();
        String[] sentence_text = text.split("\\s+");

        switch (ngrams) {
            case 1:
                for (String token : sentence_text) {
                    String token_tmp = token.replaceAll("[^A-Za-z0-9]", "").intern();
                    if (token_tmp.isEmpty() || stop_words.contains(token_tmp) || !StringUtils.isAlpha(token_tmp)) {
                        continue;
                    }
                    Integer val = rst.get(token_tmp);
                    val = val == null ? 0 : val;
                    val += 1;
                    rst.put(token_tmp, val);
                }

                break;
            case 2:
                for (int k = 0; k < sentence_text.length; k++) {
                    if ((k + 1) >= sentence_text.length) {
                        break;
                    }
                    String token_1 = sentence_text[k].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_2 = sentence_text[k + 1].replaceAll("[^A-Za-z0-9]", "").intern();

                    if (token_1.isEmpty() || token_2.isEmpty() || !StringUtils.isAlpha(token_1) || !StringUtils.isAlpha(token_2)) {
                        continue;
                    }

                    String token = (token_1 + " " + token_2).intern();
                    Integer val = rst.get(token);
                    val = val == null ? 0 : val;
                    val += 1;
                    rst.put(token, val);
                }

                break;
            case 3:
                for (int k = 0; k < sentence_text.length; k++) {
                    if ((k + 2) >= sentence_text.length) {
                        break;
                    }
                    String token_1 = sentence_text[k].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_2 = sentence_text[k + 1].replaceAll("[^A-Za-z0-9]", "").intern();
                    String token_3 = sentence_text[k + 2].replaceAll("[^A-Za-z0-9]", "").intern();

                    if (token_1.isEmpty() || token_2.isEmpty() || token_3.isEmpty() ||
                            !StringUtils.isAlpha(token_1) || !StringUtils.isAlpha(token_2) || !StringUtils.isAlpha(token_3)) {
                        continue;
                    }
                    String token = (token_1 + " " + token_2 + " " + token_3).intern();
                    Integer val = rst.get(token);
                    val = val == null ? 0 : val;
                    val += 1;
                    rst.put(token, val);
                }
                break;
        }
        return rst;
    }

    /**
     * Add n-grams from a given array of textual content from an entity.
     *
     * @param label_terms
     * @param terms
     * @param l_t_counts
     * @param last_unigram_index
     * @param terms_dictionary
     * @param stop_words
     * @return
     */
    public static int addNGrams(String[] label_terms, THashMap<Integer, TIntIntHashMap> terms,
                                THashMap<Integer, TIntIntHashMap> l_t_counts, int last_unigram_index,
                                THashMap<Integer, THashMap<String, Integer>> terms_dictionary,
                                Set<String> stop_words, int ngrams) {
        for (int k = 1; k <= ngrams; k++) {
            TIntIntHashMap ngram_terms = terms.get(k);
            ngram_terms = ngram_terms == null ? new TIntIntHashMap() : ngram_terms;
            terms.put(k, ngram_terms);

            TIntIntHashMap counts = l_t_counts.get(k);
            THashMap<String, Integer> ngram_term_dictionary = terms_dictionary.get(k);
            for (int i = 0; i < label_terms.length; i += 1) {
                String term = getTerm(label_terms, i, i + k);
                if (k == 1 && stop_words.contains(term)) {
                    continue;
                }

                if (!ngram_term_dictionary.containsKey(term)) {
                    last_unigram_index++;
                    ngram_term_dictionary.put(term, last_unigram_index);
                }

                Integer unigram_count = counts.get(ngram_term_dictionary.get(term));
                unigram_count = unigram_count == null ? 0 : unigram_count;
                unigram_count += 1;
                counts.put(ngram_term_dictionary.get(term), unigram_count);

                //keep the current count of the unigram in the entity
                Integer entity_unigram_count = ngram_terms.get(ngram_term_dictionary.get(term));
                entity_unigram_count = entity_unigram_count == null ? 0 : entity_unigram_count;
                entity_unigram_count += 1;
                ngram_terms.put(ngram_term_dictionary.get(term), entity_unigram_count);
            }
        }
        return last_unigram_index;
    }

    /**
     * From a given collection picks randomly a set of indices until the relative sample size is reached.
     *
     * @param entity_dictionary
     * @param sample_size
     * @return
     */
    public static Set<Integer> getEntityIndicesSampling(Collection<Integer> entity_dictionary, int sample_size) {
        //entity sample indices
        Set<Integer> entity_indices = new HashSet<>();
        int upper_bound = entity_dictionary.size() / sample_size;
        Random rand = new Random();
        while (entity_indices.size() < upper_bound) {
            int entity_id = rand.nextInt(entity_dictionary.size() - 1);
            entity_indices.add(entity_id);
        }
        return entity_indices;
    }

    /**
     * Get the string between a range of indices within a string array.
     *
     * @param terms
     * @param start_offset
     * @param end_offset
     * @return
     */
    private static String getTerm(String[] terms, int start_offset, int end_offset) {
        String term = "";
        if (start_offset < terms.length) {
            for (int i = start_offset; i < end_offset; i++) {
                if (i < terms.length) {
                    term = term + " " + terms[i];
                }
            }
        }
        return term.trim();
    }

}
