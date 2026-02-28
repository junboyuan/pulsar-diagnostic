package com.pulsar.diagnostic.agent.subagent;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Interface for diagnostic subagents.
 * Subagents are independent agents that can execute tasks in parallel
 * without requiring context from other subagents.
 */
public interface SubAgent {

    /**
     * Get the unique identifier for this subagent
     */
    String getId();

    /**
     * Get a human-readable name
     */
    String getName();

    /**
     * Get the description of what this subagent does
     */
    String getDescription();

    /**
     * Get the set of capabilities this subagent provides
     * E.g., "log-analysis", "metrics-query", "cluster-info"
     */
    Set<String> getCapabilities();

    /**
     * Get the expected execution duration
     */
    default Duration getExpectedDuration() {
        return Duration.ofSeconds(10);
    }

    /**
     * Check if this subagent can handle the given task
     * @param taskType The type of task
     * @param parameters Task parameters
     * @return Confidence score (0.0 to 1.0)
     */
    double canHandle(String taskType, Map<String, Object> parameters);

    /**
     * Execute the subagent task
     * @param context Execution context
     * @return Execution result
     */
    SubAgentResult execute(SubAgentContext context);

    /**
     * Get required parameter names
     */
    default Set<String> getRequiredParameters() {
        return Set.of();
    }

    /**
     * Get optional parameter names with defaults
     */
    default Map<String, Object> getDefaultParameters() {
        return Map.of();
    }

    /**
     * Validate parameters before execution
     */
    default ValidationResult validateParameters(Map<String, Object> parameters) {
        for (String required : getRequiredParameters()) {
            if (!parameters.containsKey(required)) {
                return ValidationResult.failure("Missing required parameter: " + required);
            }
        }
        return ValidationResult.success();
    }

    /**
     * Whether this subagent can run in parallel with others
     */
    default boolean isParallelizable() {
        return true;
    }

    /**
     * Priority for execution ordering (higher = more important)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Validation result
     */
    record ValidationResult(boolean isValid, String errorMessage) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}