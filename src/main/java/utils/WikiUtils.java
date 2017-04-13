package utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by besnik on 1/18/17.
 */
public class WikiUtils {
    public static final String citation_type_str = "album notes,av media,book,comic,conference,court,encyclopedia,episode,journal,mailing list,map,news,newsgroup,press release,thesis,video game,web";
    public static Set<String> citation_types = new HashSet<>();

    /**
     * Gets the named citations from the Wikipedia entity text before deleting them.
     * <p>
     * We distinguish between named references which are <ref name="">{{cite ...}}</ref>
     * and references which are only as citations {{cite ...}}
     *
     * @param text
     * @return
     */
    public static String extractWikiReferences(String text, Map<Integer, Map<String, String>> citations) {
        //remove the section headings from the content
        text = text.replaceAll("^={2,}(.*?)={2,}\n", "");

        int citation_counter = 0;
        StringBuffer sb_entity = new StringBuffer();

        //remove first the references that do not adhere to any of the citation templates e.g. <ref>http..</ref>
        citation_counter = extractBrokenReferences(text, sb_entity, citations, citation_counter);

        //we remove here first the references that are named but do not have a citation (usually these are placed in the end of the entity text).
        Map<String, Integer> named_reference_mappings = new HashMap<>();
        String entity_text = sb_entity.toString();
        citation_counter = replaceReferencesWithoutCitations(entity_text, named_reference_mappings, sb_entity, citation_counter);

        //remove the references which do not have a name, leave only the citation in that case.
        entity_text = sb_entity.toString();
        sb_entity = new StringBuffer();
        citation_counter = removeReferenceWithoutNameWithCitation(entity_text, sb_entity, citations, citation_counter);

        //finally extract all the citations that are named and have references.
        entity_text = sb_entity.toString();
        sb_entity = new StringBuffer();
        citation_counter = replaceNamedReferencesWithCitations(entity_text, sb_entity, named_reference_mappings, citations, citation_counter);

        //now we remove the citations that are not named.
        entity_text = sb_entity.toString();
        sb_entity = new StringBuffer();
        replaceCitations(entity_text, sb_entity, citations, citation_counter);

        return sb_entity.toString();
    }

    /**
     * Gets the named citations from the Wikipedia entity text before deleting them.
     * <p>
     * We distinguish between named references which are <ref name="">{{cite ...}}</ref>
     * and references which are only as citations {{cite ...}}
     *
     * @param text
     * @return
     */
    public static int extractBrokenReferences(String text, StringBuffer sb, Map<Integer, Map<String, String>> citations, int citation_counter) {
        Pattern ref_p = Pattern.compile("<ref>(\\[?htt)(.*?)(\\]?)</ref>");
        Matcher ref_m = ref_p.matcher(text);

        int last_index = 0;
        while (ref_m.find()) {
            String group = ref_m.group();
            group = group.replace("<ref>", "").replace("</ref>", "").replaceAll("\\{|\\}", "").replaceAll("\\[|\\]", "");

            Map<String, String> citation_attributes = new HashMap<>();
            citation_attributes.put("url", group);
            citations.put(citation_counter, citation_attributes);

            int c_start = ref_m.start();
            int c_end = ref_m.end();

            sb.append(text.substring(last_index, c_start)).append("{{").append(citation_counter).append("}}");
            last_index = c_end;
            citation_counter++;
        }
        //add the remaining content of the text after the citation marker
        if (last_index < text.length()) {
            sb.append(text.substring(last_index));
        }
        return citation_counter;
    }

    /**
     * Here we replace all the named citations that do have a citation, e.g. <ref name="cite_1'>{{cite ...}}</ref>
     * <p>
     * In case we are dealing only with the citation, and we have already retrieved this named citation before
     * as a reference without a citation then we simply retrieve the citation content and add it to the corresponding marker.
     *
     * @param entity_text
     * @return
     */
    public static int removeReferenceWithoutNameWithCitation(String entity_text, StringBuffer sb, Map<Integer, Map<String, String>> citations, int citation_counter) {
        //extracts all the references
        Pattern p = Pattern.compile("<ref>(.*?[^/>]*)</ref>");
        Matcher m = p.matcher(entity_text);

        //the citation pattern matcher
        int last_index = 0;
        while (m.find()) {
            //citation start and end indices
            int c_start = m.start();
            int c_end = m.end();

            String citation_text = m.group();
            citation_text = citation_text.replace("<ref>", "").replace("</ref>", "");
            citations.put(citation_counter, getCitationAttributes(citation_text));
            //update the entity text
            sb.append(entity_text.substring(last_index, c_start)).append("{{").append(citation_counter).append("}}");
            last_index = c_end;
            citation_counter++;
        }

        //add the remaining content of the text after the citation marker
        if (last_index < entity_text.length()) {
            sb.append(entity_text.substring(last_index));
        }
        return citation_counter;
    }

