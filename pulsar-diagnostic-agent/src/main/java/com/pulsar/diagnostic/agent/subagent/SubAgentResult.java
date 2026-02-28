package com.pulsar.diagnostic.agent.subagent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of a subagent execution.
 */
public class SubAgentResult {

    private final boolean success;
    private final String subAgentId;
    private final String output;
    private final String error;
    private final List<Finding> findings;
    private final Map<String, Object> data;
    private final Duration duration;
    private final String executionLog;
    private final Instant completedAt;

    private SubAgentResult(Builder builder) {
        this.success = builder.success;
        this.subAgentId = builder.subAgentId;
        this.output = builder.output;
        this.error = builder.error;
        this.findings = builder.findings;
        this.data = builder.data;
        this.duration = builder.duration;
        this.executionLog = builder.executionLog;
        this.completedAt = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSubAgentId() {
        return subAgentId;
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

    public Map<String, Object> getData() {
        return data;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Get a data value by key
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return data != null ? (T) data.get(key) : null;
    }

    /**
     * Check if there are any findings
     */
    public boolean hasFindings() {
        return findings != null && !findings.isEmpty();
    }

    /**
     * Format result for display
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SubAgent: ").append(subAgentId).append(" ===\n");
        sb.append("Status: ").append(success ? "SUCCESS" : "FAILED").append("\n");
        sb.append("Duration: ").append(duration != null ? duration.toMillis() + "ms" : "N/A").append("\n");

        if (output != null && !output.isEmpty()) {
            sb.append("\n").append(output).append("\n");
        }

        if (error != null && !error.isEmpty()) {
            sb.append("\nError: ").append(error).append("\n");
        }

        if (hasFindings()) {
            sb.append("\nFindings:\n");
            for (Finding f : findings) {
                sb.append("  - [").append(f.severity()).append("] ").append(f.message()).append("\n");
            }
        }

        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = true;
        private String subAgentId;
        private String output = "";
        private String error;
        private List<Finding> findings = List.of();
        private Map<String, Object> data = Map.of();
        private Duration duration;
        private String executionLog = "";

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder subAgentId(String subAgentId) {
            this.subAgentId = subAgentId;
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

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder addData(String key, Object value) {
            this.data = new java.util.HashMap<>(this.data);
            this.data.put(key, value);
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder executionLog(String executionLog) {
            this.executionLog = executionLog;
            return this;
        }

        public SubAgentResult build() {
            return new SubAgentResult(this);
        }
    }

    /**
     * A finding discovered by a subagent
     */
    public record Finding(
            String severity,  // INFO, WARNING, ERROR, CRITICAL
            String message,
            String category,
            Map<String, Object> details
    ) {
        public static Finding info(String message) {
            return new Finding("INFO", message, null, Map.of());
        }

        public static Finding info(String message, String category) {
            return new Finding("INFO", message, category, Map.of());
        }

        public static Finding warning(String message) {
            return new Finding("WARNING", message, null, Map.of());
        }

        public static Finding warning(String message, String category) {
            return new Finding("WARNING", message, category, Map.of());
        }

        public static Finding error(String message) {
            return new Finding("ERROR", message, null, Map.of());
        }

        public static Finding error(String message, String category) {
            return new Finding("ERROR", message, category, Map.of());
        }

        public static Finding critical(String message) {
            return new Finding("CRITICAL", message, null, Map.of());
        }

        public static Finding critical(String message, String category) {
            return new Finding("CRITICAL", message, category, Map.of());
        }
    }

    /**
     * Merge multiple results into one
     */
    public static SubAgentResult merge(List<SubAgentResult> results) {
        if (results == null || results.isEmpty()) {
            return builder().output("No results to merge").build();
        }

        boolean allSuccess = results.stream().allMatch(SubAgentResult::isSuccess);
        StringBuilder outputBuilder = new StringBuilder();
        List<Finding> allFindings = new java.util.ArrayList<>();
        Map<String, Object> allData = new java.util.HashMap<>();
        Duration totalDuration = Duration.ZERO;

        for (SubAgentResult result : results) {
            if (result.getOutput() != null) {
                outputBuilder.append("--- ").append(result.getSubAgentId()).append(" ---\n");
                outputBuilder.append(result.getOutput()).append("\n\n");
            }
            if (result.getFindings() != null) {
                allFindings.addAll(result.getFindings());
            }
            if (result.getData() != null) {
                allData.putAll(result.getData());
            }
            if (result.getDuration() != null) {
                totalDuration = totalDuration.plus(result.getDuration());
            }
        }

        return builder()
                .success(allSuccess)
                .subAgentId("merged")
                .output(outputBuilder.toString())
                .findings(allFindings)
                .data(allData)
                .duration(totalDuration)
                .build();
    }
}