package entities;

import org.apache.commons.lang3.StringUtils;
import utils.WikiUtils;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This represents a Wikipedia entity page. It contains the following information pieces similar to Wikipedia.
 * <p>
 * (1) Sections ===> the granularity here can be either MAIN SECTIONS ONLY or ALL SECTIONS possible (sub sub sections)
 * this is predetermined by the variable "main_sections_only". In case this is set to "true" then we will extract
 * only the main sections, otherwise all sections all extracted.
 * <p>
 * (2) Citations ===> it contains all possible citations to external references from Wikipedia. In case we extract
 * the citations, the corresponding citations in the entity page are replaced with citation markers, e.g. {{[0-9]*}}
 * instead of {{cite ...}} or any other form of citation. The citations contain all possible information encoded
 * in them like citation type (news|web|book ... etc.)
 * <p>
 * Furthermore you can set the class to clean all file references in Wikipedia entity text.
 * Created by besnik on 9/14/16.
 */
public class WikipediaEntity implements Serializable {
    //entity title
    private String title;
    //entity content
    private String content;

    //determine whether we will clean the file references or leave them intact.
    private boolean clean_references = false;

    //determine if we will extract main sections only (heading section) or all the other subsections too.
    private boolean main_sections_only = true;
    //determine if we will extract the references from the entity text.
    private boolean extract_references = true;

    //determine if we will extract references that do not adhere to the citation templates in Wikipedia
    private boolean extract_broken_references = false;

    //a variable we use to control whether we further process the content
    private boolean split_sections = true;

    //control if we extract the citing statements for each citation
    private boolean extract_statements = false;

    //the datastructure for the extracted citations. Each citation has an ID and is described by all its attributes (e.g. type, url, title etc.)
    private Map<Integer, Map<String, String>> entity_citations;

    //the datastructure capturing the citing statements
    private Map<Integer, Map<String, List<String>>> citing_statements;

    //the sections in this Wikipedia entity page.
    private WikiSection root_sections;

    //set the categories of this page
    public Set<String> categories;

    public Map<Integer, Map<String, List<String>>> getCitingStatements() {
        return citing_statements;
    }

    /**
     * Return if the boolean value if we are going to extract references that do not adhere to the citation templates
     * in Wikipedia.
     *
     * @return
     */
    public boolean getExtractBrokenReferences() {
        return extract_broken_references;
    }

    /**
     * Set the value for the variable to determine if we are going to extract broken references from Wikipedia.
     *
     * @param extract_broken_references
     */
    public void setExtractBrokenReferences(boolean extract_broken_references) {
        this.extract_broken_references = extract_broken_references;
    }

    /**
     * Set the value for the data structure holding the citing statements. The structure is <cite id, <section, List<statement>>>
     *
     * @param citing_statements
     */
    public void setCitingStatements(Map<Integer, Map<String, List<String>>> citing_statements) {
        this.citing_statements = citing_statements;
    }

    /**
     * Return the value of the variable in which we check if we extract the citing statements.
     *
     * @return
     */
    public boolean getExtractStatements() {
        return extract_statements;
    }

    /**
     * Assign the value of the variable controlling if we extract the citing statements.
     *
     * @param extract_statements
     */
    public void setExtractStatements(boolean extract_statements) {
        this.extract_statements = extract_statements;
    }

    /**
     * Set if you want to remove file references from the Wikipedia entity text. This is usually markup text that
     * it is not very usueful, e.g. [[File]] referring to JPG file within the Wikipedia infrastructure.
     *
     * @param clean_references
     */
    public void setCleanReferences(boolean clean_references) {
        this.clean_references = clean_references;
    }

    /**
     * Get the variable whether the class is supposed to clean the file references from Wikipedia entity text.
     *
     * @return
     */
    public boolean getCleanReferences() {
        return clean_references;
    }

    /**
     * Set the variable whether we are interested in extracting citations to external references from the Wikipedia
     * entity text.
     *
     * @param extract_references
     */
    public void setExtractReferences(boolean extract_references) {
        this.extract_references = extract_references;
    }

    /**
     * Get the indicator whether we are going to extract the citations.
     *
     * @return
     */
    public boolean getExtractReferences() {
        return extract_references;
    }

    /**
     * This will determine whether we will structure the entity text into the main sections only, or
     * we will structure the text to all the existing sub-sections.
     *
     * @param main_sections_only
     */
    public void setMainSectionsOnly(boolean main_sections_only) {
        this.main_sections_only = main_sections_only;
    }

    /**
     * Returns the variable determining whether we will extract only the main sections, or all the other
     * sub-sections.
     *
     * @return
     */
    public boolean getMainSectionsOnly() {
        return main_sections_only;
    }

    /**
     * Returns the already extracted citations from the entire entity text.
     *
     * @return
     */
    public Map<Integer, Map<String, String>> getEntityCitations() {
        return entity_citations;
    }

