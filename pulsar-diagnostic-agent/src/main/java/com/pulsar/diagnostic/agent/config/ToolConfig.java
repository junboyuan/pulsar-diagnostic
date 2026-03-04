package com.pulsar.diagnostic.agent.config;

import com.pulsar.diagnostic.agent.tool.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for Spring AI Tool Calling.
 * Registers tools that the LLM can call automatically using Spring AI 2.x API.
 */
@Configuration
public class ToolConfig {

    // ==================== Cluster Status Tools ====================

    @Bean
    public List<ToolCallback> clusterStatusTools(ClusterStatusTool clusterStatusTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(clusterStatusTool)
                .build()
                .getToolCallbacks());
    }

    // ==================== Topic Info Tools ====================

    @Bean
    public List<ToolCallback> topicInfoTools(TopicInfoTool topicInfoTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(topicInfoTool)
                .build()
                .getToolCallbacks());
    }

    // ==================== Metrics Tools ====================

    @Bean
    public List<ToolCallback> brokerMetricsTools(BrokerMetricsTool brokerMetricsTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(brokerMetricsTool)
                .build()
                .getToolCallbacks());
    }

    // ==================== Log Analysis Tools ====================

    @Bean
    public List<ToolCallback> logAnalysisTools(LogAnalysisTool logAnalysisTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(logAnalysisTool)
                .build()
                .getToolCallbacks());
    }

    // ==================== Diagnostic Tools ====================

    @Bean
    public List<ToolCallback> diagnosticTools(DiagnosticTool diagnosticTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(diagnosticTool)
                .build()
                .getToolCallbacks());
    }

    // ==================== Inspection Tools ====================

    @Bean
    public List<ToolCallback> inspectionTools(InspectionTool inspectionTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(inspectionTool)
                .build()
                .getToolCallbacks());
    }
}