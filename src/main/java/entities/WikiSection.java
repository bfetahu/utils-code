package entities;

import org.apache.commons.lang3.StringUtils;
import utils.WikiUtils;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by besnik on 9/17/16.
 */
public class WikiSection implements Serializable {
    public String section_label;
    public String section_text;
    public int section_level = 0;

    //section sentences
    public List<String> sentences;

    //the child sections
    public List<WikiSection> child_sections;

    //all the citations for this section
    private Map<String, List<Map<String, String>>> section_statement_citations;

    //all the citations within this sections
    private Map<Integer, Map<String, String>> section_citations;

    public WikiSection() {
        child_sections = new ArrayList<>();
        sentences = new ArrayList<>();
        section_citations = new HashMap<>();
    }

    public Map<String, List<Map<String, String>>> getSectionStatementCitations() {
        return section_statement_citations;
    }

    public void setSectionStatementCitations(Map<String, List<Map<String, String>>> section_citations) {
        this.section_statement_citations = section_citations;
    }


    public Map<Integer, Map<String, String>> getSectionCitations() {
        return section_citations;
    }

    public void setSectionCitations(Map<Integer, Map<String, String>> citations) {
        Pattern cite_number_pattern = Pattern.compile("\\{\\{[0-9]+\\}\\}");
        Matcher cite_matcher = cite_number_pattern.matcher(section_text);
        while (cite_matcher.find()) {
            int cite_marker = Integer.valueOf(cite_matcher.group().replaceAll("\\{|\\}", ""));
            if (!citations.containsKey(cite_marker)) {
                continue;
            }
            section_citations.put(cite_marker, citations.get(cite_marker));
        }
    }


    /**
     * Set the citations for a specific section. A prerequisite for this is to have the citation markers already set in
     * the entity page. For example, the references in the entity text are annotated with {{[0-9]*}}.
     * <p>
     * We have a list of all the citations from the entity which is passed as an argument to find the corresponding
     * citations for this section.
     *
     * @param entity_citations
     */
    public void setCitations(Map<Integer, Map<String, String>> entity_citations) {
        Map<String, List<Map<String, String>>> section_citations = WikiUtils.getSentenceCitation(section_text, entity_citations);
        this.section_statement_citations = section_citations;
    }

    /**
     * Returns the parent section.
     *
     * @param label
     * @param parent
     * @return
     */
    public static WikiSection findSection(String label, WikiSection parent, int level) {
        if (parent.section_level <= level - 1) {
            if (parent.section_level <= level - 1 && parent.section_label.equals(label)) {
                return parent;
            } else if (!parent.child_sections.isEmpty()) {
                for (WikiSection child_section : parent.child_sections) {
                    WikiSection section = findSection(label, child_section, level);

                    if (section != null) {
                        return section;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if it contains a specific section label.
     *
     * @param section
     * @return
     */
    public boolean hasSection(String section) {
        if (this.section_label.equals(section)) {
            return true;
        }

        if (this.child_sections.isEmpty()) {
            return false;
        }

        for (WikiSection child_section : child_sections) {
            boolean has_section = child_section.hasSection(section);
            if (has_section) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a specific section object.
     *
     * @param label
     * @return
     */
    public WikiSection findSection(String label) {
        if (section_label.equals(label)) {
            return this;
        } else if (!child_sections.isEmpty()) {
            for (WikiSection child_section : child_sections) {
                WikiSection section = child_section.findSection(label);
                if (section != null) {
                    return section;
                }
            }
        }
        return null;
    }

    /**
     * Gets all the section keys.
     *
     * @param keys
     */
    public void getSectionKeys(Set<String> keys) {
        keys.add(section_label);

        if (!child_sections.isEmpty()) {
            for (WikiSection child_section : child_sections) {
                child_section.getSectionKeys(keys);
            }
        }
    }


    /**
     * Gets all the section keys.
     *
     * @param keys
     */
    public void getSectionKeys(Set<String> keys, int level) {
        if (section_level > level) {
            return;
        }
        keys.add(section_label);

        if (!child_sections.isEmpty()) {
            for (WikiSection child_section : child_sections) {
                child_section.getSectionKeys(keys, level);
            }
        }
    }

    @Override
    public String toString() {
        return StringUtils.repeat("=", section_level) + section_label + StringUtils.repeat("=", section_level);
    }

}
