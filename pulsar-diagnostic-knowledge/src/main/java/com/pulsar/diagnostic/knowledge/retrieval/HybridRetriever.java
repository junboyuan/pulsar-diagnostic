package com.pulsar.diagnostic.knowledge.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索器
 *
 * 融合向量检索和 BM25 检索结果，使用 Reciprocal Rank Fusion (RRF) 算法
 */
@Component
public class HybridRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridRetriever.class);

    // RRF 常数 K，用于平滑排名影响
    private static final int DEFAULT_RRF_K = 60;

    private final VectorRetriever vectorRetriever;
    private final BM25Retriever bm25Retriever;

    @Value("${pulsar-diagnostic.rag.retrieval.vector-weight:0.6}")
    private double vectorWeight = 0.6;

    @Value("${pulsar-diagnostic.rag.retrieval.bm25-weight:0.4}")
    private double bm25Weight = 0.4;

    @Value("${pulsar-diagnostic.rag.retrieval.rrf-k:60}")
    private int rrfK = DEFAULT_RRF_K;

    public HybridRetriever(VectorRetriever vectorRetriever, BM25Retriever bm25Retriever) {
        this.vectorRetriever = vectorRetriever;
        this.bm25Retriever = bm25Retriever;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK) {
        log.debug("混合检索: query='{}', topK={}", truncate(query, 50), topK);

        // 如果只有单一检索器可用，降级使用
        if (!vectorRetriever.isInitialized() && !bm25Retriever.isInitialized()) {
            log.warn("所有检索器未初始化");
            return List.of();
        }

        if (!vectorRetriever.isInitialized()) {
            log.debug("降级使用 BM25 检索");
            return bm25Retriever.retrieve(query, topK);
        }

        if (!bm25Retriever.isInitialized()) {
            log.debug("降级使用向量检索");
            return vectorRetriever.retrieve(query, topK);
        }

        // 混合检索：获取更多候选，然后融合
        int candidateK = topK * 2;

        List<RetrievalResult> vectorResults = vectorRetriever.retrieve(query, candidateK);
        List<RetrievalResult> bm25Results = bm25Retriever.retrieve(query, candidateK);

        // RRF 融合
        List<RetrievalResult> fusedResults = reciprocalRankFusion(vectorResults, bm25Results, topK);

        log.debug("混合检索完成: 向量结果={}, BM25结果={}, 融合后={}",
                vectorResults.size(), bm25Results.size(), fusedResults.size());

        return fusedResults;
    }

    /**
     * Reciprocal Rank Fusion (RRF) 算法
     *
     * RRF 公式: score = 1 / (k + rank)
     * 融合分数 = 向量权重 * RRF(向量排名) + BM25权重 * RRF(BM25排名)
     */
    private List<RetrievalResult> reciprocalRankFusion(
            List<RetrievalResult> vectorResults,
            List<RetrievalResult> bm25Results,
            int topK) {

        // 文档ID -> 融合分数
        Map<String, Double> fusionScores = new HashMap<>();
        // 文档ID -> 检索结果
        Map<String, RetrievalResult> resultMap = new HashMap<>();

        // 处理向量检索结果
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            RetrievalResult result = vectorResults.get(rank);
            String docId = result.documentId();
            if (docId == null) continue;

            double rrfScore = vectorWeight / (rrfK + rank + 1);
            fusionScores.merge(docId, rrfScore, Double::sum);
            resultMap.putIfAbsent(docId, result);
        }

        // 处理 BM25 检索结果
        for (int rank = 0; rank < bm25Results.size(); rank++) {
            RetrievalResult result = bm25Results.get(rank);
            String docId = result.documentId();
            if (docId == null) continue;

            double rrfScore = bm25Weight / (rrfK + rank + 1);
            fusionScores.merge(docId, rrfScore, Double::sum);
            resultMap.putIfAbsent(docId, result);
        }

        // 按融合分数排序并返回 topK
        return fusionScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    String docId = entry.getKey();
                    double score = entry.getValue();
                    RetrievalResult original = resultMap.get(docId);

                    // 创建带融合分数的新结果
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("fusionScore", score);
                    metadata.put("originalSource", original.source());
                    metadata.put("originalScore", original.score());

                    return RetrievalResult.fromHybrid(original.document(), score, metadata);
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "HybridRetriever";
    }

    @Override
    public boolean isInitialized() {
        return vectorRetriever.isInitialized() || bm25Retriever.isInitialized();
    }

    /**
     * 设置检索权重
     */
    public void setWeights(double vectorWeight, double bm25Weight) {
        if (vectorWeight < 0 || bm25Weight < 0) {
            throw new IllegalArgumentException("权重必须为非负数");
        }
        this.vectorWeight = vectorWeight;
        this.bm25Weight = bm25Weight;
        log.info("检索权重更新: vector={}, bm25={}", vectorWeight, bm25Weight);
    }

    /**
     * 设置 RRF K 参数
     */
    public void setRrfK(int rrfK) {
        if (rrfK <= 0) {
            throw new IllegalArgumentException("RRF K 必须为正数");
        }
        this.rrfK = rrfK;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}