    /**
     * Set the citations for this Wikipedia entity page in case these citations are
     * modified. Please note that the citations are extracted and set automatically
     * in case the variable extract_references = true.
     *
     * @param entity_citations
     */
    public void setEntityCitations(Map<Integer, Map<String, String>> entity_citations) {
        this.entity_citations = entity_citations;
    }

    public WikipediaEntity() {
        root_sections = new WikiSection();
        entity_citations = new HashMap<>();
        categories = new HashSet<>();
        citing_statements = new HashMap<>();
    }

    /**
     * Get the categories this entity belongs to.
     *
     * @return
     */
    public Set<String> getCategories() {
        return categories;
    }

    /**
     * Add a single category for this entity.
     *
     * @param category
     */
    public void addCategory(String category) {
        categories.add(category);
    }

    /**
     * Set the set of categories this entity belongs to.
     *
     * @param categories
     */
    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    /**
     * Get the entity title.
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the entity title.
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the ROOT section for this entity. The root section contains all the other sections for this entity.
     *
     * @return
     */
    public WikiSection getRootSection() {
        return root_sections;
    }

    /**
     * Get a single section based on the section label. In case this section does not exist NULL is returned.
     *
     * @param section
     * @return
     */
    public WikiSection getSection(String section) {
        return root_sections.findSection(section);
    }

    /**
     * Get the variable determining whether we will split the sections or not.
     *
     * @return
     */
    public boolean getSplitSections() {
        return split_sections;
    }

    /**
     * Set the variable indicating if we will split the sections.
     *
     * @param split_sections
     */
    public void setSplitSections(boolean split_sections) {
        this.split_sections = split_sections;
    }

    /**
     * Set the entity text for this entity. Here based on the given variables
     * (split_sections|extract_references|main_sections_only|clean_references)
     * we process the entity page like extracting the sections, the references,
     * and cleaning the content.
     *
     * @param wiki_content
     */
    public void setContent(String wiki_content) {
        //remove the infobox
        this.content = wiki_content;
        content = WikiUtils.removeInfoboxInformaion(content);
        if (clean_references) {
            content = WikiUtils.removeWikiFileReferences(content);
            content = WikiUtils.removeWikiAnchorReferences(content);
        }
        //\[\[Category:(.*)\]\]
        content = WikiUtils.removeCategoryInformation(content);

        //if we are interested in extracting the citations, do that here.
        if (extract_references) {
            content = WikiUtils.extractWikiReferences(content, entity_citations);
        }

        //set the sections
        if (split_sections) {
            splitEntityIntoSections(content);
        }

        //set the citing statements
        if (extract_statements) {
            assignCitingStatements();
        }
    }

    /**
     * Get the content of this entity page.
     *
     * @return
     */
    public String getContent() {
        return content;
    }

    /**
     * Check if this entity contains a specific section.
     *
     * @param section
     * @return
     */
    public boolean hasSection(String section) {
        return root_sections.hasSection(section);
    }

    /**
     * Get the section text for a specific section label.
     *
     * @param section
     * @return
     */
    public String getSectionText(String section) {
        if (hasSection(section)) {
            return root_sections.findSection(section).section_text;
        }

        return "Section does not exist!";
    }

    /**
     * Return all the section labels.
     *
     * @return
     */
    public Set<String> getSectionKeys() {
        Set<String> keys = new HashSet<>();
        root_sections.getSectionKeys(keys);
        return keys;
    }

    /**
     * Return the keys only up to a specific section level.
     *
     * @param level
     * @return
     */
    public Set<String> getSectionKeys(int level) {
        Set<String> keys = new HashSet<>();
        root_sections.getSectionKeys(keys, level);
        return keys;
    }

    /**
     * Splits the entity page text into chunks of text where each chunk belongs to a section. We begin with the main section
     * that is usually the introduction of an entity page. For the subsequent section we extract the text and in case
     * the section is a subsection we denote its parent.
     *
     * @param entity_text
     */
    public void splitEntityIntoSections(String entity_text) {
        //determine how to split the section text.
        Pattern section_pattern = main_sections_only ? Pattern.compile("(?<!=)==(?!=)(.*?)(?<!=)==(?!=)") : Pattern.compile("={2,}(.*?)={2,}");
        Matcher section_matcher = section_pattern.matcher(entity_text);

        root_sections = new WikiSection();
        root_sections.section_label = "MAIN_SECTION";

        String section_name = "MAIN_SECTION";

        int start_index = 0;
        int end_index = 0;
        int current_section_level = 0;

        List<Map.Entry<Integer, String>> prev_section_entries = new LinkedList<>();
        boolean has_sections = false;
        while (section_matcher.find()) {
            has_sections = true;
            String new_section = section_matcher.group();
            int tmp_current_section_level = StringUtils.countMatches(new_section, "=") / 2;
            new_section = new_section.replaceAll("=", "").trim();
            end_index = section_matcher.start();

            //the previous text is about the main section.
            if (!section_name.equals(new_section)) {
                //extract the citations and chunk the section text into paragraphs.
                String section_text = entity_text.substring(start_index, end_index);
                if (section_name.toLowerCase().contains("references") || section_name.toLowerCase().contains("notes") || section_name.toLowerCase().contains("see also")) {
                    continue;
                }

                WikiSection section = new WikiSection();
                section.section_label = section_name;
                section.section_text = section_text.replaceAll("\n+", "\n");

                if (current_section_level == 0 && section_name.equals("MAIN_SECTION")) {
                    root_sections = section;
                    root_sections.section_level = 1;

                    prev_section_entries.add(new AbstractMap.SimpleEntry<>(root_sections.section_level, root_sections.section_label));
                } else {
                    String parent_section_label = getParentSection(prev_section_entries, current_section_level);
                    WikiSection root_section = WikiSection.findSection(parent_section_label, root_sections, current_section_level);
                    section.section_level = root_section.section_level + 1;
                    root_section.child_sections.add(section);

                    AbstractMap.SimpleEntry<Integer, String> section_entry = new AbstractMap.SimpleEntry<>(section.section_level, section.section_label);
                    prev_section_entries.add(new AbstractMap.SimpleEntry<>(section_entry));
                }
            }

            //change the parent section only if you go deeper in the section level, for example if you are iterating over the main sections then we keep the "MAIN_SECTION" as the parent section.
            section_name = new_section;
            current_section_level = tmp_current_section_level;
            start_index = section_matcher.end();
        }

        if (!has_sections) {
            root_sections.section_text = entity_text;
        }
    }


