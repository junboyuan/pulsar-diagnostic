package com.pulsar.diagnostic.knowledge.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 检索结果模型
 *
 * @param document   检索到的文档
 * @param score      相似度/相关性分数
 * @param source     检索来源（vector/bm25/hybrid）
 * @param metadata   额外元数据
 */
public record RetrievalResult(
        Document document,
        double score,
        String source,
        Map<String, Object> metadata
) {
    /**
     * 创建向量检索结果
     */
    public static RetrievalResult fromVector(Document document, double score) {
        return new RetrievalResult(document, score, "vector", Map.of());
    }

    /**
     * 创建 BM25 检索结果
     */
    public static RetrievalResult fromBM25(Document document, double score) {
        return new RetrievalResult(document, score, "bm25", Map.of());
    }

    /**
     * 创建混合检索结果
     */
    public static RetrievalResult fromHybrid(Document document, double score, Map<String, Object> metadata) {
        return new RetrievalResult(document, score, "hybrid", metadata);
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
}