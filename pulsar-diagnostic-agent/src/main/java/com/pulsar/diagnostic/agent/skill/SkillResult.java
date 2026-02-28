package com.pulsar.diagnostic.agent.skill;

import java.util.List;
import java.util.Map;

/**
 * Result of a skill execution.
 */
public class SkillResult {

    private final boolean success;
    private final String output;
    private final String error;
    private final List<Finding> findings;
    private final List<Recommendation> recommendations;
    private final Map<String, Object> metadata;
    private final String executionLog;

    private SkillResult(Builder builder) {
        this.success = builder.success;
        this.output = builder.output;
        this.error = builder.error;
        this.findings = builder.findings;
        this.recommendations = builder.recommendations;
        this.metadata = builder.metadata;
        this.executionLog = builder.executionLog;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    /**
     * Format the result as a human-readable string
     */
    public String format() {
        StringBuilder sb = new StringBuilder();

        if (!success) {
            sb.append("❌ Skill execution failed: ").append(error).append("\n");
            return sb.toString();
        }

        sb.append(output).append("\n");

        if (!findings.isEmpty()) {
            sb.append("\n=== Findings ===\n");
            for (int i = 0; i < findings.size(); i++) {
                Finding f = findings.get(i);
                sb.append(String.format("%d. [%s] %s\n", i + 1, f.severity(), f.title()));
                sb.append("   ").append(f.description()).append("\n");
                if (f.affectedResource() != null) {
                    sb.append("   Affected: ").append(f.affectedResource()).append("\n");
                }
            }
        }

        if (!recommendations.isEmpty()) {
            sb.append("\n=== Recommendations ===\n");
            for (int i = 0; i < recommendations.size(); i++) {
                Recommendation r = recommendations.get(i);
                sb.append(String.format("%d. %s\n", i + 1, r.description()));
                if (r.action() != null) {
                    sb.append("   Action: ").append(r.action()).append("\n");
                }
                if (r.priority() != null) {
                    sb.append("   Priority: ").append(r.priority()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = true;
        private String output = "";
        private String error;
        private List<Finding> findings = List.of();
        private List<Recommendation> recommendations = List.of();
        private Map<String, Object> metadata = Map.of();
        private String executionLog = "";

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            this.success = false;
            return this;
        }

        public Builder findings(List<Finding> findings) {
            this.findings = findings;
            return this;
        }

        public Builder addFinding(Finding finding) {
            this.findings = new java.util.ArrayList<>(this.findings);
            this.findings.add(finding);
            return this;
        }

        public Builder recommendations(List<Recommendation> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public Builder addRecommendation(Recommendation recommendation) {
            this.recommendations = new java.util.ArrayList<>(this.recommendations);
            this.recommendations.add(recommendation);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder executionLog(String executionLog) {
            this.executionLog = executionLog;
            return this;
        }

        public SkillResult build() {
            return new SkillResult(this);
        }
    }

    /**
     * A finding discovered during skill execution
     */
    public record Finding(
            String title,
            String description,
            String severity,  // INFO, WARNING, ERROR, CRITICAL
            String affectedResource,
            Map<String, Object> details
    ) {
        public static Finding info(String title, String description) {
            return new Finding(title, description, "INFO", null, Map.of());
        }

        public static Finding info(String title, String description, String affectedResource) {
            return new Finding(title, description, "INFO", affectedResource, Map.of());
        }

        public static Finding warning(String title, String description, String affectedResource) {
            return new Finding(title, description, "WARNING", affectedResource, Map.of());
        }

        public static Finding error(String title, String description, String affectedResource) {
            return new Finding(title, description, "ERROR", affectedResource, Map.of());
        }

        public static Finding critical(String title, String description, String affectedResource) {
            return new Finding(title, description, "CRITICAL", affectedResource, Map.of());
        }
    }

    /**
     * A recommendation for action
     */
    public record Recommendation(
            String description,
            String action,
            String priority,  // LOW, MEDIUM, HIGH, URGENT
            String category
    ) {
        public static Recommendation low(String description, String action) {
            return new Recommendation(description, action, "LOW", null);
        }

        public static Recommendation medium(String description, String action) {
            return new Recommendation(description, action, "MEDIUM", null);
        }

        public static Recommendation high(String description, String action) {
            return new Recommendation(description, action, "HIGH", null);
        }

        public static Recommendation urgent(String description, String action) {
            return new Recommendation(description, action, "URGENT", null);
        }
    }
}