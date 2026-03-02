package com.pulsar.diagnostic.agent.skill.declarative;

/**
 * Result of a skill execution.
 */
public record SkillExecutionResult(
        boolean success,
        String skillName,
        String output,
        String error,
        long executionTimeMs,
        java.util.Map<String, Object> metadata
) {
    /**
     * Create a successful result
     */
    public static SkillExecutionResult success(String skillName, String output, long executionTimeMs) {
        return new SkillExecutionResult(true, skillName, output, null, executionTimeMs, java.util.Map.of());
    }

    /**
     * Create a successful result with metadata
     */
    public static SkillExecutionResult success(String skillName, String output, long executionTimeMs,
                                                java.util.Map<String, Object> metadata) {
        return new SkillExecutionResult(true, skillName, output, null, executionTimeMs, metadata);
    }

    /**
     * Create a failure result
     */
    public static SkillExecutionResult failure(String error) {
        return new SkillExecutionResult(false, null, null, error, 0, java.util.Map.of());
    }

    /**
     * Create a failure result with skill name
     */
    public static SkillExecutionResult failure(String skillName, String error) {
        return new SkillExecutionResult(false, skillName, null, error, 0, java.util.Map.of());
    }

    /**
     * Check if result has output
     */
    public boolean hasOutput() {
        return output != null && !output.isEmpty();
    }

    /**
     * Get output or error message
     */
    public String getOutputOrError() {
        return success ? output : "Error: " + error;
    }
}