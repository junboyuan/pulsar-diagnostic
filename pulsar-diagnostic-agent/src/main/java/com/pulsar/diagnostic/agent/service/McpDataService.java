package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.dto.McpDataResult;
import com.pulsar.diagnostic.agent.intent.IntentResult;
import com.pulsar.diagnostic.agent.intent.RouteType;
import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.agent.mcp.McpToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MCP 数据服务
 *
 * 专门处理 MCP 工具调用和数据获取
 */
@Service
public class McpDataService {

    private static final Logger log = LoggerFactory.getLogger(McpDataService.class);

    /**
     * 意图到 MCP 工具的映射
     */
    private static final Map<String, String[]> INTENT_TOOL_MAPPING = Map.of(
            "backlog-diagnosis", new String[]{"get_topic_backlog", "get_consumer_stats", "list_topics"},
            "cluster-health-check", new String[]{"inspect_cluster", "get_broker_metrics", "list_brokers"},
            "performance-analysis", new String[]{"get_broker_metrics", "get_topic_metrics", "get_cluster_metrics"},
            "connectivity-troubleshoot", new String[]{"check_connectivity", "get_connection_stats", "list_brokers"},
            "capacity-planning", new String[]{"get_cluster_metrics", "get_resource_usage", "list_brokers"},
            "topic-consultation", new String[]{"get_topic_info", "list_topics", "get_topic_config"}
    );

    private final McpToolRegistry mcpToolRegistry;
    private final McpClient mcpClient;

    @Value("${pulsar-diagnostic.mcp.timeout-ms:5000}")
    private long timeoutMs = 5000;

    public McpDataService(McpToolRegistry mcpToolRegistry, McpClient mcpClient) {
        this.mcpToolRegistry = mcpToolRegistry;
        this.mcpClient = mcpClient;
    }

    /**
     * 根据意图获取 MCP 数据
     *
     * @param intent 意图识别结果
     * @param query  用户查询
     * @return MCP 数据结果
     */
    public McpDataResult fetchMcpData(IntentResult intent, String query) {
        if (intent == null || intent.routeType() == RouteType.GENERAL_CHAT) {
            log.debug("不需要 MCP 数据: intent={}", intent != null ? intent.intent() : "null");
            return McpDataResult.empty();
        }

        // 获取建议的工具列表
        List<String> tools = getSuggestedTools(intent);
        if (tools.isEmpty()) {
            log.debug("没有找到匹配的 MCP 工具: intent={}", intent.intent());
            return McpDataResult.empty();
        }

        log.info("获取 MCP 数据: intent={}, tools={}", intent.intent(), tools);
        return fetchMultipleTools(tools, buildDefaultArguments(query));
    }

    /**
     * 批量获取多个工具的数据
     *
     * @param toolNames 工具名称列表
     * @return MCP 数据结果
     */
    public McpDataResult fetchMultipleTools(List<String> toolNames) {
        return fetchMultipleTools(toolNames, Collections.emptyMap());
    }

    /**
     * 批量获取多个工具的数据（带参数）
     *
     * @param toolNames 工具名称列表
     * @param arguments 默认参数
     * @return MCP 数据结果
     */
    public McpDataResult fetchMultipleTools(List<String> toolNames, Map<String, Object> arguments) {
        if (toolNames == null || toolNames.isEmpty()) {
            return McpDataResult.empty();
        }

        long startTime = System.currentTimeMillis();
        Map<String, String> results = new LinkedHashMap<>();

        try {
            // 过滤可用的工具
            List<String> availableTools = toolNames.stream()
                    .filter(mcpToolRegistry::hasTool)
                    .toList();

            if (availableTools.isEmpty()) {
                log.warn("没有可用的 MCP 工具: requested={}", toolNames);
                return McpDataResult.empty();
            }

            // 并行调用工具
            List<CompletableFuture<Map.Entry<String, String>>> futures = availableTools.stream()
                    .map(toolName -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String result = mcpToolRegistry.callTool(toolName, arguments);
                            return Map.entry(toolName, result);
                        } catch (Exception e) {
                            log.warn("调用 MCP 工具失败: tool={}, error={}", toolName, e.getMessage());
                            return Map.entry(toolName, "Error: " + e.getMessage());
                        }
                    }))
                    .toList();

            // 等待所有结果
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            // 收集结果
            for (CompletableFuture<Map.Entry<String, String>> future : futures) {
                Map.Entry<String, String> entry = future.get();
                results.put(entry.getKey(), entry.getValue());
            }

            // 聚合上下文
            String aggregatedContext = aggregateResults(results);

            long fetchTime = System.currentTimeMillis() - startTime;
            log.info("MCP 数据获取完成: tools={}, time={}ms", results.size(), fetchTime);

            return McpDataResult.success(results, aggregatedContext, fetchTime);

        } catch (Exception e) {
            long fetchTime = System.currentTimeMillis() - startTime;
            log.error("MCP 数据获取失败: {}", e.getMessage());
            return McpDataResult.failure(e.getMessage(), fetchTime);
        }
    }

    /**
     * 智能选择工具并获取数据
     *
     * @param query  用户查询
     * @param intent 意图结果
     * @return MCP 数据结果
     */
    public McpDataResult smartFetch(String query, IntentResult intent) {
        // 如果意图已经指定了工具，使用指定的工具
        if (intent.suggestedMcpTools() != null && !intent.suggestedMcpTools().isEmpty()) {
            return fetchMultipleTools(intent.suggestedMcpTools(), buildDefaultArguments(query));
        }

        // 否则根据意图自动选择工具
        return fetchMcpData(intent, query);
    }

    /**
     * 获取建议的 MCP 工具列表
     */
    private List<String> getSuggestedTools(IntentResult intent) {
        // 优先使用意图结果中的工具建议
        if (intent.suggestedMcpTools() != null && !intent.suggestedMcpTools().isEmpty()) {
            return intent.suggestedMcpTools();
        }

        // 根据意图类型查找默认工具
        String[] defaultTools = INTENT_TOOL_MAPPING.get(intent.intent());
        if (defaultTools != null) {
            return Arrays.asList(defaultTools);
        }

        return List.of();
    }

    /**
     * 构建默认参数
     */
    private Map<String, Object> buildDefaultArguments(String query) {
        Map<String, Object> args = new HashMap<>();
        args.put("query", query);
        return args;
    }

    /**
     * 聚合多个工具的结果
     */
    private String aggregateResults(Map<String, String> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 实时集群数据\n\n");

        for (Map.Entry<String, String> entry : results.entrySet()) {
            String toolName = entry.getKey();
            String result = entry.getValue();

            sb.append("### ").append(formatToolName(toolName)).append("\n");
            sb.append("```\n");
            sb.append(result);
            sb.append("\n```\n\n");
        }

        return sb.toString();
    }

    /**
     * 格式化工具名称
     */
    private String formatToolName(String toolName) {
        // 将 snake_case 转换为更友好的名称
        String[] parts = toolName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 检查 MCP 服务是否可用
     */
    public boolean isAvailable() {
        return mcpToolRegistry != null && !mcpToolRegistry.getAllTools().isEmpty();
    }

    /**
     * 获取可用的工具列表
     */
    public List<String> getAvailableTools() {
        return new ArrayList<>(mcpToolRegistry.getAllTools().keySet());
    }
}