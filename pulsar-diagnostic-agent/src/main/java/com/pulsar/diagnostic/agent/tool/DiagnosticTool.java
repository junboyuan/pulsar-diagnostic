package com.pulsar.diagnostic.agent.tool;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.common.enums.DiagnosticType;
import com.pulsar.diagnostic.common.enums.HealthStatus;
import com.pulsar.diagnostic.common.enums.Severity;
import com.pulsar.diagnostic.common.model.DiagnosticResult;
import com.pulsar.diagnostic.common.model.TopicInfo;
import com.pulsar.diagnostic.core.admin.PulsarAdminClient;
import com.pulsar.diagnostic.core.health.ClusterHealth;
import com.pulsar.diagnostic.core.health.ComponentHealth;
import com.pulsar.diagnostic.core.health.HealthCheckService;
import com.pulsar.diagnostic.core.logs.LogAnalysisService;
import com.pulsar.diagnostic.core.metrics.PrometheusMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool for diagnosing Pulsar issues.
 * Uses MCP server for comprehensive problem diagnosis.
 */
@Component
public class DiagnosticTool {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticTool.class);

    private final McpClient mcpClient;
    private final PulsarAdminClient pulsarAdminClient;
    private final HealthCheckService healthCheckService;
    private final PrometheusMetricsCollector metricsCollector;
    private final LogAnalysisService logAnalysisService;

    public DiagnosticTool(McpClient mcpClient,
                          PulsarAdminClient pulsarAdminClient,
                          HealthCheckService healthCheckService,
                          PrometheusMetricsCollector metricsCollector,
                          LogAnalysisService logAnalysisService) {
        this.mcpClient = mcpClient;
        this.pulsarAdminClient = pulsarAdminClient;
        this.healthCheckService = healthCheckService;
        this.metricsCollector = metricsCollector;
        this.logAnalysisService = logAnalysisService;
    }

    /**
     * Diagnose message backlog issues for a topic or namespace
     * @param resource Topic or namespace to diagnose
     * @param resourceType Resource type: 'topic' or 'namespace'
     */
    @Tool(description = "Diagnose message backlog issues for a topic or namespace. Input: 'resource|resourceType' where resourceType is 'topic' or 'namespace' (default: topic)")
    public String diagnoseBacklogIssue(@ToolParam(description = "Topic or namespace to diagnose") String resource,
                                        @ToolParam(description = "Resource type: 'topic' or 'namespace', default 'topic'", required = false) String resourceType) {
        log.info("Tool: Diagnosing backlog issue for: {}", resource);

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Get topic info if it's a topic
            if ("topic".equals(resourceType) || resource.startsWith("persistent://")) {
                try {
                    TopicInfo topic = pulsarAdminClient.getTopicInfo(resource);

                    if (topic.getBacklogSize() > 100000000) { // 100MB
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.PERFORMANCE)
                                .severity(Severity.HIGH)
                                .title("Large Message Backlog Detected")
                                .description(String.format("Topic '%s' has backlog of %d bytes (%d messages)",
                                        resource, topic.getBacklogSize(), topic.getMessageCount()))
                                .affectedResource(resource)
                                .symptoms(List.of("Growing message backlog", "Consumer lag increasing"))
                                .possibleCauses(List.of(
                                        "Consumer is not running or stuck",
                                        "Consumer processing too slow",
                                        "Message processing errors causing redelivery"))
                                .recommendations(List.of(
                                        "Check consumer application status",
                                        "Increase consumer parallelism",
                                        "Review consumer logs for errors",
                                        "Consider scaling consumers"))
                                .build());
                    }

                    // Check subscription status
                    if (topic.getSubscriptionCount() == 0) {
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.CONFIGURATION)
                                .severity(Severity.MEDIUM)
                                .title("No Subscriptions on Topic")
                                .description("Topic has no subscriptions - messages will accumulate indefinitely")
                                .affectedResource(resource)
                                .recommendations(List.of("Create a subscription or enable TTL"))
                                .build());
                    }

                } catch (Exception e) {
                    log.warn("Could not get topic info: {}", e.getMessage());
                }
            }

            return formatDiagnosticResults(results, "Backlog Diagnosis");

        } catch (Exception e) {
            return "Error diagnosing backlog: " + e.getMessage();
        }
    }

    /**
     * Diagnose connection issues in the cluster
     */
    @Tool(description = "Diagnose connection issues in the Pulsar cluster. No input required.")
    public String diagnoseConnectionIssues() {
        log.info("Tool: Diagnosing connection issues");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check cluster health
            ClusterHealth health = healthCheckService.performHealthCheck();

            for (ComponentHealth component : health.getComponents()) {
                if (component.getStatus() == HealthStatus.CRITICAL) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.CONNECTIVITY)
                            .severity(Severity.CRITICAL)
                            .title("Component Unavailable")
                            .description(String.format("%s '%s' is in critical state: %s",
                                    component.getType(), component.getId(), component.getMessage()))
                            .affectedResource(component.getId())
                            .possibleCauses(List.of(
                                    "Process crashed",
                                    "Network connectivity issue",
                                    "Resource exhaustion"))
                            .recommendations(List.of(
                                    "Check process status",
                                    "Review component logs",
                                    "Verify network connectivity",
                                    "Check system resources"))
                            .build());
                }
            }

            // Check metrics for connection issues
            try {
                var metrics = metricsCollector.getClusterMetricsSummary();
                if (metrics.getTotalConnections() == 0) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.CONNECTIVITY)
                            .severity(Severity.HIGH)
                            .title("No Active Connections")
                            .description("Cluster has zero active connections")
                            .possibleCauses(List.of(
                                    "All clients disconnected",
                                    "Broker is not accepting connections",
                                    "Network partition"))
                            .recommendations(List.of(
                                    "Check broker status",
                                    "Verify client configurations",
                                    "Check network connectivity"))
                            .build());
                }
            } catch (Exception e) {
                log.debug("Could not check connection metrics");
            }

            return formatDiagnosticResults(results, "Connection Diagnosis");

        } catch (Exception e) {
            return "Error diagnosing connections: " + e.getMessage();
        }
    }

    /**
     * Diagnose performance issues in the cluster
     */
    @Tool(description = "Diagnose performance issues in the Pulsar cluster. No input required.")
    public String diagnosePerformanceIssues() {
        log.info("Tool: Diagnosing performance issues");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check metrics
            try {
                var metrics = metricsCollector.getClusterMetricsSummary();

                // Check for high backlog
                if (metrics.getTotalBacklog() > 1000000) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.PERFORMANCE)
                            .severity(Severity.HIGH)
                            .title("High Cluster Backlog")
                            .description(String.format("Total backlog: %d messages", metrics.getTotalBacklog()))
                            .possibleCauses(List.of("Slow consumers", "Insufficient consumer capacity"))
                            .recommendations(List.of("Scale consumers", "Check consumer health"))
                            .build());
                }

                // Check for throughput imbalance
                double throughputRatio = metrics.getMessagesInRate() > 0 ?
                        metrics.getMessagesOutRate() / metrics.getMessagesInRate() : 0;
                if (throughputRatio < 0.5 && metrics.getMessagesInRate() > 100) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.PERFORMANCE)
                            .severity(Severity.MEDIUM)
                            .title("Message Throughput Imbalance")
                            .description(String.format("In rate: %.2f, Out rate: %.2f",
                                    metrics.getMessagesInRate(), metrics.getMessagesOutRate()))
                            .possibleCauses(List.of("Consumer bottleneck", "Processing delays"))
                            .recommendations(List.of("Increase consumer parallelism", "Optimize processing"))
                            .build());
                }

            } catch (Exception e) {
                log.debug("Could not check performance metrics");
            }

            // Check broker logs for performance issues
            try {
                var logResult = logAnalysisService.analyzeBrokerLogs(500);

                // Look for specific patterns
                for (var pattern : logResult.getDetectedPatterns()) {
                    if ("RESOURCE".equals(pattern.getCategory())) {
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.RESOURCE)
                                .severity(Severity.HIGH)
                                .title("Resource Issue Detected in Logs")
                                .description(pattern.getDescription() + " (" + pattern.getCount() + " occurrences)")
                                .possibleCauses(List.of("Insufficient resources", "Memory leak"))
                                .recommendations(List.of("Check system resources", "Consider scaling"))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not analyze logs for performance issues");
            }

            return formatDiagnosticResults(results, "Performance Diagnosis");

        } catch (Exception e) {
            return "Error diagnosing performance: " + e.getMessage();
        }
    }

    /**
     * Diagnose disk space issues on Pulsar components
     * @param component Component to check: 'broker', 'bookie', 'zookeeper', or 'all' (default: all)
     */
    @Tool(description = "Diagnose disk space issues on Pulsar components (broker, bookie, zookeeper). Input: component type or 'all' for comprehensive check")
    public String diagnoseDiskIssues(@ToolParam(description = "Component to check: 'broker', 'bookie', 'zookeeper', or 'all'", required = false) String component) {
        log.info("Tool: Diagnosing disk issues for: {}", component != null ? component : "all");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for comprehensive disk info
            try {
                String mcpResult = mcpClient.callToolSync("check_disk_space",
                        Map.of("component", component != null ? component : "all"));
                return formatMcpDiskResult(mcpResult);
            } catch (Exception e) {
                log.debug("MCP disk check not available, using local checks");
            }

            // Fallback: Check cluster health for disk-related issues
            ClusterHealth health = healthCheckService.performHealthCheck();

            for (ComponentHealth comp : health.getComponents()) {
                String compType = comp.getType().getCode();

                // Filter by requested component
                if (component != null && !"all".equals(component) && !compType.contains(component.toLowerCase())) {
                    continue;
                }

                // Check for disk-related issues
                if (comp.getMessage() != null && isDiskRelatedIssue(comp.getMessage())) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.RESOURCE)
                            .severity(determineDiskSeverity(comp.getMessage()))
                            .title("Disk Issue Detected on " + comp.getType().getDisplayName())
                            .description(String.format("%s '%s': %s",
                                    comp.getType().getDisplayName(), comp.getId(), comp.getMessage()))
                            .affectedResource(comp.getId())
                            .symptoms(getDiskSymptoms(comp.getMessage()))
                            .possibleCauses(List.of(
                                    "Disk space exhaustion",
                                    "Disk I/O performance degradation",
                                    "Disk quota limits reached",
                                    "Log files accumulating",
                                    "Ledger/index files growing"))
                            .recommendations(getDiskRecommendations(comp.getMessage()))
                            .build());
                }

                // Check bookie read-only status (often disk-related)
                if ("bookie".equals(compType) && comp.getStatus() == HealthStatus.WARNING) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.RESOURCE)
                            .severity(Severity.HIGH)
                            .title("Bookie May Be in Read-Only Mode")
                            .description(String.format("Bookie '%s' is in warning state, possibly read-only due to disk threshold", comp.getId()))
                            .affectedResource(comp.getId())
                            .symptoms(List.of("Write operations failing", "Bookie marked read-only"))
                            .possibleCauses(List.of(
                                    "Disk usage exceeded threshold (default 90%)",
                                    "Disk I/O errors detected",
                                    "Insufficient disk space for ledger creation"))
                            .recommendations(List.of(
                                    "Check disk usage: df -h",
                                    "Free up disk space or expand storage",
                                    "Review bookie diskUsageThreshold setting",
                                    "Consider compacting ledgers",
                                    "Clean up old ledger files"))
                            .build());
                }
            }

            // Check logs for disk errors
            try {
                var logResult = logAnalysisService.analyzeBrokerLogs(500);
                for (var pattern : logResult.getDetectedPatterns()) {
                    if (isDiskRelatedLog(pattern.getDescription())) {
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.RESOURCE)
                                .severity(Severity.HIGH)
                                .title("Disk Error in Broker Logs")
                                .description(String.format("Pattern found %d times: %s",
                                        pattern.getCount(), pattern.getDescription()))
                                .possibleCauses(List.of("Disk I/O failure", "Disk full", "File system errors"))
                                .recommendations(List.of(
                                        "Check disk health: dmesg | grep -i error",
                                        "Run disk diagnostics: smartctl",
                                        "Review file system status: df -h",
                                        "Consider disk replacement if hardware issue"))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not analyze logs for disk issues");
            }

            return formatDiagnosticResults(results, "Disk Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing disk issues", e);
            return "Error diagnosing disk issues: " + e.getMessage();
        }
    }

    /**
     * Check if a message is related to disk issues
     */
    private boolean isDiskRelatedIssue(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("disk") ||
               lower.contains("no space") ||
               lower.contains("disk full") ||
               lower.contains("read-only") ||
               lower.contains("storage") ||
               lower.contains("quota") ||
               lower.contains("i/o error") ||
               lower.contains("enospc");
    }

    /**
     * Determine severity based on disk issue message
     */
    private Severity determineDiskSeverity(String message) {
        if (message == null) return Severity.MEDIUM;
        String lower = message.toLowerCase();
        if (lower.contains("critical") || lower.contains("full") || lower.contains("no space")) {
            return Severity.CRITICAL;
        }
        if (lower.contains("read-only") || lower.contains("warning")) {
            return Severity.HIGH;
        }
        return Severity.MEDIUM;
    }

    /**
     * Get disk-related symptoms
     */
    private List<String> getDiskSymptoms(String message) {
        List<String> symptoms = new ArrayList<>();
        symptoms.add("Disk space alert triggered");
        if (message != null && message.toLowerCase().contains("read-only")) {
            symptoms.add("Component in read-only mode");
        }
        symptoms.add("Write operations may fail");
        return symptoms;
    }

    /**
     * Get disk-related recommendations
     */
    private List<String> getDiskRecommendations(String message) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Check disk usage: df -h");
        recommendations.add("Clean up old logs and ledger files");
        recommendations.add("Review retention policies");
        recommendations.add("Expand storage if needed");
        if (message != null && message.toLowerCase().contains("bookie")) {
            recommendations.add("Check bookie diskUsageThreshold setting");
            recommendations.add("Run bookie recovery if needed");
        }
        return recommendations;
    }

    /**
     * Check if log pattern is disk-related
     */
    private boolean isDiskRelatedLog(String pattern) {
        if (pattern == null) return false;
        String lower = pattern.toLowerCase();
        return lower.contains("disk") ||
               lower.contains("enospc") ||
               lower.contains("no space") ||
               lower.contains("i/o error") ||
               lower.contains("read only") ||
               lower.contains("filesystem");
    }

    /**
     * Format MCP disk result
     */
    private String formatMcpDiskResult(String mcpResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Disk Space Diagnosis ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n---\n");
        sb.append("**Recommendations:**\n");
        sb.append("- Monitor disk usage regularly: df -h\n");
        sb.append("- Set up disk usage alerts (e.g., at 80% threshold)\n");
        sb.append("- Review log retention policies\n");
        sb.append("- Consider log rotation and cleanup scripts\n");
        sb.append("- For BookKeeper: check diskUsageThreshold and diskUsageWarnThreshold\n");
        return sb.toString();
    }

    /**
     * Diagnose authentication/authorization issues
     */
    @Tool(description = "Diagnose authentication and authorization issues in Pulsar. Input: optional topic or namespace for permission check")
    public String diagnoseAuthIssues(@ToolParam(description = "Topic or namespace to check permissions", required = false) String resource) {
        log.info("Tool: Diagnosing auth issues for: {}", resource != null ? resource : "cluster");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for auth config
            try {
                String mcpResult = mcpClient.callToolSync("check_auth_config",
                        Map.of("resource", resource != null ? resource : ""));
                return formatAuthResult(mcpResult);
            } catch (Exception e) {
                log.debug("MCP auth check not available, using local checks");
            }

            // Fallback: Check cluster health for auth-related issues
            ClusterHealth health = healthCheckService.performHealthCheck();

            for (ComponentHealth component : health.getComponents()) {
                if (component.getMessage() != null && isAuthRelatedIssue(component.getMessage())) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.SECURITY)
                            .severity(Severity.HIGH)
                            .title("Authentication/Authorization Issue Detected")
                            .description(String.format("%s '%s': %s",
                                    component.getType().getDisplayName(), component.getId(), component.getMessage()))
                            .affectedResource(component.getId())
                            .symptoms(List.of("Connection rejected", "Permission denied", "Authentication failed"))
                            .possibleCauses(List.of(
                                    "Authentication not enabled but required",
                                    "Invalid or expired token",
                                    "Incorrect permission configuration",
                                    "TLS certificate issues"))
                            .recommendations(List.of(
                                    "Check authentication settings in broker.conf",
                                    "Verify token validity and expiration",
                                    "Review namespace/tenant permissions",
                                    "Check TLS configuration if using SSL"))
                            .build());
                }
            }

            return formatDiagnosticResults(results, "Auth Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing auth issues", e);
            return "Error diagnosing auth issues: " + e.getMessage();
        }
    }

    /**
     * Diagnose slow production issues
     */
    @Tool(description = "Diagnose slow message production issues. Input: optional topic name to check")
    public String diagnoseProduceSlow(@ToolParam(description = "Topic to diagnose", required = false) String topic) {
        log.info("Tool: Diagnosing slow production for: {}", topic != null ? topic : "cluster");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for producer stats
            try {
                String mcpResult = mcpClient.callToolSync("get_producer_stats",
                        Map.of("topic", topic != null ? topic : ""));
                return formatProducerStatsResult(mcpResult, "slow");
            } catch (Exception e) {
                log.debug("MCP producer stats not available, using local checks");
            }

            // Check broker metrics for production issues
            try {
                var metrics = metricsCollector.getClusterMetricsSummary();

                // Check for high latency
                if (metrics.getMessagesInRate() > 0) {
                    // Check broker load
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.PERFORMANCE)
                            .severity(Severity.MEDIUM)
                            .title("Production Performance Analysis")
                            .description(String.format("Message in rate: %.2f msg/s", metrics.getMessagesInRate()))
                            .possibleCauses(analyzeSlowProductionCauses(metrics))
                            .recommendations(List.of(
                                    "Check producer batch size configuration",
                                    "Review network latency between producer and broker",
                                    "Check broker CPU/memory usage",
                                    "Verify BookKeeper write performance"))
                            .build());
                }

            } catch (Exception e) {
                log.debug("Could not check production metrics");
            }

            // Check logs for production errors
            try {
                var logResult = logAnalysisService.analyzeBrokerLogs(500);
                for (var pattern : logResult.getDetectedPatterns()) {
                    if ("PERFORMANCE".equals(pattern.getCategory())) {
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.PERFORMANCE)
                                .severity(Severity.MEDIUM)
                                .title("Production Performance Issue in Logs")
                                .description(pattern.getDescription() + " (" + pattern.getCount() + " occurrences)")
                                .possibleCauses(List.of("Broker overload", "Network issues", "Disk I/O bottleneck"))
                                .recommendations(List.of("Scale brokers", "Check network", "Optimize disk I/O"))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not analyze logs for production issues");
            }

            return formatDiagnosticResults(results, "Slow Production Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing slow production", e);
            return "Error diagnosing slow production: " + e.getMessage();
        }
    }

    /**
     * Diagnose production failure issues
     */
    @Tool(description = "Diagnose message production failure issues. Input: optional topic name to check")
    public String diagnoseProduceFailed(@ToolParam(description = "Topic to diagnose", required = false) String topic) {
        log.info("Tool: Diagnosing production failures for: {}", topic != null ? topic : "cluster");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for topic info
            if (topic != null) {
                try {
                    String mcpResult = mcpClient.callToolSync("get_topic_info",
                            Map.of("topic", topic));
                    return formatTopicErrorResult(mcpResult);
                } catch (Exception e) {
                    log.debug("MCP topic info not available");
                }
            }

            // Check cluster health
            ClusterHealth health = healthCheckService.performHealthCheck();

            for (ComponentHealth component : health.getComponents()) {
                if (component.getStatus() == HealthStatus.CRITICAL) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.CONNECTIVITY)
                            .severity(Severity.CRITICAL)
                            .title("Component Failure Affecting Production")
                            .description(String.format("%s '%s' is in critical state: %s",
                                    component.getType().getDisplayName(), component.getId(), component.getMessage()))
                            .affectedResource(component.getId())
                            .possibleCauses(List.of(
                                    "Broker crashed",
                                    "Bookie unavailable",
                                    "Disk full",
                                    "Permission denied"))
                            .recommendations(List.of(
                                    "Check component status and logs",
                                    "Verify disk space",
                                    "Check permissions",
                                    "Restart failed component"))
                            .build());
                }
            }

            // Check logs for production errors
            try {
                var logResult = logAnalysisService.analyzeBrokerLogs(500);
                for (var pattern : logResult.getDetectedPatterns()) {
                    if ("TOPIC".equals(pattern.getCategory()) || "STORAGE".equals(pattern.getCategory())) {
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.CONFIGURATION)
                                .severity(Severity.HIGH)
                                .title("Production Error in Logs")
                                .description(pattern.getDescription() + " (" + pattern.getCount() + " occurrences)")
                                .possibleCauses(List.of("Topic not found", "Permission issue", "Storage failure"))
                                .recommendations(List.of(
                                        "Verify topic exists",
                                        "Check permissions",
                                        "Review BookKeeper status"))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not analyze logs for production failures");
            }

            return formatDiagnosticResults(results, "Production Failure Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing production failures", e);
            return "Error diagnosing production failures: " + e.getMessage();
        }
    }

    /**
     * Diagnose slow consumption issues
     */
    @Tool(description = "Diagnose slow message consumption issues. Input: optional topic or subscription name")
    public String diagnoseConsumeSlow(@ToolParam(description = "Topic or subscription to diagnose", required = false) String resource) {
        log.info("Tool: Diagnosing slow consumption for: {}", resource != null ? resource : "cluster");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for consumer stats
            try {
                String mcpResult = mcpClient.callToolSync("get_consumer_stats",
                        Map.of("resource", resource != null ? resource : ""));
                return formatConsumerStatsResult(mcpResult, "slow");
            } catch (Exception e) {
                log.debug("MCP consumer stats not available, using local checks");
            }

            // Check cluster metrics for consumption issues
            try {
                var metrics = metricsCollector.getClusterMetricsSummary();

                // Check backlog
                if (metrics.getTotalBacklog() > 100000) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.PERFORMANCE)
                            .severity(Severity.HIGH)
                            .title("High Message Backlog")
                            .description(String.format("Total backlog: %d messages. In rate: %.2f, Out rate: %.2f",
                                    metrics.getTotalBacklog(), metrics.getMessagesInRate(), metrics.getMessagesOutRate()))
                            .possibleCauses(analyzeSlowConsumeCauses(metrics))
                            .recommendations(List.of(
                                    "Check consumer application status",
                                    "Scale consumer instances",
                                    "Optimize consumer processing logic",
                                    "Review consumer configuration (prefetch, batch size)"))
                            .build());
                }

                // Check throughput imbalance
                double ratio = metrics.getMessagesInRate() > 0 ?
                        metrics.getMessagesOutRate() / metrics.getMessagesInRate() : 0;
                if (ratio < 0.8 && metrics.getMessagesInRate() > 100) {
                    results.add(DiagnosticResult.builder()
                            .type(DiagnosticType.PERFORMANCE)
                            .severity(Severity.MEDIUM)
                            .title("Consumer Throughput Imbalance")
                            .description(String.format("Consumption rate (%.2f) lower than production rate (%.2f)",
                                    metrics.getMessagesOutRate(), metrics.getMessagesInRate()))
                            .possibleCauses(List.of("Consumer bottleneck", "Processing too slow", "Traffic spike"))
                            .recommendations(List.of(
                                    "Add more consumers",
                                    "Optimize message processing",
                                    "Check for consumer errors"))
                            .build());
                }

            } catch (Exception e) {
                log.debug("Could not check consumption metrics");
            }

            return formatDiagnosticResults(results, "Slow Consumption Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing slow consumption", e);
            return "Error diagnosing slow consumption: " + e.getMessage();
        }
    }

    /**
     * Diagnose consumption failure issues
     */
    @Tool(description = "Diagnose message consumption failure issues. Input: optional topic or subscription name")
    public String diagnoseConsumeFailed(@ToolParam(description = "Topic or subscription to diagnose", required = false) String resource) {
        log.info("Tool: Diagnosing consumption failures for: {}", resource != null ? resource : "cluster");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for subscription stats
            try {
                String mcpResult = mcpClient.callToolSync("check_subscription",
                        Map.of("resource", resource != null ? resource : ""));
                return formatSubscriptionErrorResult(mcpResult);
            } catch (Exception e) {
                log.debug("MCP subscription check not available");
            }

            // Check logs for consumption errors
            try {
                var logResult = logAnalysisService.analyzeBrokerLogs(500);
                for (var pattern : logResult.getDetectedPatterns()) {
                    if ("TOPIC".equals(pattern.getCategory())) {
                        results.add(DiagnosticResult.builder()
                                .type(DiagnosticType.CONFIGURATION)
                                .severity(Severity.HIGH)
                                .title("Consumption Error in Logs")
                                .description(pattern.getDescription() + " (" + pattern.getCount() + " occurrences)")
                                .possibleCauses(List.of(
                                        "Subscription not found",
                                        "Consumer connection issues",
                                        "Schema mismatch",
                                        "DLQ configuration issue"))
                                .recommendations(List.of(
                                        "Verify subscription exists",
                                        "Check consumer logs",
                                        "Review schema configuration",
                                        "Check DLQ settings"))
                                .build());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not analyze logs for consumption failures");
            }

            return formatDiagnosticResults(results, "Consumption Failure Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing consumption failures", e);
            return "Error diagnosing consumption failures: " + e.getMessage();
        }
    }

    /**
     * Diagnose duplicate consumption issues
     */
    @Tool(description = "Diagnose duplicate message consumption issues. Input: optional topic or subscription name")
    public String diagnoseConsumeDuplicate(@ToolParam(description = "Topic or subscription to diagnose", required = false) String resource) {
        log.info("Tool: Diagnosing duplicate consumption for: {}", resource != null ? resource : "cluster");

        try {
            List<DiagnosticResult> results = new ArrayList<>();

            // Check via MCP for subscription stats
            try {
                String mcpResult = mcpClient.callToolSync("get_subscription_stats",
                        Map.of("resource", resource != null ? resource : ""));
                return formatDuplicateResult(mcpResult);
            } catch (Exception e) {
                log.debug("MCP subscription stats not available");
            }

            // Analyze potential duplicate causes
            results.add(DiagnosticResult.builder()
                    .type(DiagnosticType.CONFIGURATION)
                    .severity(Severity.MEDIUM)
                    .title("Potential Duplicate Consumption Causes")
                    .description("Messages may be consumed multiple times due to various factors")
                    .possibleCauses(List.of(
                            "Consumer reconnection without proper ack",
                            "Ack timeout causing redelivery",
                            "Consumer crash before ack",
                            "Network instability",
                            "Batch ack issues"))
                    .recommendations(List.of(
                            "Check ackTimeoutMillis configuration (default 30000ms)",
                            "Enable idempotent processing in consumer application",
                            "Review consumer reconnection logic",
                            "Check for network stability issues",
                            "Consider using Key_Shared subscription for ordering"))
                    .build());

            return formatDiagnosticResults(results, "Duplicate Consumption Diagnosis");

        } catch (Exception e) {
            log.error("Error diagnosing duplicate consumption", e);
            return "Error diagnosing duplicate consumption: " + e.getMessage();
        }
    }

    // Helper methods

    private boolean isAuthRelatedIssue(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("auth") ||
               lower.contains("permission") ||
               lower.contains("denied") ||
               lower.contains("unauthorized") ||
               lower.contains("forbidden") ||
               lower.contains("token") ||
               lower.contains("credential");
    }

    private List<String> analyzeSlowProductionCauses(Object metrics) {
        return List.of(
                "Traffic spike exceeding broker capacity",
                "Broker CPU/memory overload",
                "BookKeeper write latency",
                "Network latency between producer and broker",
                "Batch size too small",
                "Compression overhead");
    }

    private List<String> analyzeSlowConsumeCauses(Object metrics) {
        return List.of(
                "Consumer processing too slow",
                "Traffic spike - consumers can't keep up",
                "Too few consumers for the load",
                "Consumer application errors causing redelivery",
                "Prefetch configuration not optimal",
                "Network latency");
    }

    private String formatAuthResult(String mcpResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Authentication/Authorization Diagnosis ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n**Common Solutions:**\n");
        sb.append("- Verify authentication is properly configured\n");
        sb.append("- Check token validity: pulsar-admin tokens validate <token>\n");
        sb.append("- Review permissions: pulsar-admin namespaces grant-permission\n");
        return sb.toString();
    }

    private String formatProducerStatsResult(String mcpResult, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Production Performance Diagnosis ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n**Optimization Tips:**\n");
        sb.append("- Increase batch size for better throughput\n");
        sb.append("- Use compression for large messages\n");
        sb.append("- Consider async sending for high throughput\n");
        sb.append("- Check network latency to broker\n");
        return sb.toString();
    }

    private String formatTopicErrorResult(String mcpResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Topic Status Check ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n**Troubleshooting:**\n");
        sb.append("- Verify topic exists: pulsar-admin topics list\n");
        sb.append("- Check permissions: pulsar-admin topics permissions\n");
        sb.append("- Review broker logs for errors\n");
        return sb.toString();
    }

    private String formatConsumerStatsResult(String mcpResult, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Consumer Performance Diagnosis ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n**Optimization Tips:**\n");
        sb.append("- Scale consumer instances\n");
        sb.append("- Increase prefetch count\n");
        sb.append("- Optimize message processing\n");
        sb.append("- Check for consumer errors in logs\n");
        return sb.toString();
    }

    private String formatSubscriptionErrorResult(String mcpResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Subscription Status Check ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n**Troubleshooting:**\n");
        sb.append("- Verify subscription exists\n");
        sb.append("- Check consumer logs for errors\n");
        sb.append("- Review DLQ configuration\n");
        sb.append("- Check schema compatibility\n");
        return sb.toString();
    }

    private String formatDuplicateResult(String mcpResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Duplicate Consumption Analysis ===\n\n");
        sb.append(mcpResult);
        sb.append("\n\n**Prevention Strategies:**\n");
        sb.append("- Implement idempotent processing\n");
        sb.append("- Configure appropriate ackTimeoutMillis\n");
        sb.append("- Use negative ack with care\n");
        sb.append("- Monitor consumer reconnections\n");
        return sb.toString();
    }

    /**
     * Run comprehensive diagnostic on the entire cluster
     */
    @Tool(description = "Run a comprehensive diagnostic on the entire cluster. No input required.")
    public String runComprehensiveDiagnostic() {
        log.info("Tool: Running comprehensive diagnostic via MCP");

        try {
            // Use MCP diagnose_problem tool for comprehensive diagnosis
            return mcpClient.callToolSync("diagnose_problem",
                    Map.of("problem_type", "unknown"));
        } catch (Exception e) {
            log.error("Failed to run comprehensive diagnostic via MCP, falling back", e);

            StringBuilder sb = new StringBuilder();
            sb.append("=== Comprehensive Cluster Diagnostic ===\n\n");

            // Run all diagnostics
            sb.append(diagnoseConnectionIssues()).append("\n");
            sb.append(diagnosePerformanceIssues()).append("\n");

            // Add cluster health summary
            try {
                ClusterHealth health = healthCheckService.performHealthCheck();
                sb.append("\n=== Cluster Health Summary ===\n");
                sb.append(String.format("Overall Status: %s\n", health.getStatus()));
                sb.append(String.format("Components Checked: %d\n", health.getComponents().size()));
                sb.append(String.format("Healthy: %d, Issues: %d\n",
                        health.getHealthyCount(), health.getUnhealthyCount()));
            } catch (Exception ex) {
                sb.append("Could not get health summary: ").append(ex.getMessage()).append("\n");
            }

            return sb.toString();
        }
    }

    private String formatDiagnosticResults(List<DiagnosticResult> results, String title) {
        if (results.isEmpty()) {
            return String.format("=== %s ===\n\nNo issues detected.", title);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %s ===\n\n", title));
        sb.append(String.format("Issues Found: %d\n\n", results.size()));

        int index = 1;
        for (DiagnosticResult result : results) {
            sb.append(String.format("%d. [%s - %s] %s\n",
                    index++,
                    result.getType().getDescription(),
                    result.getSeverity().getCode().toUpperCase(),
                    result.getTitle()));

            sb.append(String.format("   Description: %s\n", result.getDescription()));

            if (result.getPossibleCauses() != null && !result.getPossibleCauses().isEmpty()) {
                sb.append("   Possible Causes:\n");
                for (String cause : result.getPossibleCauses()) {
                    sb.append("   - ").append(cause).append("\n");
                }
            }

            if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
                sb.append("   Recommendations:\n");
                for (String rec : result.getRecommendations()) {
                    sb.append("   → ").append(rec).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}