    /**
     * Extract the citing statements for an entity.  This is done only if we have extracted previously the citation
     * for a Wikipedia entity.
     * <p>
     * Here we will extract then all the citing statements to the extracted statements at the section level.
     */
    public void assignCitingStatements() {
        Pattern cite_pattern = Pattern.compile("\\{[0-9]*\\}");
        Pattern sentence_pattern = Pattern.compile("(.*?)\\.\\s{0,}(\\{\\{[0-9]+\\}\\}\\s?){1,}");
        //we perform this process at section level
        for (String section_key : getSectionKeys()) {
            WikiSection section = getSection(section_key);

            if (!(section.section_text != null && !section.section_text.isEmpty())) {
                continue;
            }
            String[] paragraphs = section.section_text.split("\\n+");
            for (String paragraph : paragraphs) {
                extractCitingSentencesForSection(section_key, paragraph, sentence_pattern, cite_pattern);
            }
        }
    }

    /**
     * For a given section paragraph extract all the citing sentences. Assign each citing statement to its corresponding
     * section and citation id.
     *
     * @param section
     * @param text
     * @param sentence_pattern
     * @param cite_pattern
     */
    public void extractCitingSentencesForSection(String section, String text, Pattern sentence_pattern, Pattern cite_pattern) {
        //find sentences with a citation
        Matcher sentence_matcher = sentence_pattern.matcher(text);
        //extract citation number
        while (sentence_matcher.find()) {
            String sentence = sentence_matcher.group();
            Matcher cite_matcher = cite_pattern.matcher(sentence);
            while (cite_matcher.find()) {
                String cite_number = cite_matcher.group().replaceAll("\\{|\\}", "");
                int citation_id = Integer.valueOf(cite_number);

                if (!entity_citations.containsKey(citation_id)) {
                    continue;
                }

                if (!citing_statements.containsKey(citation_id)) {
                    citing_statements.put(citation_id, new HashMap<>());
                }
                if (!citing_statements.get(citation_id).containsKey(section)) {
                    citing_statements.get(citation_id).put(section, new ArrayList<>());
                }
                citing_statements.get(citation_id).get(section).add(sentence);
            }
        }
    }

    /**
     * Return the parent section. We have a linked list from which we read from the last item to the first and find the
     * first occurrence of a section which has a level - 1 than the one we consider.
     *
     * @param prev_section_entries
     * @param level
     * @return
     */
    public String getParentSection(List<Map.Entry<Integer, String>> prev_section_entries, int level) {
        for (int j = prev_section_entries.size() - 1; j >= 0; j--) {
            Map.Entry<Integer, String> section_entry = prev_section_entries.get(j);
            if (section_entry.getKey() <= level - 1) {
                return section_entry.getValue();
            }
        }
        return "MAIN_SECTION";
    }

    public static void main(String[] args) {
        String text6 = "'''Jorge Mario Cardinal Bergoglio''' (born December 17, 1936 in Buenos Aires, Argentina)is a Roman Catholic cardinal and currently archbishop of Buenos Aires. He is thought to be papabile.\n" +
                "\n" +
                "Bergoglio, Jorge\n" +
                "Bergoglio, Jorge\n" +
                "Bergoglio, Jorge\n" +
                "Bergoglio, Jorge\n" +
                "de:Jorge Mario Bergoglio\n" +
                "{{bio-stub}}";
        WikipediaEntity entity = new WikipediaEntity();
        entity.setMainSectionsOnly(false);
        entity.setSplitSections(true);
        entity.setContent(text6);

        System.out.println(entity.getSection("MAIN_SECTION").section_text);
    }
}
