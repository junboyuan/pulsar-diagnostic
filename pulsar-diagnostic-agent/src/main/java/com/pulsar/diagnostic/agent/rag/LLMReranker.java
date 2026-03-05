package com.pulsar.diagnostic.agent.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.knowledge.rerank.RerankResult;
import com.pulsar.diagnostic.knowledge.rerank.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * LLM 重排器
 *
 * 使用大语言模型对检索结果进行相关性评分和重排序
 */
@Component
public class LLMReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(LLMReranker.class);

    private static final String RERANK_PROMPT = """
            你是一个相关性评分专家。请评估以下文档与用户查询的相关性。

            用户查询: %s

            文档内容:
            %s

            评分标准:
            - 1分: 高度相关，直接回答用户问题
            - 2分: 部分相关，包含有用信息但不完整
            - 3分: 低相关性，仅有边缘联系
            - 4分: 不相关

            请只输出JSON格式: {"score": 1, "reason": "简短理由"}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${pulsar-diagnostic.rag.rerank.enabled:true}")
    private boolean enabled = true;

    @Value("${pulsar-diagnostic.rag.rerank.batch-size:5}")
    private int batchSize = 5;

    @Value("${pulsar-diagnostic.rag.rerank.timeout-seconds:30}")
    private int timeoutSeconds = 30;

    // 用于并行评分的线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public LLMReranker(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<RerankResult> rerank(String query, List<Document> documents, int topK) {
        log.debug("LLM 重排: query='{}', documents={}, topK={}",
                truncate(query, 30), documents.size(), topK);

        if (!enabled) {
            log.debug("LLM 重排已禁用，返回原始顺序");
            return createDefaultResults(documents, topK);
        }

        if (documents.isEmpty()) {
            return List.of();
        }

        try {
            // 并行评分
            List<CompletableFuture<ScoredDocument>> futures = documents.stream()
                    .map(doc -> CompletableFuture.supplyAsync(
                            () -> scoreDocument(query, doc),
                            executorService))
                    .collect(Collectors.toList());

            // 等待所有评分完成
            List<ScoredDocument> scoredDocs = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 按分数排序并返回 topK
            List<RerankResult> results = scoredDocs.stream()
                    .sorted(Comparator.comparingDouble(ScoredDocument::score))
                    .limit(topK)
                    .map(sd -> RerankResult.of(
                            sd.document(),
                            sd.score(),
                            sd.reason(),
                            sd.originalRank()))
                    .collect(Collectors.toList());

            log.debug("LLM 重排完成: 返回 {} 条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("LLM 重排失败: {}", e.getMessage());
            return createDefaultResults(documents, topK);
        }
    }

    /**
     * 使用 LLM 对单个文档进行评分
     */
    private ScoredDocument scoreDocument(String query, Document document) {
        try {
            String prompt = String.format(RERANK_PROMPT, query, truncate(document.getText(), 1000));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 解析评分结果
            ScoringResult result = parseScoringResult(response);

            return new ScoredDocument(document, result.score, result.reason, 0);

        } catch (Exception e) {
            log.warn("文档评分失败: {}", e.getMessage());
            // 评分失败时给予中等分数
            return new ScoredDocument(document, 3.0, "评分失败", 0);
        }
    }

    /**
     * 解析 LLM 返回的评分结果
     */
    private ScoringResult parseScoringResult(String response) {
        if (response == null || response.isBlank()) {
            return new ScoringResult(3.0, "无响应");
        }

        try {
            // 尝试提取 JSON
            String json = extractJson(response);
            if (json == null) {
                return new ScoringResult(3.0, "无法解析");
            }

            JsonNode node = objectMapper.readTree(json);
            double score = node.has("score") ? node.get("score").asDouble() : 3.0;
            String reason = node.has("reason") ? node.get("reason").asText() : "";

            // 确保分数在有效范围内
            score = Math.max(1.0, Math.min(4.0, score));

            return new ScoringResult(score, reason);

        } catch (Exception e) {
            log.debug("解析评分失败: {}", e.getMessage());
            return new ScoringResult(3.0, "解析失败");
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 尝试提取 ```json 代码块
        int jsonStart = response.indexOf("```json");
        if (jsonStart >= 0) {
            int jsonEnd = response.indexOf("```", jsonStart + 7);
            if (jsonEnd > jsonStart) {
                return response.substring(jsonStart + 7, jsonEnd).trim();
            }
        }

        // 尝试提取第一个 JSON 对象
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    /**
     * 创建默认结果（不进行重排）
     */
    private List<RerankResult> createDefaultResults(List<Document> documents, int topK) {
        List<RerankResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(documents.size(), topK); i++) {
            results.add(RerankResult.of(documents.get(i), 3.0, "未重排", i));
        }
        return results;
    }

    @Override
    public String getName() {
        return "LLMReranker";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    // 内部记录类

    private record ScoredDocument(Document document, double score, String reason, int originalRank) {}

    private record ScoringResult(double score, String reason) {}
}