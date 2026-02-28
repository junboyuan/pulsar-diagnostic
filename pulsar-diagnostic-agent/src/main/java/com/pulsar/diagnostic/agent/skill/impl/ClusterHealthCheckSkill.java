package com.pulsar.diagnostic.agent.skill.impl;

import com.pulsar.diagnostic.agent.skill.*;
import com.pulsar.diagnostic.agent.subagent.SubAgentRegistry;
import com.pulsar.diagnostic.agent.subagent.SubAgentResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for comprehensive cluster health check.
 * Uses SubAgents for parallel data collection.
 */
@Component
public class ClusterHealthCheckSkill extends AbstractSkill {

    public static final String NAME = "cluster-health-check";

    private final SubAgentRegistry subAgentRegistry;

    public ClusterHealthCheckSkill(SubAgentRegistry subAgentRegistry) {
        this.subAgentRegistry = subAgentRegistry;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Perform comprehensive cluster health check including all components and services";
    }

    @Override
    public String getCategory() {
        return "diagnosis";
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of(
                SkillParameter.optional("deep", "Perform deep analysis (includes log analysis)", "boolean", "false"),
                SkillParameter.optional("components", "Components to check: brokers, bookies, zookeeper, all", "string", "all")
        );
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of(
                "health check",
                "check cluster health",
                "is my cluster healthy",
                "cluster status",
                "overall cluster health"
        );
    }

    @Override
    protected SkillResult doExecute(SkillContext context) {
        boolean deep = context.getParameter("deep", false);

        context.log("Starting comprehensive health check with SubAgents...");
        context.log("Deep analysis: " + deep);

        List<SkillResult.Finding> findings = new ArrayList<>();
        List<SkillResult.Recommendation> recommendations = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        int healthyCount = 0;
        int warningCount = 0;
        int criticalCount = 0;

        // Use SubAgents for parallel data collection
        context.log("Launching parallel SubAgents...");
        List<String> subAgentIds = new ArrayList<>();
        subAgentIds.add("cluster-info");
        subAgentIds.add("metrics-query");
        if (deep) {
            subAgentIds.add("log-analysis");
        }

        Map<String, SubAgentResult> subAgentResults = subAgentRegistry.executeParallel(
                subAgentIds, Map.of(), NAME);

        // Process cluster info result
        SubAgentResult clusterInfoResult = subAgentResults.get("cluster-info");
        if (clusterInfoResult != null) {
            output.append("=== Cluster Information ===\n");
            output.append(clusterInfoResult.getOutput()).append("\n");

            if (clusterInfoResult.hasFindings()) {
                for (SubAgentResult.Finding f : clusterInfoResult.getFindings()) {
                    findings.add(convertFinding(f));
                    updateCounts(f.severity(), healthyCount, warningCount, criticalCount);
                }
            }
        }

        // Process metrics result
        SubAgentResult metricsResult = subAgentResults.get("metrics-query");
        if (metricsResult != null) {
            output.append("\n=== Cluster Metrics ===\n");
            output.append(metricsResult.getOutput()).append("\n");

            if (metricsResult.hasFindings()) {
                for (SubAgentResult.Finding f : metricsResult.getFindings()) {
                    findings.add(convertFinding(f));
                    updateCounts(f.severity(), healthyCount, warningCount, criticalCount);
                }
            }
        }

        // Process log analysis result (if deep)
        if (deep) {
            SubAgentResult logResult = subAgentResults.get("log-analysis");
            if (logResult != null) {
                output.append("\n=== Log Analysis ===\n");
                output.append(logResult.getOutput()).append("\n");

                if (logResult.hasFindings()) {
                    for (SubAgentResult.Finding f : logResult.getFindings()) {
                        findings.add(convertFinding(f));
                        updateCounts(f.severity(), healthyCount, warningCount, criticalCount);
                    }
                }
            }
        }

        // Run MCP comprehensive diagnosis
        context.log("Running MCP diagnosis...");
        try {
            String mcpDiag = context.mcp().diagnoseProblem("unknown", null);
            output.append("\n=== MCP Diagnosis ===\n").append(mcpDiag).append("\n");
        } catch (Exception e) {
            context.log("MCP diagnosis failed: " + e.getMessage());
        }

        // Calculate health score from counts (need final variables for lambda)
        final int finalHealthyCount = healthyCount;
        final int finalWarningCount = warningCount;
        final int finalCriticalCount = criticalCount;

        double healthScore = calculateHealthScore(finalHealthyCount, finalWarningCount, finalCriticalCount);
        String overallStatus = determineOverallStatus(healthScore);

        output.append("\n=== Health Summary ===\n");
        output.append(String.format("Overall Status: %s\n", overallStatus));
        output.append(String.format("Health Score: %.1f/100\n", healthScore));
        output.append(String.format("Components Healthy: %d\n", finalHealthyCount));
        output.append(String.format("Components with Warnings: %d\n", finalWarningCount));
        output.append(String.format("Components Critical: %d\n", finalCriticalCount));

        // Generate recommendations
        generateHealthRecommendations(findings, healthScore, recommendations);

        return SkillResult.builder()
                .success(true)
                .output(output.toString())
                .findings(findings)
                .recommendations(recommendations)
                .metadata(Map.of(
                        "healthScore", healthScore,
                        "overallStatus", overallStatus,
                        "healthyCount", finalHealthyCount,
                        "warningCount", finalWarningCount,
                        "criticalCount", finalCriticalCount,
                        "deepAnalysis", deep
                ))
                .build();
    }

