package com.pulsar.diagnostic.agent.skill;

import java.util.List;
import java.util.Map;

/**
 * Client interface for calling MCP tools within skills.
 */
public interface McpSkillClient {

    /**
     * Call an MCP tool
     * @param toolName Name of the tool to call
     * @param arguments Arguments to pass to the tool
     * @return Tool result as string
     */
    String callTool(String toolName, Map<String, Object> arguments);

    /**
     * Inspect cluster components
     */
    default String inspectCluster(List<String> components) {
        return callTool("inspect_cluster", Map.of("components", components));
    }

    /**
     * Analyze logs for a component
     */
    default String analyzeLogs(String component, int maxLines) {
        return callTool("analyze_logs", Map.of("component", component, "max_lines", maxLines));
    }

    /**
     * Query Prometheus metrics
     */
    default String queryMetrics(String query, boolean detectAnomalies) {
        return callTool("query_metrics", Map.of("query", query, "detect_anomalies", detectAnomalies));
    }

    /**
     * Diagnose a specific topic
     */
    default String diagnoseTopic(String topic, boolean checkBacklog, boolean checkSubscriptions) {
        return callTool("diagnose_topic", Map.of(
                "topic", topic,
                "check_backlog", checkBacklog,
                "check_subscriptions", checkSubscriptions
        ));
    }

    /**
     * Diagnose a subscription
     */
    default String diagnoseSubscription(String topic, String subscription) {
        return callTool("diagnose_subscription", Map.of("topic", topic, "subscription", subscription));
    }

    /**
     * Comprehensive problem diagnosis
     */
    default String diagnoseProblem(String problemType, String resource) {
        return callTool("diagnose_problem", Map.of("problem_type", problemType, "resource", resource));
    }
}