package com.pulsar.diagnostic.knowledge.rag;

import com.pulsar.diagnostic.knowledge.query.EnhancedQuery;
import com.pulsar.diagnostic.knowledge.query.QueryEnhancer;
import com.pulsar.diagnostic.knowledge.rerank.RerankResult;
import com.pulsar.diagnostic.knowledge.rerank.Reranker;
import com.pulsar.diagnostic.knowledge.retrieval.BM25Retriever;
import com.pulsar.diagnostic.knowledge.retrieval.DocumentRetriever;
import com.pulsar.diagnostic.knowledge.retrieval.HybridRetriever;
import com.pulsar.diagnostic.knowledge.retrieval.RetrievalResult;
import com.pulsar.diagnostic.knowledge.retrieval.VectorRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 服务
 *
 * 提供完整的检索增强生成能力，包括：
 * - 查询增强（重写、扩展、实体提取）
 * - 混合检索（向量 + BM25）
 * - LLM 重排序
 * - 上下文构建
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final QueryEnhancer queryEnhancer;
    private final Reranker reranker;
    private final VectorRetriever vectorRetriever;
    private final BM25Retriever bm25Retriever;
    private final HybridRetriever hybridRetriever;

    @Value("${pulsar-diagnostic.rag.enabled:true}")
    private boolean ragEnabled = true;

    public RAGService(
            QueryEnhancer queryEnhancer,
            Reranker reranker,
            VectorRetriever vectorRetriever,
            BM25Retriever bm25Retriever,
            HybridRetriever hybridRetriever) {
        this.queryEnhancer = queryEnhancer;
        this.reranker = reranker;
        this.vectorRetriever = vectorRetriever;
        this.bm25Retriever = bm25Retriever;
        this.hybridRetriever = hybridRetriever;
    }

    /**
     * 执行 RAG 检索
     *
     * @param query  用户查询
     * @param config RAG 配置
     * @return RAG 响应
     */
    public RAGResponse retrieve(String query, RAGConfig config) {
        log.info("RAG 检索开始: query='{}'", truncate(query, 50));

        long startTime = System.currentTimeMillis();
        long retrievalTime = 0;
        long rerankTime = 0;

        if (!ragEnabled) {
            log.debug("RAG 已禁用");
            return createEmptyResponse(query);
        }

        try {
            // 1. 查询增强
            EnhancedQuery enhancedQuery = enhanceQuery(query, config);
            log.debug("查询增强完成: rewritten='{}'", truncate(enhancedQuery.rewrittenQuery(), 30));

            // 2. 检索
            long retrievalStart = System.currentTimeMillis();
            List<RetrievalResult> retrievalResults = retrieveDocuments(enhancedQuery, config);
            retrievalTime = System.currentTimeMillis() - retrievalStart;
            log.debug("检索完成: 返回 {} 条候选", retrievalResults.size());

            // 3. 重排序
            long rerankStart = System.currentTimeMillis();
            List<RerankResult> rerankResults = rerankDocuments(query, retrievalResults, config);
            rerankTime = System.currentTimeMillis() - rerankStart;
            log.debug("重排完成: 返回 {} 条结果", rerankResults.size());

            // 4. 构建上下文
            String context = buildContext(rerankResults);

            long totalTime = System.currentTimeMillis() - startTime;

            RAGResponse.Metadata metadata = new RAGResponse.Metadata(
                    retrievalTime,
                    rerankTime,
                    totalTime,
                    retrievalResults.size(),
                    config.hybridEnabled() ? "hybrid" : "vector"
            );

            log.info("RAG 检索完成: totalTime={}ms, candidates={}, final={}",
                    totalTime, retrievalResults.size(), rerankResults.size());

            return new RAGResponse(query, enhancedQuery, rerankResults, context, metadata);

        } catch (Exception e) {
            log.error("RAG 检索失败: {}", e.getMessage(), e);
            return createEmptyResponse(query);
        }
    }

    /**
     * 使用默认配置执行 RAG 检索
     */
    public RAGResponse retrieve(String query) {
        return retrieve(query, RAGConfig.defaultConfig());
    }

    /**
     * 使用快速配置执行 RAG 检索
     */
    public RAGResponse retrieveFast(String query) {
        return retrieve(query, RAGConfig.fast());
    }

    /**
     * 查询增强
     */
    private EnhancedQuery enhanceQuery(String query, RAGConfig config) {
        if (!config.queryEnhanceEnabled() || !queryEnhancer.isEnabled()) {
            return EnhancedQuery.original(query);
        }
        return queryEnhancer.enhance(query);
    }

    /**
     * 文档检索
     */
    private List<RetrievalResult> retrieveDocuments(EnhancedQuery query, RAGConfig config) {
        DocumentRetriever retriever = selectRetriever(config);
        return retriever.retrieve(query.rewrittenQuery(), config.retrievalTopK());
    }

    /**
     * 选择检索器
     */
    private DocumentRetriever selectRetriever(RAGConfig config) {
        if (!config.hybridEnabled()) {
            return vectorRetriever;
        }
        return hybridRetriever;
    }

    /**
     * 文档重排
     */
    private List<RerankResult> rerankDocuments(String query, List<RetrievalResult> results, RAGConfig config) {
        if (!config.rerankEnabled() || !reranker.isEnabled()) {
            // 不重排时，直接转换为 RerankResult
            return results.stream()
                    .limit(config.rerankTopK())
                    .map(r -> RerankResult.of(r.document(), r.score(), "未重排", 0))
                    .collect(Collectors.toList());
        }

        List<Document> documents = results.stream()
                .map(RetrievalResult::document)
                .collect(Collectors.toList());

        return reranker.rerank(query, documents, config.rerankTopK());
    }

    /**
     * 构建上下文文本
     */
    private String buildContext(List<RerankResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            RerankResult result = results.get(i);
            if (i > 0) {
                context.append("\n\n---\n\n");
            }
            context.append(result.content());
        }

        return context.toString();
    }

    /**
     * 创建空响应
     */
    private RAGResponse createEmptyResponse(String query) {
        return new RAGResponse(
                query,
                EnhancedQuery.original(query),
                List.of(),
                "",
                RAGResponse.Metadata.empty()
        );
    }

    /**
     * 获取 BM25 检索器（用于索引文档）
     */
    public BM25Retriever getBM25Retriever() {
        return bm25Retriever;
    }

    /**
     * 检查 RAG 是否就绪
     */
    public boolean isReady() {
        return vectorRetriever.isInitialized() || bm25Retriever.isInitialized();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}