    /**
     * Here we replace all the named citations that do have a citation, e.g. <ref name="cite_1'>{{cite ...}}</ref>
     * <p>
     * In case we are dealing only with the citation, and we have already retrieved this named citation before
     * as a reference without a citation then we simply retrieve the citation content and add it to the corresponding marker.
     *
     * @param entity_text
     * @param citation_mapper
     * @param citations
     * @param citation_counter
     * @return
     */
    public static int replaceNamedReferencesWithCitations(String entity_text, StringBuffer sb, Map<String, Integer> citation_mapper, Map<Integer, Map<String, String>> citations, int citation_counter) {
        //extracts all the references
        Pattern p = Pattern.compile("<ref(\\s+name=(.*?))?>(.*?)</ref>");
        Matcher m = p.matcher(entity_text);

        //the citation pattern matcher
        Pattern p_citation = Pattern.compile("(?i)\\{\\{Cit(.*?)\\}\\}");
        int last_index = 0;
        while (m.find()) {
            String reference = m.group();

            //citation start and end indices
            int c_start = m.start();
            int c_end = m.end();

            //get the reference name
            String ref_name = reference.substring(0, reference.indexOf(">")).replaceAll("<ref(\\s+name=)?", "").replaceAll("\"", "").replaceAll(">", "").trim();
            if (!citation_mapper.containsKey(ref_name)) {
                citation_mapper.put(ref_name, citation_counter);
                citation_counter++;
            }

            int cite_index = citation_mapper.get(ref_name);
            //parse the citation
            Matcher cite_match = p_citation.matcher(reference);
            if (cite_match.find()) {
                String citation = cite_match.group();
                citation = citation.replaceAll("\\{|\\}", "");
                citations.put(cite_index, getCitationAttributes(citation));
            }

            //update the entity text
            sb.append(entity_text.substring(last_index, c_start)).append("{{").append(cite_index).append("}}");
            last_index = c_end;
        }

        //add the remaining content of the text after the citation marker
        if (last_index < entity_text.length()) {
            sb.append(entity_text.substring(last_index));
        }
        return citation_counter;
    }

    /**
     * Here we replace the named references that do not contain the actual citations, e.g. <ref name="cite_1"/>. We map
     * them into specific citation markers e.g. {{0}} => "cite_1". Later on "cite_1" is used to resolve {{0}} to
     * the corresponding citation text.
     *
     * @param entity_text
     */
    public static int replaceReferencesWithoutCitations(String entity_text, Map<String, Integer> citations, StringBuffer sb, int citation_counter) {
        Pattern citation_pattern = Pattern.compile("<ref(\\s+name=(.*?))?/>");
        Matcher citation_matcher = citation_pattern.matcher(entity_text);
        int last_index = 0;

        while (citation_matcher.find()) {
            String ref_name = citation_matcher.group().replaceAll("<ref\\s+name=", "").replaceAll("\"", "").replaceAll(">", "").replaceAll("/", "").trim();
            int c_start = citation_matcher.start();
            int c_end = citation_matcher.end();

            //add the citation mapper
            if (!citations.containsKey(ref_name)) {
                citations.put(ref_name, citation_counter);
                citation_counter++;
            }
            sb.append(entity_text.substring(last_index, c_start)).append("{{").append(citations.get(ref_name)).append("}}");
            last_index = c_end;

        }

        //add the remaining content of the text after the citation marker
        if (last_index < entity_text.length()) {
            sb.append(entity_text.substring(last_index));
        }

        return citation_counter;
    }

    /**
     * Here we replace the references that contain actual citations, e.g. {{cite ...}}.
     * We replace the citations with citation markers e.g. {{0}} t
     *
     * @param entity_text
     */
    public static int replaceCitations(String entity_text, StringBuffer sb, Map<Integer, Map<String, String>> citations, int citation_counter) {
        Pattern citation_pattern = Pattern.compile("(?i)\\{\\{Cit(.*?)\\}\\}");
        Matcher citation_matcher = citation_pattern.matcher(entity_text);

        int last_index = 0;

        //keep count of the citation markers.
        while (citation_matcher.find()) {
            //get the actual citation sentence.
            String citation = citation_matcher.group();
            int c_start = citation_matcher.start();
            int c_end = citation_matcher.end();

            sb.append(entity_text.substring(last_index, c_start)).append("{{").append(citation_counter).append("}}");
            last_index = c_end;

            //add the citation mapper
            citations.put(citation_counter, getCitationAttributes(citation));
            citation_counter++;
        }

        //add the remaining content of the text after the citation marker
        if (last_index < entity_text.length()) {
            sb.append(entity_text.substring(last_index));
        }
        return citation_counter;
    }


