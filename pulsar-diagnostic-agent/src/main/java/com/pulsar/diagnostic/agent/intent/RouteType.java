package com.pulsar.diagnostic.agent.intent;

/**
 * 路由类型枚举
 *
 * 定义请求处理的数据来源路由策略
 */
public enum RouteType {
    /**
     * 仅使用知识库（RAG）
     * 适用于概念性问题、最佳实践等
     */
    KNOWLEDGE_ONLY("knowledge"),

    /**
     * 仅使用 MCP 实时数据
     * 适用于实时状态查询、数量统计等
     */
    MCP_ONLY("mcp"),

    /**
     * 混合模式：知识库 + MCP
     * 适用于需要结合知识和实时数据的诊断问题
     */
    HYBRID("hybrid"),

    /**
     * 通用对话模式
     * 不需要特定数据源，使用通用 LLM 能力
     */
    GENERAL_CHAT("general");

    private final String code;

    RouteType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 是否需要知识库
     */
    public boolean needsKnowledge() {
        return this == KNOWLEDGE_ONLY || this == HYBRID;
    }

    /**
     * 是否需要 MCP 数据
     */
    public boolean needsMcp() {
        return this == MCP_ONLY || this == HYBRID;
    }

    /**
     * 从代码解析
     */
    public static RouteType fromCode(String code) {
        for (RouteType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return GENERAL_CHAT;
    }
}