package com.pulsar.diagnostic.knowledge.retrieval;

import com.pulsar.diagnostic.knowledge.store.KnowledgeVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量检索器
 *
 * 基于向量语义相似度进行文档检索
 */
@Component
public class VectorRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(VectorRetriever.class);

    private final KnowledgeVectorStore vectorStore;

    public VectorRetriever(KnowledgeVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK) {
        log.debug("向量检索: query='{}', topK={}", truncate(query, 50), topK);

        if (!isInitialized()) {
            log.warn("向量存储未初始化");
            return List.of();
        }

        try {
            List<Document> documents = vectorStore.search(query, topK);

            List<RetrievalResult> results = documents.stream()
                    .map(doc -> RetrievalResult.fromVector(doc, extractScore(doc)))
                    .toList();

            log.debug("向量检索完成: 返回 {} 条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String getName() {
        return "VectorRetriever";
    }

    @Override
    public boolean isInitialized() {
        return vectorStore.isInitialized();
    }

    /**
     * 从文档元数据中提取分数
     */
    private double extractScore(Document doc) {
        if (doc.getMetadata() != null && doc.getMetadata().containsKey("score")) {
            Object score = doc.getMetadata().get("score");
            if (score instanceof Number) {
                return ((Number) score).doubleValue();
            }
        }
        // SimpleVectorStore 不返回分数，使用默认值
        return 0.0;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}