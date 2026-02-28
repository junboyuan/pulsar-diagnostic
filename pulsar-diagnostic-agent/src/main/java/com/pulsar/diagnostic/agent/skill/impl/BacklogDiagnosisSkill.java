package com.pulsar.diagnostic.agent.skill.impl;

import com.pulsar.diagnostic.agent.skill.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for diagnosing message backlog issues.
 * Analyzes topics and subscriptions for backlog problems and provides recommendations.
 */
@Component
public class BacklogDiagnosisSkill extends AbstractSkill {

    public static final String NAME = "backlog-diagnosis";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Diagnose message backlog issues in topics and subscriptions, identify root causes and provide solutions";
    }

    @Override
    public String getCategory() {
        return "diagnosis";
    }

    @Override
    public List<SkillParameter> getRequiredParameters() {
        return List.of();
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of(
                SkillParameter.optional("topic", "Specific topic to diagnose (optional, if not provided will analyze all topics)", "string", null),
                SkillParameter.optional("namespace", "Namespace to analyze (optional)", "string", null),
                SkillParameter.optional("threshold", "Backlog threshold in messages to trigger warning (default: 10000)", "integer", "10000")
        );
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of(
                "check backlog",
                "diagnose backlog issue",
                "why is my topic growing",
                "message lag problem",
                "consumer not keeping up"
        );
    }

    @Override
    protected SkillResult doExecute(SkillContext context) {
        String topic = context.getParameter("topic");
        String namespace = context.getParameter("namespace");
        int threshold = context.getParameter("threshold", 10000);

        context.log("Starting backlog diagnosis...");
        context.log("Parameters: topic=" + topic + ", namespace=" + namespace + ", threshold=" + threshold);

        List<SkillResult.Finding> findings = new ArrayList<>();
        List<SkillResult.Recommendation> recommendations = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        // Step 1: Get cluster metrics to understand overall backlog
        context.log("Step 1: Analyzing cluster-wide backlog...");
        String metricsResult = context.tools().getClusterMetrics();
        output.append("=== Cluster Backlog Analysis ===\n").append(metricsResult).append("\n");

        // Extract total backlog from metrics
        String backlogStr = extractValue(metricsResult, "Total Backlog");
        long totalBacklog = (long) parseDouble(backlogStr, 0);

        if (totalBacklog > threshold) {
            findings.add(SkillResult.Finding.warning(
                    "High Cluster Backlog",
                    String.format("Total backlog of %d messages exceeds threshold of %d", totalBacklog, threshold),
                    "cluster"
            ));
        }

        // Step 2: If specific topic provided, diagnose it
        if (topic != null && !topic.isEmpty()) {
            context.log("Step 2: Diagnosing specific topic: " + topic);
            String topicResult = context.tools().checkTopicBacklog(topic);
            output.append("\n=== Topic Backlog Analysis ===\n").append(topicResult).append("\n");

            // Get subscriptions
            String subsResult = context.tools().getTopicSubscriptions(topic);
            output.append("\n=== Subscriptions ===\n").append(subsResult).append("\n");

            // Analyze subscriptions for issues
            analyzeSubscriptionsInOutput(subsResult, findings, recommendations, threshold);
        } else {
            // Step 3: Use MCP diagnose_problem for general backlog analysis
            context.log("Step 2: Running comprehensive backlog diagnosis...");
            String diagnosisResult = context.mcp().diagnoseProblem("backlog", namespace);
            output.append("\n=== Diagnosis Results ===\n").append(diagnosisResult).append("\n");
        }

        // Step 4: Get knowledge-based recommendations
        if (context.knowledge().isReady()) {
            context.log("Step 3: Getting knowledge-based recommendations...");
            String knowledge = context.knowledge().getTroubleshootingGuide("backlog");
            if (knowledge != null && !knowledge.isEmpty()) {
                output.append("\n=== Best Practices ===\n").append(knowledge).append("\n");
            }
        }

        // Add general recommendations if backlog is high
        if (!findings.isEmpty()) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Scale consumers to handle message rate",
                    "Increase consumer parallelism or add more consumer instances"
            ));
            recommendations.add(SkillResult.Recommendation.medium(
                    "Review consumer health",
                    "Check consumer logs for errors or processing bottlenecks"
            ));
            recommendations.add(SkillResult.Recommendation.medium(
                    "Consider dead letter queue",
                    "For persistent failures, configure DLQ to prevent message accumulation"
            ));
        }

        // Build result
        return SkillResult.builder()
                .success(true)
                .output(output.toString())
                .findings(findings)
                .recommendations(recommendations)
                .metadata(Map.of(
                        "totalBacklog", totalBacklog,
                        "threshold", threshold,
                        "topicAnalyzed", topic != null ? topic : "all"
                ))
                .build();
    }

    private void analyzeSubscriptionsInOutput(String subsResult,
                                               List<SkillResult.Finding> findings,
                                               List<SkillResult.Recommendation> recommendations,
                                               int threshold) {
        if (subsResult == null) return;

        // Check for backlog in subscriptions
        for (String line : subsResult.split("\n")) {
            if (line.toLowerCase().contains("backlog")) {
                String value = extractValue(line, "backlog");
                if (value != null) {
                    try {
                        long backlog = (long) parseDouble(value, 0);
                        if (backlog > threshold) {
                            findings.add(SkillResult.Finding.warning(
                                    "Subscription Backlog Detected",
                                    String.format("Subscription has backlog of %s messages", value),
                                    "subscription"
                            ));
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Check for consumers
            if (line.toLowerCase().contains("consumers: 0")) {
                findings.add(SkillResult.Finding.error(
                        "No Active Consumers",
                        "Subscription has no active consumers - messages will accumulate",
                        "subscription"
                ));
                recommendations.add(SkillResult.Recommendation.urgent(
                        "Start consumer for subscription",
                        "No consumers are connected to this subscription"
                ));
            }
        }
    }

    @Override
    public double canHandle(String query) {
        String lower = query.toLowerCase();
        double score = 0.0;

        // Strong indicators
        if (lower.contains("backlog")) score += 0.4;
        if (lower.contains("lag")) score += 0.3;
        if (lower.contains("consumer") && (lower.contains("slow") || lower.contains("stuck") || lower.contains("behind"))) score += 0.4;
        if (lower.contains("message") && (lower.contains("accumulat") || lower.contains("growing"))) score += 0.3;
        if (lower.contains("subscription") && lower.contains("issue")) score += 0.2;

        return Math.min(score, 1.0);
    }
}