    /**
     * Parses a Wikipedia text, namely its references and citations into a common format.
     *
     * @param text
     * @return
     */
    public static String removeWikiFileReferences(String text) {
        Matcher m = Pattern.compile("\\[\\[File[^\\]]*?[^\\]].*\\]\\]").matcher(text);
        StringBuffer sb = new StringBuffer();
        int last_index = 0;

        //remove the empty references.
        boolean has_updated = false;

        while (m.find()) {
            int c_start = m.start();
            int c_end = m.end();

            sb.append(text.substring(last_index, c_start));
            last_index = c_end;
            has_updated = true;
        }
        if (has_updated) {
            if (last_index < text.length()) {
                sb.append(text.substring(last_index));
            }
            text = sb.toString();
        }
        sb.delete(0, sb.length());

        return text;
    }

    /**
     * Parses a Wikipedia text, and removes the double square brackets from the anchor text.
     *
     * @param text
     * @return
     */
    public static String removeWikiAnchorReferences(String text) {
        Matcher m = Pattern.compile("\\[{2}(.*?)\\]{2}").matcher(text);
        StringBuffer sb = new StringBuffer();
        int last_index = 0;

        while (m.find()) {
            int c_start = m.start();
            int c_end = m.end();
            String anchor = m.group();

            //check if the anchor text has an alternate textual representation,
            // e.g. [[Walter W. Granger|Walter Granger]] or [[Medicine Bow, Wyoming]].
            if (anchor.contains("|")) {
                sb.append(text.substring(last_index, c_start));
                int next_pos = c_start + anchor.indexOf("|");
                sb.append(text.substring(next_pos + 1, c_end - 2));
            } else {
                sb.append(text.substring(last_index, c_start));
                sb.append(text.substring(c_start + 2, c_end - 2));
            }
            last_index = c_end;
        }

        sb.append(text.substring(last_index, text.length()));
        return sb.toString();
    }

    /**
     * Splits a Wikipedia citation into its detailed attributes.
     *
     * @param citation_text
     * @return
     */
    public static Map<String, String> getCitationAttributes(String citation_text) {
        //split the citation line into <key, value> pairs.
        String[] url_iter = citation_text.split("\\|");
        Map<String, String> citation_features = new HashMap<>();
        for (int i = 0; i < url_iter.length; i++) {
            String s_tmp = url_iter[i];
            String[] key_s = s_tmp.split("=");
            if (key_s.length <= 1) {
                continue;
            }

            String key = key_s[0].trim().intern();
            String value = s_tmp.substring(s_tmp.indexOf("=") + 1).trim().toLowerCase();
            citation_features.put(key, value);
        }
        citation_features.put("type", getCitationType(citation_text));

        if (citation_features.containsKey("url") && citation_features.containsKey("type")) {
            citation_features.put("url", citation_features.get("url") + " (" + citation_features.get("type") + ")"); //add type to url, TODO: validate if this works
        }

        return citation_features;
    }


    /**
     * Returns the citation type from a citation template in Wikipedia.
     *
     * @param url_text
     * @return
     */
    public static String getCitationType(String url_text) {
        if (citation_types.isEmpty()) {
            String[] tmp = citation_type_str.split(",");
            for (String s : tmp) citation_types.add(s);
        }
        int start = url_text.indexOf(" ");
        int end = url_text.indexOf("|");

        if (start != -1 && end != -1 && start < end) {
            String type = url_text.substring(start, end).trim().intern();
            if (!citation_types.contains(type.toLowerCase())) {
                type = "N/A";
            }
            return type;
        }
        return "N/A";
    }


    /**
     * Get the domain from an URL.
     *
     * @param url
     * @return
     */
    public static String getURLDomain(String url) {
        String domain = "";
        try {
            if (url.startsWith("http://")) {
                int end_index = url.indexOf("/", "http://".length());
                if (end_index == -1) {
                    domain = url;
                } else {
                    domain = url.substring(0, end_index);
                }
            } else if (url.startsWith("https://")) {
                int end_index = url.indexOf("/", "https://".length());
                if (end_index == -1) {
                    domain = url;
                } else {
                    domain = url.substring(0, end_index);
                }
            } else if (url.startsWith("www.")) {
                int end_index = url.indexOf("/");
                if (end_index == -1) {
                    domain = url;
                } else {
                    domain = url.substring(0, end_index);
                }
            }

            if (domain.trim().isEmpty()) {
                return "N/A";
            }

            domain = domain.trim().toLowerCase();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(url);
        }
        domain = domain.replace("http://", "");
        domain = domain.replace("https://", "");
        domain = domain.replace("www.", "");
        return domain;
    }


    public static String removeCategoryInformation(String content) {
        return content.replaceAll("\\[\\[Category:(.*)\\]\\]", "");
    }

