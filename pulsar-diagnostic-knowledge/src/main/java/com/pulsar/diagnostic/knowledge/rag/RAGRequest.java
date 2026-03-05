package com.pulsar.diagnostic.knowledge.rag;

import com.pulsar.diagnostic.knowledge.query.EnhancedQuery;

import java.util.List;

/**
 * RAG 请求
 *
 * @param query       用户查询
 * @param config      RAG 配置
 * @param context     额外上下文（可选）
 */
public record RAGRequest(
        String query,
        RAGConfig config,
        String context
) {
    /**
     * 创建默认请求
     */
    public static RAGRequest of(String query) {
        return new RAGRequest(query, RAGConfig.defaultConfig(), null);
    }

    /**
     * 创建带配置的请求
     */
    public static RAGRequest of(String query, RAGConfig config) {
        return new RAGRequest(query, config, null);
    }

    /**
     * 创建带上下文的请求
     */
    public static RAGRequest of(String query, RAGConfig config, String context) {
        return new RAGRequest(query, config, context);
    }
}