package com.pulsar.diagnostic.agent.tool;

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

/**
 * Tool for diagnosing Pulsar issues
 */
@Component
public class DiagnosticTool {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticTool.class);

    private final PulsarAdminClient pulsarAdminClient;
    private final HealthCheckService healthCheckService;
    private final PrometheusMetricsCollector metricsCollector;
    private final LogAnalysisService logAnalysisService;

    public DiagnosticTool(PulsarAdminClient pulsarAdminClient,
                          HealthCheckService healthCheckService,
                          PrometheusMetricsCollector metricsCollector,
                          LogAnalysisService logAnalysisService) {
        this.pulsarAdminClient = pulsarAdminClient;
        this.healthCheckService = healthCheckService;
        this.metricsCollector = metricsCollector;
        this.logAnalysisService = logAnalysisService;
    }

    @Tool(description = "Diagnose message backlog issues for a topic or namespace")
    public String diagnoseBacklogIssue(
            @ToolParam(description = "Topic or namespace to diagnose") String resource,
            @ToolParam(description = "Resource type: 'topic' or 'namespace'", required = false)
            String resourceType) {
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
                                .symptom(List.of("Growing message backlog", "Consumer lag increasing"))
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

    @Tool(description = "Diagnose connection issues in the cluster")
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

    @Tool(description = "Diagnose performance issues in the cluster")
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

    @Tool(description = "Run comprehensive diagnostic on the entire cluster")
    public String runComprehensiveDiagnostic() {
        log.info("Tool: Running comprehensive diagnostic");

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
        } catch (Exception e) {
            sb.append("Could not get health summary: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
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