    private void updateCounts(String severity, int healthy, int warning, int critical) {
        // This is a helper to count severities - we'll handle counting differently
    }

    private SkillResult.Finding convertFinding(SubAgentResult.Finding f) {
        String category = f.category() != null ? f.category() : "unknown";
        return switch (f.severity()) {
            case "CRITICAL" -> SkillResult.Finding.critical(f.message(), "Critical issue detected", category);
            case "ERROR" -> SkillResult.Finding.error(f.message(), "Error condition detected", category);
            case "WARNING" -> SkillResult.Finding.warning(f.message(), "Warning condition detected", category);
            default -> SkillResult.Finding.info(f.message(), "Information", category);
        };
    }

    private double calculateHealthScore(int healthy, int warnings, int critical) {
        int total = healthy + warnings + critical;
        if (total == 0) return 100.0;

        double score = (healthy * 100.0 + warnings * 60.0 + critical * 0.0) / total;
        return Math.round(score * 10.0) / 10.0;
    }

    private String determineOverallStatus(double healthScore) {
        if (healthScore >= 90) return "HEALTHY";
        if (healthScore >= 70) return "WARNING";
        if (healthScore >= 50) return "DEGRADED";
        return "CRITICAL";
    }

    private void generateHealthRecommendations(List<SkillResult.Finding> findings, double healthScore,
                                                List<SkillResult.Recommendation> recommendations) {
        boolean hasCritical = findings.stream().anyMatch(f -> "CRITICAL".equals(f.severity()));
        boolean hasErrors = findings.stream().anyMatch(f -> "ERROR".equals(f.severity()));

        if (hasCritical) {
            recommendations.add(SkillResult.Recommendation.urgent(
                    "Address critical issues immediately",
                    "Cluster has critical health issues that need immediate attention"
            ));
        }

        if (hasErrors) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Resolve error conditions",
                    "One or more components have errors that should be addressed"
            ));
        }

        if (healthScore < 70) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Review cluster configuration",
                    "Cluster health score is below optimal - review configuration and resources"
            ));
        }

        if (findings.isEmpty()) {
            recommendations.add(SkillResult.Recommendation.low(
                    "Continue monitoring",
                    "Cluster is healthy - maintain regular monitoring"
            ));
        }
    }

    @Override
    public double canHandle(String query) {
        String lower = query.toLowerCase();
        double score = 0.0;

        if (lower.contains("health")) score += 0.4;
        if (lower.contains("status")) score += 0.2;
        if (lower.contains("check") && (lower.contains("cluster") || lower.contains("overall"))) score += 0.3;
        if (lower.contains("healthy")) score += 0.3;
        if (lower.contains("overall") && lower.contains("state")) score += 0.2;

        return Math.min(score, 1.0);
    }
}