package com.pulsar.diagnostic.agent.dto;

import com.pulsar.diagnostic.agent.intent.RouteType;
import java.util.Collections;
import java.util.List;

/**
 * 编排器响应
 *
 * 封装完整的响应处理结果，包括上下文来源信息
 */
public record OrchestratorResponse(
        /**
         * 最终回复内容
         */
        String content,

        /**
         * 识别的意图
         */
        String intent,

        /**
         * 路由类型
         */
        RouteType routeType,

        /**
         * 知识库上下文
         */
        String knowledgeContext,

        /**
         * MCP 数据上下文
         */
        String mcpContext,

        /**
         * 置信度
         */
        double confidence,

        /**
         * 总处理时间（毫秒）
         */
        long totalTimeMs
) {
    /**
     * 创建简单响应
     */
    public static OrchestratorResponse simple(String content, String intent, RouteType routeType) {
        return new OrchestratorResponse(content, intent, routeType, null, null, 1.0, 0);
    }

    /**
     * 创建完整响应
     */
    public static OrchestratorResponse full(String content, String intent, RouteType routeType,
                                            String knowledgeContext, String mcpContext,
                                            double confidence, long totalTimeMs) {
        return new OrchestratorResponse(content, intent, routeType, knowledgeContext, mcpContext, confidence, totalTimeMs);
    }

    /**
     * 创建错误响应
     */
    public static OrchestratorResponse error(String errorMessage) {
        return new OrchestratorResponse(
                "处理请求时遇到错误：" + errorMessage,
                "error",
                RouteType.GENERAL_CHAT,
                null,
                null,
                0.0,
                0
        );
    }

    /**
     * 是否使用了知识库
     */
    public boolean usedKnowledge() {
        return knowledgeContext != null && !knowledgeContext.isEmpty();
    }

    /**
     * 是否使用了 MCP
     */
    public boolean usedMcp() {
        return mcpContext != null && !mcpContext.isEmpty();
    }
}