package com.pulsar.diagnostic.agent.dto;

import java.util.Collections;
import java.util.Map;

/**
 * MCP 数据获取结果
 *
 * 封装从 MCP 服务获取的实时数据
 */
public record McpDataResult(
        /**
         * 是否成功获取数据
         */
        boolean success,

        /**
         * 工具名称到结果的映射
         */
        Map<String, String> toolResults,

        /**
         * 聚合后的上下文文本
         */
        String aggregatedContext,

        /**
         * 错误信息（如果失败）
         */
        String errorMessage,

        /**
         * 获取数据耗时（毫秒）
         */
        long fetchTimeMs
) {
    /**
     * 创建成功结果
     */
    public static McpDataResult success(Map<String, String> toolResults, String context, long fetchTimeMs) {
        return new McpDataResult(true, toolResults, context, null, fetchTimeMs);
    }

    /**
     * 创建失败结果
     */
    public static McpDataResult failure(String errorMessage, long fetchTimeMs) {
        return new McpDataResult(false, Collections.emptyMap(), null, errorMessage, fetchTimeMs);
    }

    /**
     * 创建空结果
     */
    public static McpDataResult empty() {
        return new McpDataResult(true, Collections.emptyMap(), "", null, 0);
    }

    /**
     * 是否有数据
     */
    public boolean hasData() {
        return success && aggregatedContext != null && !aggregatedContext.isEmpty();
    }

    /**
     * 获取工具调用数量
     */
    public int toolCount() {
        return toolResults != null ? toolResults.size() : 0;
    }
}