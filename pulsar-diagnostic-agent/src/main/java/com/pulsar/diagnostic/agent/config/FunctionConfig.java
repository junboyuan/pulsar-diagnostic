package com.pulsar.diagnostic.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.agent.tool.*;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration for Spring AI Function Calling.
 * Registers tools as functions that the LLM can call automatically.
 */
@Configuration
public class FunctionConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Cluster Status Functions ====================

    @Bean
    public FunctionCallback getClusterInfo(ClusterStatusTool clusterStatusTool) {
        return FunctionCallbackWrapper.builder((String input) -> clusterStatusTool.getClusterInfo())
                .withName("getClusterInfo")
                .withDescription("Get overall Pulsar cluster information including brokers, bookies, and health status. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback performHealthCheck(ClusterStatusTool clusterStatusTool) {
        return FunctionCallbackWrapper.builder((String input) -> clusterStatusTool.performHealthCheck())
                .withName("performHealthCheck")
                .withDescription("Perform a comprehensive health check on the Pulsar cluster. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback getActiveBrokers(ClusterStatusTool clusterStatusTool) {
        return FunctionCallbackWrapper.builder((String input) -> clusterStatusTool.getActiveBrokers())
                .withName("getActiveBrokers")
                .withDescription("Get list of all active brokers in the cluster. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback getBookies(ClusterStatusTool clusterStatusTool) {
        return FunctionCallbackWrapper.builder((String input) -> clusterStatusTool.getBookies())
                .withName("getBookies")
                .withDescription("Get list of all bookies (BookKeeper nodes) in the cluster. No input required.")
                .build();
    }

    // ==================== Topic Info Functions ====================

    @Bean
    public FunctionCallback getTopicInfo(TopicInfoTool topicInfoTool) {
        return FunctionCallbackWrapper.builder((String topicName) -> topicInfoTool.getTopicInfo(topicName))
                .withName("getTopicInfo")
                .withDescription("Get detailed information about a specific Pulsar topic. Input: topic name (e.g., persistent://tenant/namespace/topic)")
                .build();
    }

    @Bean
    public FunctionCallback getTopicStats(TopicInfoTool topicInfoTool) {
        return FunctionCallbackWrapper.builder((String topicName) -> topicInfoTool.getTopicStats(topicName))
                .withName("getTopicStats")
                .withDescription("Get statistics for a specific Pulsar topic. Input: topic name")
                .build();
    }

    @Bean
    public FunctionCallback getTopicSubscriptions(TopicInfoTool topicInfoTool) {
        return FunctionCallbackWrapper.builder((String topicName) -> topicInfoTool.getTopicSubscriptions(topicName))
                .withName("getTopicSubscriptions")
                .withDescription("Get all subscriptions for a specific Pulsar topic. Input: topic name")
                .build();
    }

    @Bean
    public FunctionCallback checkTopicBacklog(TopicInfoTool topicInfoTool) {
        return FunctionCallbackWrapper.builder((String topicName) -> topicInfoTool.checkTopicBacklog(topicName))
                .withName("checkTopicBacklog")
                .withDescription("Check backlog status for a specific Pulsar topic. Input: topic name")
                .build();
    }

    @Bean
    public FunctionCallback listTopicsInNamespace(TopicInfoTool topicInfoTool) {
        return FunctionCallbackWrapper.builder((String namespace) -> topicInfoTool.listTopicsInNamespace(namespace))
                .withName("listTopicsInNamespace")
                .withDescription("List all topics in a namespace. Input: namespace name (e.g., tenant/namespace)")
                .build();
    }

    // ==================== Metrics Functions ====================

    @Bean
    public FunctionCallback getClusterMetrics(BrokerMetricsTool brokerMetricsTool) {
        return FunctionCallbackWrapper.builder((String input) -> brokerMetricsTool.getClusterMetrics())
                .withName("getClusterMetrics")
                .withDescription("Get cluster-wide metrics from Prometheus. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback getBrokerMetrics(BrokerMetricsTool brokerMetricsTool) {
        return FunctionCallbackWrapper.builder((String input) -> brokerMetricsTool.getBrokerMetrics())
                .withName("getBrokerMetrics")
                .withDescription("Get metrics for all brokers in the cluster. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback queryMetric(BrokerMetricsTool brokerMetricsTool) {
        return FunctionCallbackWrapper.builder((String query) -> brokerMetricsTool.queryMetric(query))
                .withName("queryMetric")
                .withDescription("Query a specific Prometheus metric using PromQL. Input: PromQL query string")
                .build();
    }

    @Bean
    public FunctionCallback getAllMetrics(BrokerMetricsTool brokerMetricsTool) {
        return FunctionCallbackWrapper.builder((String input) -> brokerMetricsTool.getAllMetrics())
                .withName("getAllMetrics")
                .withDescription("Get all available Pulsar metrics. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback checkMetricsAvailable(BrokerMetricsTool brokerMetricsTool) {
        return FunctionCallbackWrapper.builder((String input) -> brokerMetricsTool.checkMetricsAvailable())
                .withName("checkMetricsAvailable")
                .withDescription("Check if Prometheus metrics are available. No input required.")
                .build();
    }

    // ==================== Log Analysis Functions ====================

    @Bean
    public FunctionCallback analyzeBrokerLogs(LogAnalysisTool logAnalysisTool) {
        return FunctionCallbackWrapper.builder((String maxLinesStr) -> {
            int maxLines = 500;
            if (maxLinesStr != null && !maxLinesStr.isEmpty()) {
                try {
                    maxLines = Integer.parseInt(maxLinesStr.trim());
                } catch (NumberFormatException e) {
                    // use default
                }
            }
            return logAnalysisTool.analyzeBrokerLogs(maxLines);
        })
                .withName("analyzeBrokerLogs")
                .withDescription("Analyze Pulsar broker logs for errors and patterns. Input: max number of lines (optional, default 500)")
                .build();
    }

    @Bean
    public FunctionCallback analyzeBookieLogs(LogAnalysisTool logAnalysisTool) {
        return FunctionCallbackWrapper.builder((String maxLinesStr) -> {
            int maxLines = 500;
            if (maxLinesStr != null && !maxLinesStr.isEmpty()) {
                try {
                    maxLines = Integer.parseInt(maxLinesStr.trim());
                } catch (NumberFormatException e) {
                    // use default
                }
            }
            return logAnalysisTool.analyzeBookieLogs(maxLines);
        })
                .withName("analyzeBookieLogs")
                .withDescription("Analyze BookKeeper bookie logs for errors and patterns. Input: max number of lines (optional, default 500)")
                .build();
    }

    @Bean
    public FunctionCallback getRecentErrors(LogAnalysisTool logAnalysisTool) {
        return FunctionCallbackWrapper.builder((String maxErrorsStr) -> {
            int maxErrors = 50;
            if (maxErrorsStr != null && !maxErrorsStr.isEmpty()) {
                try {
                    maxErrors = Integer.parseInt(maxErrorsStr.trim());
                } catch (NumberFormatException e) {
                    // use default
                }
            }
            return logAnalysisTool.getRecentErrors(maxErrors);
        })
                .withName("getRecentErrors")
                .withDescription("Get recent error messages from all logs. Input: max number of errors to return (optional, default 50)")
                .build();
    }

    // ==================== Diagnostic Functions ====================

    @Bean
    public FunctionCallback diagnoseBacklogIssue(DiagnosticTool diagnosticTool) {
        return FunctionCallbackWrapper.builder((String input) -> {
            // Parse input as "resource|resourceType" or just "resource"
            String resource = input;
            String resourceType = "topic";
            if (input != null && input.contains("|")) {
                String[] parts = input.split("\\|");
                resource = parts[0].trim();
                if (parts.length > 1) {
                    resourceType = parts[1].trim();
                }
            }
            return diagnosticTool.diagnoseBacklogIssue(resource, resourceType);
        })
                .withName("diagnoseBacklogIssue")
                .withDescription("Diagnose message backlog issues for a topic or namespace. Input: 'resource|resourceType' where resourceType is 'topic' or 'namespace' (default: topic)")
                .build();
    }

    @Bean
    public FunctionCallback diagnoseConnectionIssues(DiagnosticTool diagnosticTool) {
        return FunctionCallbackWrapper.builder((String input) -> diagnosticTool.diagnoseConnectionIssues())
                .withName("diagnoseConnectionIssues")
                .withDescription("Diagnose connection issues in the Pulsar cluster. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback diagnosePerformanceIssues(DiagnosticTool diagnosticTool) {
        return FunctionCallbackWrapper.builder((String input) -> diagnosticTool.diagnosePerformanceIssues())
                .withName("diagnosePerformanceIssues")
                .withDescription("Diagnose performance issues in the Pulsar cluster. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback runComprehensiveDiagnostic(DiagnosticTool diagnosticTool) {
        return FunctionCallbackWrapper.builder((String input) -> diagnosticTool.runComprehensiveDiagnostic())
                .withName("runComprehensiveDiagnostic")
                .withDescription("Run a comprehensive diagnostic on the entire cluster. No input required.")
                .build();
    }

    // ==================== Inspection Functions ====================

    @Bean
    public FunctionCallback performFullInspection(InspectionTool inspectionTool) {
        return FunctionCallbackWrapper.builder((String input) -> inspectionTool.performFullInspection())
                .withName("performFullInspection")
                .withDescription("Perform a full cluster inspection. No input required.")
                .build();
    }

    @Bean
    public FunctionCallback performInspection(InspectionTool inspectionTool) {
        return FunctionCallbackWrapper.builder((String focusArea) -> inspectionTool.performInspection(focusArea))
                .withName("performInspection")
                .withDescription("Perform a focused inspection on a specific area. Input: focus area (e.g., 'brokers', 'topics', 'performance')")
                .build();
    }

    @Bean
    public FunctionCallback quickHealthSnapshot(InspectionTool inspectionTool) {
        return FunctionCallbackWrapper.builder((String input) -> inspectionTool.quickHealthSnapshot())
                .withName("quickHealthSnapshot")
                .withDescription("Get a quick health snapshot of the cluster. No input required.")
                .build();
    }
}