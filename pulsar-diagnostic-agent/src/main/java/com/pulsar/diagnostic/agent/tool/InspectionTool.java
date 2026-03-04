package com.pulsar.diagnostic.agent.tool;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.common.enums.HealthStatus;
import com.pulsar.diagnostic.common.model.InspectionReport;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for performing cluster inspections.
 * Uses MCP server for cluster inspection.
 */
@Component
public class InspectionTool {

    private static final Logger log = LoggerFactory.getLogger(InspectionTool.class);

    private final McpClient mcpClient;
    private final PulsarAdminClient pulsarAdminClient;
    private final HealthCheckService healthCheckService;
    private final PrometheusMetricsCollector metricsCollector;
    private final LogAnalysisService logAnalysisService;

    public InspectionTool(McpClient mcpClient,
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
     * Perform a comprehensive cluster inspection covering all components
     */
    @Tool(description = "Perform a full cluster inspection. No input required.")
    public String performFullInspection() {
        log.info("Tool: Performing full cluster inspection via MCP");
        try {
            // Use MCP diagnose_problem for comprehensive inspection
            return mcpClient.callToolSync("diagnose_problem",
                    Map.of("problem_type", "performance"));
        } catch (Exception e) {
            log.error("Failed to perform full inspection via MCP, falling back", e);
            return performInspection(null);
        }
    }

    /**
     * Perform cluster inspection focused on specific areas
     * @param focusArea Focus areas: 'brokers', 'bookies', 'topics', 'performance', 'logs', or null for full inspection
     */
    @Tool(description = "Perform a focused inspection on a specific area. Input: focus area (e.g., 'brokers', 'topics', 'performance')")
    public String performInspection(@ToolParam(description = "Focus area: 'brokers', 'bookies', 'topics', 'performance', 'logs', or null for full inspection", required = false) String focusArea) {
        log.info("Tool: Performing inspection with focus: {}", focusArea);

        List<InspectionReport.InspectionItem> items = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now();

        // Broker inspection
        if (focusArea == null || "brokers".equals(focusArea)) {
            items.addAll(inspectBrokers());
        }

        // Bookie inspection
        if (focusArea == null || "bookies".equals(focusArea)) {
            items.addAll(inspectBookies());
        }

        // Topic inspection
        if (focusArea == null || "topics".equals(focusArea)) {
            items.addAll(inspectTopics());
        }

        // Performance inspection
        if (focusArea == null || "performance".equals(focusArea)) {
            items.addAll(inspectPerformance());
        }

        // Log inspection
        if (focusArea == null || "logs".equals(focusArea)) {
            items.addAll(inspectLogs());
        }

        // Build report
        InspectionReport report = buildReport(items, startTime);

        return formatInspectionReport(report);
    }

    /**
     * Quick health snapshot of the cluster
     */
    @Tool(description = "Get a quick health snapshot of the cluster. No input required.")
    public String quickHealthSnapshot() {
        log.info("Tool: Quick health snapshot via MCP");
        try {
            return mcpClient.callToolSync("inspect_cluster",
                    Map.of("components", List.of("all")));
        } catch (Exception e) {
            log.error("Failed to get health snapshot via MCP, falling back", e);
            try {
                ClusterHealth health = healthCheckService.performHealthCheck();

                StringBuilder sb = new StringBuilder();
                sb.append("=== Quick Health Snapshot ===\n");
                sb.append(String.format("Time: %s\n\n", health.getCheckTime()));
                sb.append(String.format("Overall Status: %s\n", health.getStatus()));
                sb.append(String.format("Summary: %s\n\n", health.getSummary()));

                // Count by status
                long healthy = health.getComponents().stream()
                        .filter(c -> c.getStatus() == HealthStatus.HEALTHY).count();
                long warning = health.getComponents().stream()
                        .filter(c -> c.getStatus() == HealthStatus.WARNING).count();
                long critical = health.getComponents().stream()
                        .filter(c -> c.getStatus() == HealthStatus.CRITICAL).count();

                sb.append("Status Breakdown:\n");
                sb.append(String.format("  ✅ Healthy: %d\n", healthy));
                sb.append(String.format("  ⚠️ Warning: %d\n", warning));
                sb.append(String.format("  ❌ Critical: %d\n", critical));

                // Quick metrics
                try {
                    var metrics = metricsCollector.getClusterMetricsSummary();
                    sb.append("\nKey Metrics:\n");
                    sb.append(String.format("  Messages In: %.2f/s\n", metrics.getMessagesInRate()));
                    sb.append(String.format("  Messages Out: %.2f/s\n", metrics.getMessagesOutRate()));
                    sb.append(String.format("  Total Backlog: %d\n", metrics.getTotalBacklog()));
                    sb.append(String.format("  Connections: %d\n", metrics.getTotalConnections()));
                } catch (Exception ex) {
                    sb.append("\nMetrics: Not available\n");
                }

                return sb.toString();

            } catch (Exception ex) {
                return "Error getting health snapshot: " + ex.getMessage();
            }
        }
    }

    private List<InspectionReport.InspectionItem> inspectBrokers() {
        List<InspectionReport.InspectionItem> items = new ArrayList<>();

        try {
            var brokers = pulsarAdminClient.getBrokers();

            // Check broker count
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Brokers")
                    .name("Broker Count")
                    .description("Number of active brokers")
                    .status(brokers.isEmpty() ? HealthStatus.CRITICAL :
                            brokers.size() < 2 ? HealthStatus.WARNING : HealthStatus.HEALTHY)
                    .message(String.format("%d active broker(s)", brokers.size()))
                    .remediation(brokers.isEmpty() ? "Start broker processes" :
                            brokers.size() < 2 ? "Consider adding more brokers for HA" : null)
                    .build());

            // Check individual broker health
            for (var broker : brokers) {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Brokers")
                        .name("Broker: " + broker.getBrokerId())
                        .description("Individual broker health")
                        .status(broker.getHealthStatus())
                        .message(broker.getHealthStatus().getDescription())
                        .build());
            }

        } catch (Exception e) {
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Brokers")
                    .name("Broker Inspection")
                    .status(HealthStatus.UNKNOWN)
                    .message("Failed to inspect brokers: " + e.getMessage())
                    .build());
        }

        return items;
    }

    private List<InspectionReport.InspectionItem> inspectBookies() {
        List<InspectionReport.InspectionItem> items = new ArrayList<>();

        try {
            var bookies = pulsarAdminClient.getBookies();

            // Check bookie count
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Bookies")
                    .name("Bookie Count")
                    .description("Number of available bookies")
                    .status(bookies.isEmpty() ? HealthStatus.CRITICAL :
                            bookies.size() < 3 ? HealthStatus.WARNING : HealthStatus.HEALTHY)
                    .message(String.format("%d bookie(s) available", bookies.size()))
                    .remediation(bookies.isEmpty() ? "Start bookie processes" :
                            bookies.size() < 3 ? "Need at least 3 bookies for quorum" : null)
                    .build());

            // Check for read-only bookies
            long readOnlyCount = bookies.stream()
                    .filter(b -> b.isReadOnly())
                    .count();

            if (readOnlyCount > 0) {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Bookies")
                        .name("Read-Only Bookies")
                        .description("Bookies in read-only mode")
                        .status(HealthStatus.WARNING)
                        .message(String.format("%d bookie(s) in read-only mode", readOnlyCount))
                        .remediation("Check disk space and bookie configuration")
                        .build());
            }

        } catch (Exception e) {
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Bookies")
                    .name("Bookie Inspection")
                    .status(HealthStatus.UNKNOWN)
                    .message("Failed to inspect bookies: " + e.getMessage())
                    .build());
        }

        return items;
    }

    private List<InspectionReport.InspectionItem> inspectTopics() {
        List<InspectionReport.InspectionItem> items = new ArrayList<>();

        try {
            var namespaces = pulsarAdminClient.getNamespaces();

            int totalTopics = 0;
            long totalBacklog = 0;

            for (String ns : namespaces) {
                try {
                    var topics = pulsarAdminClient.getTopics(ns);
                    totalTopics += topics.size();
                } catch (Exception e) {
                    // Skip namespace if can't get topics
                }
            }

            items.add(InspectionReport.InspectionItem.builder()
                    .category("Topics")
                    .name("Topic Count")
                    .description("Total number of topics")
                    .status(HealthStatus.HEALTHY)
                    .message(String.format("%d topics across %d namespaces", totalTopics, namespaces.size()))
                    .build());

            // Check backlog
            try {
                var metrics = metricsCollector.getClusterMetricsSummary();
                if (metrics.getTotalBacklog() > 1000000) {
                    items.add(InspectionReport.InspectionItem.builder()
                            .category("Topics")
                            .name("Message Backlog")
                            .description("Total unconsumed messages")
                            .status(HealthStatus.WARNING)
                            .message(String.format("%d messages in backlog", metrics.getTotalBacklog()))
                            .remediation("Check consumer health and scale if needed")
                            .build());
                }
            } catch (Exception e) {
                // Skip backlog check
            }

        } catch (Exception e) {
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Topics")
                    .name("Topic Inspection")
                    .status(HealthStatus.UNKNOWN)
                    .message("Failed to inspect topics: " + e.getMessage())
                    .build());
        }

        return items;
    }

    private List<InspectionReport.InspectionItem> inspectPerformance() {
        List<InspectionReport.InspectionItem> items = new ArrayList<>();

        try {
            var metrics = metricsCollector.getClusterMetricsSummary();

            // Check throughput
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Performance")
                    .name("Message Throughput")
                    .description("Messages in/out rate")
                    .status(HealthStatus.HEALTHY)
                    .message(String.format("In: %.2f msg/s, Out: %.2f msg/s",
                            metrics.getMessagesInRate(), metrics.getMessagesOutRate()))
                    .build());

            // Check connection count
            if (metrics.getTotalConnections() == 0) {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Performance")
                        .name("Connections")
                        .description("Active connections")
                        .status(HealthStatus.WARNING)
                        .message("No active connections")
                        .remediation("Check client applications")
                        .build());
            } else {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Performance")
                        .name("Connections")
                        .description("Active connections")
                        .status(HealthStatus.HEALTHY)
                        .message(String.format("%d active connections", metrics.getTotalConnections()))
                        .build());
            }

        } catch (Exception e) {
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Performance")
                    .name("Performance Inspection")
                    .status(HealthStatus.UNKNOWN)
                    .message("Metrics not available: " + e.getMessage())
                    .build());
        }

        return items;
    }

    private List<InspectionReport.InspectionItem> inspectLogs() {
        List<InspectionReport.InspectionItem> items = new ArrayList<>();

        try {
            var brokerLogs = logAnalysisService.analyzeBrokerLogs(200);
            var bookieLogs = logAnalysisService.analyzeBookieLogs(200);

            // Check for errors
            if (brokerLogs.getErrorCount() > 0) {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Logs")
                        .name("Broker Errors")
                        .description("Recent error logs from brokers")
                        .status(brokerLogs.getErrorCount() > 10 ? HealthStatus.WARNING : HealthStatus.HEALTHY)
                        .message(String.format("%d errors found in broker logs", brokerLogs.getErrorCount()))
                        .remediation("Review broker logs for details")
                        .build());
            }

            if (bookieLogs.getErrorCount() > 0) {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Logs")
                        .name("Bookie Errors")
                        .description("Recent error logs from bookies")
                        .status(bookieLogs.getErrorCount() > 10 ? HealthStatus.WARNING : HealthStatus.HEALTHY)
                        .message(String.format("%d errors found in bookie logs", bookieLogs.getErrorCount()))
                        .remediation("Review bookie logs for details")
                        .build());
            }

            if (brokerLogs.getErrorCount() == 0 && bookieLogs.getErrorCount() == 0) {
                items.add(InspectionReport.InspectionItem.builder()
                        .category("Logs")
                        .name("Log Analysis")
                        .description("Error log check")
                        .status(HealthStatus.HEALTHY)
                        .message("No errors found in recent logs")
                        .build());
            }

        } catch (Exception e) {
            items.add(InspectionReport.InspectionItem.builder()
                    .category("Logs")
                    .name("Log Inspection")
                    .status(HealthStatus.UNKNOWN)
                    .message("Could not analyze logs: " + e.getMessage())
                    .build());
        }

        return items;
    }

    private InspectionReport buildReport(List<InspectionReport.InspectionItem> items, LocalDateTime startTime) {
        int passed = 0, warning = 0, failed = 0;

        for (InspectionReport.InspectionItem item : items) {
            if (item.getStatus() == HealthStatus.HEALTHY) {
                passed++;
            } else if (item.getStatus() == HealthStatus.WARNING) {
                warning++;
            } else if (item.getStatus() == HealthStatus.CRITICAL || item.getStatus() == HealthStatus.UNKNOWN) {
                failed++;
            }
        }

        HealthStatus overallStatus;
        if (failed > 0) {
            overallStatus = HealthStatus.CRITICAL;
        } else if (warning > 0) {
            overallStatus = HealthStatus.WARNING;
        } else {
            overallStatus = HealthStatus.HEALTHY;
        }

        List<String> recommendations = new ArrayList<>();
        for (InspectionReport.InspectionItem item : items) {
            if (item.getRemediation() != null) {
                recommendations.add(item.getRemediation());
            }
        }

        return InspectionReport.builder()
                .reportId(UUID.randomUUID().toString())
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .overallStatus(overallStatus)
                .totalChecks(items.size())
                .passedChecks(passed)
                .warningChecks(warning)
                .failedChecks(failed)
                .items(items)
                .recommendations(recommendations)
                .build();
    }

    private String formatInspectionReport(InspectionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║              CLUSTER INSPECTION REPORT                   ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n\n");

        sb.append(String.format("Report ID: %s\n", report.getReportId()));
        sb.append(String.format("Start Time: %s\n", report.getStartTime()));
        sb.append(String.format("End Time: %s\n", report.getEndTime()));
        sb.append(String.format("Duration: %s\n\n",
                java.time.Duration.between(report.getStartTime(), report.getEndTime()).toMillis() + "ms"));

        sb.append("=== Summary ===\n");
        sb.append(String.format("Overall Status: %s\n", report.getOverallStatus()));
        sb.append(String.format("Total Checks: %d\n", report.getTotalChecks()));
        sb.append(String.format("  ✅ Passed: %d\n", report.getPassedChecks()));
        sb.append(String.format("  ⚠️ Warnings: %d\n", report.getWarningChecks()));
        sb.append(String.format("  ❌ Failed: %d\n\n", report.getFailedChecks()));

        sb.append("=== Detailed Results ===\n\n");

        String currentCategory = null;
        for (InspectionReport.InspectionItem item : report.getItems()) {
            if (!item.getCategory().equals(currentCategory)) {
                currentCategory = item.getCategory();
                sb.append(String.format("[%s]\n", currentCategory));
            }

            String statusIcon = switch (item.getStatus()) {
                case HEALTHY -> "✅";
                case WARNING -> "⚠️";
                case CRITICAL -> "❌";
                default -> "❓";
            };

            sb.append(String.format("  %s %s: %s\n", statusIcon, item.getName(), item.getMessage()));
            if (item.getRemediation() != null) {
                sb.append(String.format("      → Remediation: %s\n", item.getRemediation()));
            }
        }

        if (!report.getRecommendations().isEmpty()) {
            sb.append("\n=== Recommendations ===\n");
            int i = 1;
            for (String rec : report.getRecommendations()) {
                sb.append(String.format("%d. %s\n", i++, rec));
            }
        }

        return sb.toString();
    }
}