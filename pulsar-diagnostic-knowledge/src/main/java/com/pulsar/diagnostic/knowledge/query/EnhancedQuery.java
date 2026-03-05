package com.pulsar.diagnostic.knowledge.query;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强后的查询模型
 *
 * @param originalQuery   原始查询
 * @param rewrittenQuery  重写后的查询
 * @param expandedQueries 扩展查询列表
 * @param entities        提取的关键实体
 * @param intent          识别的查询意图
 */
public record EnhancedQuery(
        String originalQuery,
        String rewrittenQuery,
        List<String> expandedQueries,
        List<String> entities,
        String intent
) {
    /**
     * 创建默认增强查询（无增强）
     */
    public static EnhancedQuery original(String query) {
        return new EnhancedQuery(query, query, List.of(), List.of(), "unknown");
    }

    /**
     * 创建增强查询
     */
    public static EnhancedQuery of(String original, String rewritten,
                                   List<String> expanded, List<String> entities, String intent) {
        return new EnhancedQuery(original, rewritten, expanded, entities, intent);
    }

    /**
     * 获取所有查询（原始+扩展）
     */
    public List<String> allQueries() {
        List<String> queries = new ArrayList<>();
        queries.add(rewrittenQuery);
        queries.addAll(expandedQueries);
        return queries;
    }
}