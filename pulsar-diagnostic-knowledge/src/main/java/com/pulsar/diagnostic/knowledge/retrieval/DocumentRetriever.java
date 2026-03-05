package com.pulsar.diagnostic.knowledge.retrieval;

import java.util.List;

/**
 * 文档检索器接口
 *
 * 定义统一的文档检索抽象，支持多种检索策略
 */
public interface DocumentRetriever {

    /**
     * 检索相关文档
     *
     * @param query 查询文本
     * @param topK  返回的最大文档数量
     * @return 检索结果列表，按相关性降序排列
     */
    List<RetrievalResult> retrieve(String query, int topK);

    /**
     * 获取检索器名称
     *
     * @return 检索器名称标识
     */
    String getName();

    /**
     * 检查检索器是否已初始化
     *
     * @return 是否已初始化
     */
    default boolean isInitialized() {
        return true;
    }
}