package com.pulsar.diagnostic.knowledge.rerank;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 重排器接口
 *
 * 对检索结果进行相关性重排序
 */
public interface Reranker {

    /**
     * 对文档列表进行重排序
     *
     * @param query    用户查询
     * @param documents 待重排的文档列表
     * @param topK     返回的最大文档数量
     * @return 重排后的结果列表，按相关性降序排列
     */
    List<RerankResult> rerank(String query, List<Document> documents, int topK);

    /**
     * 获取重排器名称
     *
     * @return 重排器名称标识
     */
    String getName();

    /**
     * 检查重排器是否启用
     *
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}