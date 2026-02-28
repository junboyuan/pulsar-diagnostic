package com.pulsar.diagnostic.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Orchestrator for complex diagnostic workflows.
 * Chains multiple skills together and coordinates execution.
 */
@Component
public class SkillOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SkillOrchestrator.class);

    private final SkillRegistry skillRegistry;

    // Workflow definitions
    private final Map<String, WorkflowDefinition> workflows = new ConcurrentHashMap<>();

    public SkillOrchestrator(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
        registerDefaultWorkflows();
    }

    /**
     * Register default diagnostic workflows
     */
    private void registerDefaultWorkflows() {
        // Full diagnostic workflow - runs multiple skills in sequence
        registerWorkflow(WorkflowDefinition.builder()
                .name("full-diagnostic")
                .description("Comprehensive diagnostic covering all aspects")
                .addStep("cluster-health-check", Map.of("deep", true))
                .addStep("performance-analysis", Map.of())
                .addStep("backlog-diagnosis", Map.of())
                .build());

        // Troubleshooting workflow - focused on problem solving
        registerWorkflow(WorkflowDefinition.builder()
                .name("troubleshoot")
                .description("General troubleshooting workflow")
                .addStep("connectivity-troubleshoot", Map.of())
                .addStep("performance-analysis", Map.of())
                .addStep("backlog-diagnosis", Map.of())
                .build());

        // Performance optimization workflow
        registerWorkflow(WorkflowDefinition.builder()
                .name("performance-optimization")
                .description("Analyze and optimize cluster performance")
                .addStep("performance-analysis", Map.of())
                .addStep("capacity-planning", Map.of("growthRate", 20))
                .build());

        // Pre-maintenance check workflow
        registerWorkflow(WorkflowDefinition.builder()
                .name("pre-maintenance")
                .description("Check cluster health before maintenance")
                .addStep("cluster-health-check", Map.of("deep", true))
                .addStep("connectivity-troubleshoot", Map.of())
                .build());

        // Capacity review workflow
        registerWorkflow(WorkflowDefinition.builder()
                .name("capacity-review")
                .description("Review capacity and plan for growth")
                .addStep("cluster-health-check", Map.of())
                .addStep("performance-analysis", Map.of())
                .addStep("capacity-planning", Map.of())
                .build());
    }

    /**
     * Register a custom workflow
     */
    public void registerWorkflow(WorkflowDefinition workflow) {
        workflows.put(workflow.getName(), workflow);
        log.info("Registered workflow: {}", workflow.getName());
    }

    /**
     * Execute a workflow by name
     */
    public WorkflowResult executeWorkflow(String workflowName, Map<String, Object> parameters) {
        WorkflowDefinition workflow = workflows.get(workflowName);
        if (workflow == null) {
            return WorkflowResult.failed("Workflow not found: " + workflowName);
        }

        log.info("Executing workflow: {}", workflowName);
        long startTime = System.currentTimeMillis();

        List<WorkflowStepResult> stepResults = new ArrayList<>();
        List<SkillResult.Finding> allFindings = new ArrayList<>();
        List<SkillResult.Recommendation> allRecommendations = new ArrayList<>();
        StringBuilder combinedOutput = new StringBuilder();

        combinedOutput.append("=== Workflow: ").append(workflow.getDescription()).append(" ===\n\n");

        // Execute each step
        for (WorkflowStep step : workflow.getSteps()) {
            // Merge parameters (step params override workflow params)
            Map<String, Object> stepParams = new HashMap<>(parameters);
            stepParams.putAll(step.parameters());

            log.info("Executing workflow step: {}", step.skillName());

            // Execute skill
            SkillResult result = skillRegistry.execute(step.skillName(), stepParams);

            // Record step result
            WorkflowStepResult stepResult = new WorkflowStepResult(
                    step.skillName(),
                    result.isSuccess(),
                    result.getError(),
                    System.currentTimeMillis()
            );
            stepResults.add(stepResult);

            // Append output
            combinedOutput.append("--- Step: ").append(step.skillName()).append(" ---\n");
            combinedOutput.append(result.getOutput()).append("\n\n");

            // Collect findings and recommendations
            if (result.getFindings() != null) {
                allFindings.addAll(result.getFindings());
            }
            if (result.getRecommendations() != null) {
                allRecommendations.addAll(result.getRecommendations());
            }

            // Stop workflow if step fails and workflow is not set to continue on failure
            if (!result.isSuccess() && !workflow.isContinueOnFailure()) {
                log.warn("Workflow stopped due to step failure: {}", step.skillName());
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Deduplicate recommendations
        List<SkillResult.Recommendation> uniqueRecommendations = deduplicateRecommendations(allRecommendations);

        // Build final output
        combinedOutput.append("\n=== Workflow Summary ===\n");
        combinedOutput.append(String.format("Duration: %dms\n", duration));
        combinedOutput.append(String.format("Steps Completed: %d/%d\n",
                stepResults.stream().filter(WorkflowStepResult::success).count(),
                workflow.getSteps().size()));
        combinedOutput.append(String.format("Total Findings: %d\n", allFindings.size()));
        combinedOutput.append(String.format("Total Recommendations: %d\n", uniqueRecommendations.size()));

        return new WorkflowResult(
                workflowName,
                true,
                combinedOutput.toString(),
                stepResults,
                allFindings,
                uniqueRecommendations,
                duration
        );
    }

    /**
     * Execute multiple skills in parallel
     */
    public Map<String, SkillResult> executeParallel(List<String> skillNames, Map<String, Object> parameters) {
        log.info("Executing {} skills in parallel", skillNames.size());

        Map<String, CompletableFuture<SkillResult>> futures = new HashMap<>();

        for (String skillName : skillNames) {
            futures.put(skillName, CompletableFuture.supplyAsync(() ->
                    skillRegistry.execute(skillName, parameters)));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

        // Collect results
        return futures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            try {
                                return e.getValue().get();
                            } catch (Exception ex) {
                                return SkillResult.builder()
                                        .error("Parallel execution failed: " + ex.getMessage())
                                        .build();
                            }
                        }
                ));
    }

    /**
     * Auto-select and execute the best workflow for a query
     */
    public WorkflowResult executeBestMatch(String query, Map<String, Object> parameters) {
        // First, try to find a matching skill
        Skill bestSkill = skillRegistry.findBestMatch(query);

        if (bestSkill != null) {
            log.info("Found matching skill: {} for query", bestSkill.getName());
            SkillResult result = skillRegistry.execute(bestSkill.getName(), parameters);

            return new WorkflowResult(
                    bestSkill.getName(),
                    result.isSuccess(),
                    result.getOutput(),
                    List.of(new WorkflowStepResult(bestSkill.getName(), result.isSuccess(), result.getError(), System.currentTimeMillis())),
                    result.getFindings(),
                    result.getRecommendations(),
                    0
            );
        }

        // If no single skill matches, try to find a matching workflow
        WorkflowDefinition bestWorkflow = findBestWorkflow(query);
        if (bestWorkflow != null) {
            log.info("Found matching workflow: {} for query", bestWorkflow.getName());
            return executeWorkflow(bestWorkflow.getName(), parameters);
        }

        // Default to full diagnostic
        log.info("No specific match found, executing full diagnostic workflow");
        return executeWorkflow("full-diagnostic", parameters);
    }

    /**
     * Find the best matching workflow for a query
     */
    private WorkflowDefinition findBestWorkflow(String query) {
        String lower = query.toLowerCase();

        // Simple keyword matching
        if (lower.contains("troubleshoot") || lower.contains("problem") || lower.contains("issue")) {
            return workflows.get("troubleshoot");
        }
        if (lower.contains("performance") || lower.contains("optimize") || lower.contains("slow")) {
            return workflows.get("performance-optimization");
        }
        if (lower.contains("capacity") || lower.contains("scale") || lower.contains("plan")) {
            return workflows.get("capacity-review");
        }
        if (lower.contains("maintenance") || lower.contains("upgrade")) {
            return workflows.get("pre-maintenance");
        }
        if (lower.contains("health") || lower.contains("status") || lower.contains("check")) {
            return workflows.get("full-diagnostic");
        }

        return null;
    }

    /**
     * Get all available workflows
     */
    public Collection<WorkflowDefinition> getAvailableWorkflows() {
        return workflows.values();
    }

    /**
     * Get workflow by name
     */
    public WorkflowDefinition getWorkflow(String name) {
        return workflows.get(name);
    }

    /**
     * Deduplicate recommendations by description
     */
    private List<SkillResult.Recommendation> deduplicateRecommendations(List<SkillResult.Recommendation> recommendations) {
        Map<String, SkillResult.Recommendation> unique = new LinkedHashMap<>();
        for (SkillResult.Recommendation rec : recommendations) {
            unique.putIfAbsent(rec.description(), rec);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Workflow definition
     */
    public static class WorkflowDefinition {
        private final String name;
        private final String description;
        private final List<WorkflowStep> steps;
        private final boolean continueOnFailure;

        private WorkflowDefinition(Builder builder) {
            this.name = builder.name;
            this.description = builder.description;
            this.steps = builder.steps;
            this.continueOnFailure = builder.continueOnFailure;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<WorkflowStep> getSteps() { return steps; }
        public boolean isContinueOnFailure() { return continueOnFailure; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private String description;
            private List<WorkflowStep> steps = new ArrayList<>();
            private boolean continueOnFailure = true;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder addStep(String skillName, Map<String, Object> parameters) {
                this.steps.add(new WorkflowStep(skillName, parameters));
                return this;
            }

            public Builder continueOnFailure(boolean continueOnFailure) {
                this.continueOnFailure = continueOnFailure;
                return this;
            }

            public WorkflowDefinition build() {
                return new WorkflowDefinition(this);
            }
        }
    }

    /**
     * Workflow step definition
     */
    public record WorkflowStep(String skillName, Map<String, Object> parameters) {}

    /**
     * Result of a single workflow step
     */
    public record WorkflowStepResult(
            String skillName,
            boolean success,
            String error,
            long timestamp
    ) {}

    /**
     * Result of workflow execution
     */
    public record WorkflowResult(
            String workflowName,
            boolean success,
            String output,
            List<WorkflowStepResult> stepResults,
            List<SkillResult.Finding> findings,
            List<SkillResult.Recommendation> recommendations,
            long durationMs
    ) {
        public static WorkflowResult failed(String error) {
            return new WorkflowResult(
                    "unknown",
                    false,
                    error,
                    List.of(),
                    List.of(),
                    List.of(),
                    0
            );
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append(output).append("\n");

            if (!findings.isEmpty()) {
                sb.append("\n=== All Findings ===\n");
                for (int i = 0; i < findings.size(); i++) {
                    SkillResult.Finding f = findings.get(i);
                    sb.append(String.format("%d. [%s] %s\n", i + 1, f.severity(), f.title()));
                }
            }

            if (!recommendations.isEmpty()) {
                sb.append("\n=== All Recommendations ===\n");
                for (int i = 0; i < recommendations.size(); i++) {
                    SkillResult.Recommendation r = recommendations.get(i);
                    sb.append(String.format("%d. [%s] %s\n", i + 1, r.priority(), r.description()));
                }
            }

            return sb.toString();
        }
    }
}