package com.pulsar.diagnostic.knowledge.rerank;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 重排结果模型
 *
 * @param document     重排后的文档
 * @param score        相关性分数 (1-4，1为最高相关)
 * @param reason       相关性评分理由
 * @param originalRank 原始排名
 * @param metadata     额外元数据
 */
public record RerankResult(
        Document document,
        double score,
        String reason,
        int originalRank,
        Map<String, Object> metadata
) {
    /**
     * 创建重排结果
     */
    public static RerankResult of(Document document, double score, String reason, int originalRank) {
        return new RerankResult(document, score, reason, originalRank, Map.of());
    }

    /**
     * 创建重排结果（带元数据）
     */
    public static RerankResult of(Document document, double score, String reason, int originalRank, Map<String, Object> metadata) {
        return new RerankResult(document, score, reason, originalRank, metadata);
    }

    /**
     * 获取文档ID
     */
    public String documentId() {
        return document != null ? document.getId() : null;
    }

    /**
     * 获取文档内容
     */
    public String content() {
        return document != null ? document.getText() : "";
    }

    /**
     * 判断是否高度相关（score <= 2）
     */
    public boolean isHighlyRelevant() {
        return score <= 2.0;
    }
}