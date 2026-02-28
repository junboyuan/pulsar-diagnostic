package com.pulsar.diagnostic.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for skills with common functionality.
 */
public abstract class AbstractSkill implements Skill {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public SkillResult execute(SkillContext context) {
        log.info("Executing skill: {}", getName());
        context.log("Starting skill: " + getName());

        long startTime = System.currentTimeMillis();

        try {
            SkillResult result = doExecute(context);
            long duration = System.currentTimeMillis() - startTime;

            context.log("Skill completed in " + duration + "ms");

            return SkillResult.builder()
                    .success(result.isSuccess())
                    .output(result.getOutput())
                    .error(result.getError())
                    .findings(result.getFindings())
                    .recommendations(result.getRecommendations())
                    .metadata(result.getMetadata())
                    .executionLog(context.getExecutionLog())
                    .build();

        } catch (Exception e) {
            log.error("Skill execution failed: {}", getName(), e);
            return SkillResult.builder()
                    .error("Skill execution failed: " + e.getMessage())
                    .executionLog(context.getExecutionLog())
                    .build();
        }
    }

    /**
     * Subclasses implement this method to perform the actual skill logic.
     */
    protected abstract SkillResult doExecute(SkillContext context);

    /**
     * Helper to create a successful result with output
     */
    protected SkillResult success(String output) {
        return SkillResult.builder()
                .success(true)
                .output(output)
                .build();
    }

    /**
     * Helper to create a failed result with error
     */
    protected SkillResult failure(String error) {
        return SkillResult.builder()
                .error(error)
                .build();
    }

    /**
     * Helper to parse number from string
     */
    protected double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Helper to extract value from structured output
     */
    protected String extractValue(String output, String key) {
        if (output == null) return null;
        for (String line : output.split("\n")) {
            if (line.toLowerCase().contains(key.toLowerCase())) {
                int colonIndex = line.indexOf(":");
                if (colonIndex > 0 && colonIndex < line.length() - 1) {
                    return line.substring(colonIndex + 1).trim();
                }
            }
        }
        return null;
    }

    /**
     * Check if output contains an error indicator
     */
    protected boolean hasError(String output) {
        if (output == null) return true;
        String lower = output.toLowerCase();
        return lower.contains("error") || lower.contains("failed") || lower.contains("exception");
    }

    /**
     * Default parameter lists - override in subclasses
     */
    @Override
    public List<SkillParameter> getRequiredParameters() {
        return List.of();
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of();
    }

    @Override
    public String getCategory() {
        return "general";
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of();
    }

    @Override
    public double canHandle(String query) {
        String lowerQuery = query.toLowerCase();
        String name = getName().toLowerCase().replace("-", " ");
        String desc = getDescription().toLowerCase();

        double score = 0.0;

        // Check name match
        if (lowerQuery.contains(name)) {
            score += 0.3;
        }

        // Check description keywords
        String[] descWords = desc.split("\\s+");
        for (String word : descWords) {
            if (word.length() > 3 && lowerQuery.contains(word)) {
                score += 0.1;
            }
        }

        // Check example prompts
        for (String example : getExamplePrompts()) {
            if (lowerQuery.contains(example.toLowerCase())) {
                score += 0.2;
                break;
            }
        }

        return Math.min(score, 1.0);
    }
}