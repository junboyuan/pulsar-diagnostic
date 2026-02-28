package com.pulsar.diagnostic.agent.skill;

import java.util.List;
import java.util.Map;

/**
 * Client interface for querying the knowledge base within skills.
 */
public interface KnowledgeSkillClient {

    /**
     * Check if knowledge base is ready
     */
    boolean isReady();

    /**
     * Search for relevant knowledge
     * @param query Search query
     * @param topK Number of results to return
     * @return List of relevant knowledge snippets
     */
    List<String> search(String query, int topK);

    /**
     * Search with context for better results
     * @param query Search query
     * @param topK Number of results
     * @return Knowledge context with related information
     */
    KnowledgeContext searchWithContext(String query, int topK);

    /**
     * Search by category
     * @param query Search query
     * @param category Category filter (e.g., "troubleshooting", "best-practices", "configuration")
     * @param topK Number of results
     * @return List of matching knowledge
     */
    List<String> searchByCategory(String query, String category, int topK);

    /**
     * Get troubleshooting guide for a specific issue
     */
    default String getTroubleshootingGuide(String issueType) {
        List<String> results = searchByCategory(issueType, "troubleshooting", 3);
        return String.join("\n\n---\n\n", results);
    }

    /**
     * Get best practices for a component
     */
    default String getBestPractices(String component) {
        List<String> results = searchByCategory(component + " best practices", "best-practices", 5);
        return String.join("\n\n---\n\n", results);
    }

    /**
     * Get configuration recommendations
     */
    default String getConfigurationRecommendations(String component) {
        List<String> results = searchByCategory(component + " configuration", "configuration", 5);
        return String.join("\n\n---\n\n", results);
    }

    /**
     * Knowledge context from search
     */
    record KnowledgeContext(
            String query,
            List<KnowledgeItem> items,
            String context
    ) {}

    record KnowledgeItem(
            String id,
            String content,
            Map<String, Object> metadata
    ) {}
}