    /**
     * Removes the infobox information from a Wikipedia page.
     *
     * @param content
     * @return
     */
    public static String removeInfoboxInformaion(String content) {
        //remove the infobox
        if (content.contains("{{Infobox")) {
            int start_index = content.indexOf("{{Infobox");
            content = content.substring(start_index);
            int last_infobox_pos = extractInfoboxPosition(content) + 1;
            content = content.substring(last_infobox_pos);
            content = content.replaceAll("\\{\\{(Infobox.*\\n(?:\\|.*\\n)+)\\}\\}", "");
        }

        return content;
    }


    public static int extractInfoboxPosition(String str) {
        int last_pos = 0;
        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < str.length(); i++) {
            char current = str.charAt(i);
            if (current == '{') {
                stack.push(current);
            }


            if (current == '}') {
                char last = stack.peek();
                if (current == '}' && last == '{') {
                    stack.pop();
                    last_pos = i;

                    if (stack.isEmpty()) {
                        return last_pos;
                    }
                }
            }
        }

        return last_pos;
    }


    /**
     * Retrieves all the citations from the already extracted citation from the entity text for a single paragraph.
     * We return only the type and the URL of the citations, the citation marker, the span in the paragraph.
     *
     * @param paragraph
     * @param entity_citations
     * @return
     */
    public static Map<String, List<String[]>> getSentenceCitationAttributes(String paragraph, Map<Integer, Map<String, String>> entity_citations) {
        Map<String, List<String[]>> paragraph_citations = new HashMap<>();

        Pattern sentence_pattern = Pattern.compile("(.*?)\\.\\s{0,}(\\{\\{[0-9]+\\}\\}\\s?){1,}");
        Matcher sentence_matcher = sentence_pattern.matcher(paragraph);

        //extract citation number
        Pattern cite_number_pattern = Pattern.compile("\\{\\{[0-9]+\\}\\}");

        while (sentence_matcher.find()) {
            //the start and end of the sentence.
            int c_start = sentence_matcher.start();
            int c_end = sentence_matcher.end();

            String sentence_with_citations = sentence_matcher.group();
            Matcher cite_matcher = cite_number_pattern.matcher(sentence_with_citations);

            //check if its an empty sentence
            String sentence_cleaned = sentence_with_citations.replaceAll("\\{\\{[0-9]+\\}\\}", "").trim();
            if (sentence_cleaned.isEmpty()) {
                continue;
            }

            List<String[]> sub_citations = new ArrayList<>();
            while (cite_matcher.find()) {
                String cite_marker = cite_matcher.group().replaceAll("\\{|\\}", "");
                int citation_marker = Integer.valueOf(cite_marker);
                if (!entity_citations.containsKey(citation_marker)) {
                    continue;
                }
                Map<String, String> citation = entity_citations.get(citation_marker);
                String type = citation.containsKey("type") ? citation.get("type") : "N/A";
                String url = citation.containsKey("url") ? citation.get("url") : "N/A";
                sub_citations.add(new String[]{type, url, cite_marker, c_start + "=" + c_end});
            }
            paragraph_citations.put(sentence_with_citations, sub_citations);
        }
        return paragraph_citations;
    }

    /**
     * Retrieves all the citations from the already extracted citation from the entity text for a single paragraph.
     * We return only the type and the URL of the citations, the citation marker, the span in the paragraph.
     *
     * @param paragraph
     * @param entity_citations
     * @return
     */
    public static Map<String, List<Map<String, String>>> getSentenceCitation(String paragraph, Map<Integer, Map<String, String>> entity_citations) {
        Map<String, List<Map<String, String>>> citations = new HashMap<>();

        Pattern sentence_pattern = Pattern.compile("(.*?)\\.\\s{0,}(\\{\\{[0-9]+\\}\\}\\s?){1,}");
        Matcher sentence_matcher = sentence_pattern.matcher(paragraph);

        //extract citation number
        Pattern cite_number_pattern = Pattern.compile("\\{\\{[0-9]+\\}\\}");

        while (sentence_matcher.find()) {
            String sentence_with_citations = sentence_matcher.group();
            Matcher cite_matcher = cite_number_pattern.matcher(sentence_with_citations);

            //check if its an empty sentence
            String sentence_cleaned = sentence_with_citations.replaceAll("\\{\\{[0-9]+\\}\\}", "").trim();
            if (sentence_cleaned.isEmpty()) {
                continue;
            }
            List<Map<String, String>> sub_citations = new ArrayList<>();
            while (cite_matcher.find()) {
                String cite_marker = cite_matcher.group().replaceAll("\\{|\\}", "");
                int citation_marker = Integer.valueOf(cite_marker);
                if (!entity_citations.containsKey(citation_marker)) {
                    continue;
                }
                Map<String, String> citation = entity_citations.get(citation_marker);
                sub_citations.add(citation);
            }
            citations.put(sentence_with_citations, sub_citations);
        }
        return citations;
    }
}
