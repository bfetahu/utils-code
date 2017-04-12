package utils;

import entities.WikipediaEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by besnik on 2/9/17.
 */
public class WikiQualityUtils {
    /**
     * Check if the current revision is featured content
     * We look for the following tags.
     * {{Featured article}} {{Featured list}} {{Featured portal}} {{Good article}} {{Spoken Wikipedia}} {{Spoken Wikipedia boilerplate}}
     *
     * @param entity
     * @return
     */
    public static boolean isFeaturedArticle(WikipediaEntity entity) {
        String revision_tmp = entity.getContent().toLowerCase();
        boolean is_featured = revision_tmp.contains("{{featured article}}") || revision_tmp.contains("{{featured list}}") ||
                revision_tmp.contains("{{featured portal}}") || revision_tmp.contains("{{spoken wikipedia}}") || revision_tmp.contains("{{spoken wikipedia boilerplate}}");
        return is_featured;
    }

    public static boolean isGoodArticle(WikipediaEntity entity) {
        String revision_tmp = entity.getContent().toLowerCase();
        boolean is_featured = revision_tmp.contains("{{good article}}");
        return is_featured;
    }

    /**
     * Check if the article is marked for deletion.
     *
     * @param entity
     * @return
     */
    public static boolean isMarkedForDeletion(WikipediaEntity entity) {
        String content = entity.getContent().toLowerCase();

        boolean is_marked_for_deletion = content.contains("{{cleanup") || content.contains("{{afd");
        return is_marked_for_deletion;
    }

    /**
     * Check if an article is marked as violating the NPOV policy.
     *
     * @param entity
     * @return
     */
    public static Set<String> hasNPOVViolations(WikipediaEntity entity) {
        return extractSectionsWithQualityIssues("{{pov", entity);
    }

    /**
     * Check if an article is marked having an unbalanced view point.
     *
     * @param entity
     * @return
     */
    public static Set<String> hasUnbalancedStance(WikipediaEntity entity) {
        return extractSectionsWithQualityIssues("{{unbalanced", entity);
    }

    /**
     * Check if an article is marked as contradictory or if some of its sections are marked as contradictory.
     *
     * @param entity
     * @return
     */
    public static Set<String> hasContradictorySections(WikipediaEntity entity) {
        return extractSectionsWithQualityIssues("{{contradict", entity);
    }

    /**
     * Check if the article is marked with having confusing content, and extract the respective sections.
     *
     * @param entity
     * @return
     */
    public static Set<String> hasConfusingSections(WikipediaEntity entity) {
        return extractSectionsWithQualityIssues("{{confusing", entity);
    }

    /**
     * Check if the article has factual inconsistencies or the facts are inaccurate. Return the corresponding sections.
     *
     * @param entity
     * @return
     */
    public static Set<String> hasFactualInaccuracies(WikipediaEntity entity) {
        return extractSectionsWithQualityIssues("{{disputed", entity);
    }

    /**
     * Extract the sections that have a specific quality issue. For example, {{contradict}}, {{confusing}} etc.
     *
     * @param quality_tag
     * @param entity
     * @return
     */
    public static Set<String> extractSectionsWithQualityIssues(String quality_tag, WikipediaEntity entity) {
        boolean has_quality_issue = entity.getContent().contains(quality_tag);

        //if it is a contradictory article then find all the sections which contain the contradictory parts.
        if (has_quality_issue) {
            //extract all the sections that are marked as contradictory.
            Set<String> contradictory_sections = new HashSet<>();
            for (String section : entity.getSectionKeys()) {
                String section_text = entity.getSectionText(section);
                if (section_text.contains(quality_tag)) {
                    contradictory_sections.add(section);
                }
            }

            //if its empty then we know that the entire article is marked as contradictory
            return contradictory_sections;
        }
        return null;
    }


}
