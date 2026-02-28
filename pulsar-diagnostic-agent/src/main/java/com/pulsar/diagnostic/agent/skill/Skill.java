package com.pulsar.diagnostic.agent.skill;

import java.util.List;
import java.util.Map;

/**
 * Interface for diagnostic skills.
 * Skills encapsulate complex multi-step diagnostic workflows.
 */
public interface Skill {

    /**
     * Get the skill name/identifier
     */
    String getName();

    /**
     * Get a human-readable description of what this skill does
     */
    String getDescription();

    /**
     * Get the skill category (e.g., "diagnosis", "analysis", "troubleshooting", "consultation")
     */
    String getCategory();

    /**
     * Get required parameters for this skill
     */
    List<SkillParameter> getRequiredParameters();

    /**
     * Get optional parameters for this skill
     */
    List<SkillParameter> getOptionalParameters();

    /**
     * Execute the skill with the given context
     * @param context The execution context containing tools, knowledge, and parameters
     * @return The skill execution result
     */
    SkillResult execute(SkillContext context);

    /**
     * Check if this skill can handle the given query
     * @param query The user query
     * @return Confidence score (0.0 to 1.0) indicating how well this skill matches
     */
    default double canHandle(String query) {
        return 0.0;
    }

    /**
     * Get example prompts that this skill can handle
     */
    default List<String> getExamplePrompts() {
        return List.of();
    }

    /**
     * Validate parameters before execution
     * @param parameters The parameters to validate
     * @return Validation result
     */
    default ValidationResult validateParameters(Map<String, Object> parameters) {
        for (SkillParameter required : getRequiredParameters()) {
            if (!parameters.containsKey(required.name())) {
                return ValidationResult.failure("Missing required parameter: " + required.name());
            }
        }
        return ValidationResult.success();
    }

    /**
     * Parameter definition for a skill
     */
    record SkillParameter(
            String name,
            String description,
            String type,
            boolean required,
            String defaultValue
    ) {
        public static SkillParameter required(String name, String description, String type) {
            return new SkillParameter(name, description, type, true, null);
        }

        public static SkillParameter optional(String name, String description, String type, String defaultValue) {
            return new SkillParameter(name, description, type, false, defaultValue);
        }
    }

    /**
     * Validation result
     */
    record ValidationResult(boolean isValid, String errorMessage) {
        static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}