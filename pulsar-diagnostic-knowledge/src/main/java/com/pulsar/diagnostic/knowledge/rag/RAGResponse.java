package com.pulsar.diagnostic.knowledge.rag;

import com.pulsar.diagnostic.knowledge.query.EnhancedQuery;
import com.pulsar.diagnostic.knowledge.rerank.RerankResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 响应
 *
 * @param originalQuery  原始查询
 * @param enhancedQuery  增强后的查询
 * @param results        重排后的结果列表
 * @param context        构建的上下文文本
 * @param metadata       元数据
 */
public record RAGResponse(
        String originalQuery,
        EnhancedQuery enhancedQuery,
        List<RerankResult> results,
        String context,
        Metadata metadata
) {
    /**
     * 获取文档内容列表
     */
    public List<String> contents() {
        return results.stream()
                .map(RerankResult::content)
                .toList();
    }

    /**
     * 判断是否有结果
     */
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /**
     * 获取结果数量
     */
    public int resultCount() {
        return results != null ? results.size() : 0;
    }

    /**
     * 元数据
     */
    public record Metadata(
            long retrievalTimeMs,
            long rerankTimeMs,
            long totalTimeMs,
            int candidateCount,
            String retrievalSource
    ) {
        public static Metadata empty() {
            return new Metadata(0, 0, 0, 0, "unknown");
        }
    }
}