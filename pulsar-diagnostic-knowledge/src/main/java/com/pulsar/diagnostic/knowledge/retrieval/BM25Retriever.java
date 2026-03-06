package com.pulsar.diagnostic.knowledge.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * BM25 关键词检索器
 *
 * 基于 BM25 算法进行关键词匹配检索，支持中文简单分词
 */
@Component
public class BM25Retriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(BM25Retriever.class);

    // BM25 参数
    private static final double K1 = 1.2;   // 词频饱和参数
    private static final double B = 0.75;   // 文档长度归一化参数

    // 中文分词正则：按标点和空白分割
    private static final Pattern CHINESE_TOKENIZER = Pattern.compile(
            "[\\s\\p{Punct}]+|(?<=[\\u4e00-\\u9fa5])(?=[A-Za-z0-9])|(?<=[A-Za-z0-9])(?=[\\u4e00-\\u9fa5])"
    );

    // 文档存储
    private final List<Document> documents = new ArrayList<>();

    // 倒排索引：term -> 文档ID列表
    private final Map<String, List<IndexEntry>> invertedIndex = new HashMap<>();

    // 文档长度统计
    private final Map<String, Integer> docLengths = new HashMap<>();
    private double avgDocLength = 0.0;

    // 文档ID到文档的映射
    private final Map<String, Document> docIdMap = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("BM25 检索器初始化完成");
    }

    /**
     * 添加文档到索引
     */
    public synchronized void addDocuments(List<Document> newDocuments) {
        if (newDocuments == null || newDocuments.isEmpty()) {
            return;
        }

        log.info("BM25 索引添加 {} 篇文档", newDocuments.size());

        for (Document doc : newDocuments) {
            String docId = doc.getId();
            if (docIdMap.containsKey(docId)) {
                continue; // 避免重复
            }

            documents.add(doc);
            docIdMap.put(docId, doc);

            String content = doc.getText();
            List<String> tokens = tokenize(content);
            docLengths.put(docId, tokens.size());

            // 构建倒排索引
            Map<String, Integer> termFreq = new HashMap<>();
            for (String token : tokens) {
                termFreq.merge(token, 1, Integer::sum);
            }

            for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
                String term = entry.getKey();
                int freq = entry.getValue();
                invertedIndex.computeIfAbsent(term, k -> new ArrayList<>())
                        .add(new IndexEntry(docId, freq));
            }
        }

        // 重新计算平均文档长度
        avgDocLength = docLengths.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1.0);

        log.info("BM25 索引构建完成: 总文档数={}, 词汇量={}", documents.size(), invertedIndex.size());
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK) {
        log.debug("BM25 检索: query='{}', topK={}", truncate(query, 50), topK);

        if (!isInitialized()) {
            log.warn("BM25 索引未初始化");
            return List.of();
        }

        try {
            List<String> queryTokens = tokenize(query);
            Map<String, Double> scores = new HashMap<>();

            // 对每个查询词计算 BM25 分数
            for (String token : queryTokens) {
                List<IndexEntry> postings = invertedIndex.get(token);
                if (postings == null) {
                    continue;
                }

                // IDF 计算
                double idf = calculateIDF(postings.size());

                // 对每个包含该词的文档计算分数
                for (IndexEntry entry : postings) {
                    String docId = entry.docId;
                    int docLength = docLengths.getOrDefault(docId, 1);
                    int tf = entry.termFrequency;

                    // BM25 公式
                    double tfNorm = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * docLength / avgDocLength));
                    double score = idf * tfNorm;

                    scores.merge(docId, score, Double::sum);
                }
            }

            // 排序并返回 topK
            List<RetrievalResult> results = scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(topK)
                    .map(entry -> {
                        Document doc = docIdMap.get(entry.getKey());
                        return RetrievalResult.fromBM25(doc, entry.getValue());
                    })
                    .collect(Collectors.toList());

            log.debug("BM25 检索完成: 返回 {} 条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("BM25 检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 计算 IDF (Inverse Document Frequency)
     */
    private double calculateIDF(int docFreq) {
        int totalDocs = documents.size();
        if (totalDocs == 0) return 0;
        // BM25 IDF 变体，避免负值
        return Math.log(1 + (totalDocs - docFreq + 0.5) / (docFreq + 0.5));
    }

    /**
     * 文本分词
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // 简单分词：按标点和空白分割，保留中文单字
        List<String> tokens = new ArrayList<>();

        // 使用正则分割
        String[] parts = CHINESE_TOKENIZER.split(text.toLowerCase());

        for (String part : parts) {
            if (part.isBlank()) continue;

            // 中文单字切分
            if (isAllChinese(part)) {
                for (char c : part.toCharArray()) {
                    tokens.add(String.valueOf(c));
                }
            } else {
                // 英文/数字作为整体
                tokens.add(part);
            }
        }

        return tokens;
    }

    /**
     * 判断是否全为中文
     */
    private boolean isAllChinese(String str) {
        for (char c : str.toCharArray()) {
            if (c < '\u4e00' || c > '\u9fa5') {
                return false;
            }
        }
        return !str.isEmpty();
    }

    @Override
    public String getName() {
        return "BM25Retriever";
    }

    @Override
    public boolean isInitialized() {
        return !documents.isEmpty();
    }

    /**
     * 清空索引
     */
    public synchronized void clear() {
        documents.clear();
        invertedIndex.clear();
        docLengths.clear();
        docIdMap.clear();
        avgDocLength = 0.0;
        log.info("BM25 索引已清空");
    }

    /**
     * 获取索引统计信息
     */
    public IndexStats getStats() {
        return new IndexStats(documents.size(), invertedIndex.size(), avgDocLength);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    // 内部类

    /**
     * 索引条目
     */
    private record IndexEntry(String docId, int termFrequency) {}

    /**
     * 索引统计信息
     */
    public record IndexStats(int documentCount, int vocabularySize, double avgDocLength) {}
}