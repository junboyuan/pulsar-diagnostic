package com.pulsar.diagnostic.agent.config;

import com.pulsar.diagnostic.agent.tool.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for Spring AI Tool Calling.
 * Registers tools that the LLM can call automatically using Spring AI 2.x API.
 */
@Configuration
public class ToolConfig {

    /**
     * Create a single bean containing all tool callbacks.
     * Spring AI 2.0.0-M2 expects a single List<ToolCallback> bean.
     */
    @Bean
    public List<ToolCallback> pulsarDiagnosticTools(
            ClusterStatusTool clusterStatusTool,
            TopicInfoTool topicInfoTool,
            BrokerMetricsTool brokerMetricsTool,
            LogAnalysisTool logAnalysisTool,
            DiagnosticTool diagnosticTool,
            InspectionTool inspectionTool) {

        List<ToolCallback> allTools = new ArrayList<>();

        // Cluster Status Tools
        allTools.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(clusterStatusTool)
                .build()
                .getToolCallbacks()));

        // Topic Info Tools
        allTools.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(topicInfoTool)
                .build()
                .getToolCallbacks()));

        // Broker Metrics Tools
        allTools.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(brokerMetricsTool)
                .build()
                .getToolCallbacks()));

        // Log Analysis Tools
        allTools.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(logAnalysisTool)
                .build()
                .getToolCallbacks()));

        // Diagnostic Tools
        allTools.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(diagnosticTool)
                .build()
                .getToolCallbacks()));

        // Inspection Tools
        allTools.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(inspectionTool)
                .build()
                .getToolCallbacks()));

        return allTools;
    }
}