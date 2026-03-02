package com.pulsar.diagnostic.agent.skill.declarative;

import java.util.List;
import java.util.Map;

/**
 * Represents a skill definition loaded from YAML configuration.
 */
public record SkillDefinition(
        String name,
        String description,
        String category,
        String systemPrompt,
        List<String> availableTools,
        SkillParameters parameters,
        List<String> examplePrompts
) {
    public record SkillParameters(
            List<SkillParameterDef> required,
            List<SkillParameterDef> optional
    ) {}

    public record SkillParameterDef(
            String name,
            String type,
            String description,
            Object defaultValue
    ) {}

    /**
     * Get the skill name as a key for registry
     */
    public String getKey() {
        return name != null ? name.replace("-", "_") : null;
    }

    /**
     * Calculate confidence score for handling a query
     */
    public double canHandle(String query) {
        if (query == null || query.isEmpty()) {
            return 0.0;
        }

        String lowerQuery = query.toLowerCase();
        double score = 0.0;

        // Check name match
        if (name != null && lowerQuery.contains(name.toLowerCase().replace("-", " "))) {
            score += 0.3;
        }

        // Check description keywords
        if (description != null) {
            String[] descWords = description.toLowerCase().split("\\s+");
            for (String word : descWords) {
                if (word.length() > 3 && lowerQuery.contains(word)) {
                    score += 0.05;
                }
            }
        }

        // Check example prompts
        if (examplePrompts != null) {
            for (String example : examplePrompts) {
                if (lowerQuery.contains(example.toLowerCase())) {
                    score += 0.2;
                    break;
                }
            }
        }

        return Math.min(score, 1.0);
    }
}