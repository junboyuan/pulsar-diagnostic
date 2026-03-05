package com.pulsar.diagnostic.agent.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.knowledge.query.EnhancedQuery;
import com.pulsar.diagnostic.knowledge.query.QueryEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 查询增强器
 *
 * 使用大语言模型进行查询重写、扩展和实体提取
 */
@Component
public class LLMQueryEnhancer implements QueryEnhancer {

    private static final Logger log = LoggerFactory.getLogger(LLMQueryEnhancer.class);

    private static final String ENHANCE_PROMPT = """
            你是一个查询增强专家。请分析用户查询并进行增强处理。

            用户查询: %s

            请执行以下增强操作:
            1. 查询重写: 将模糊查询转换为更精确的查询
            2. 查询扩展: 生成3个相关的扩展查询，帮助找到更多信息
            3. 实体提取: 提取查询中的关键实体（如组件名、指标名、错误类型等）
            4. 意图识别: 判断用户意图（诊断/查询/配置/性能/其他）

            请严格按以下JSON格式输出，不要添加其他内容:
            {
              "rewritten": "重写后的查询",
              "expanded": ["扩展查询1", "扩展查询2", "扩展查询3"],
              "entities": ["实体1", "实体2"],
              "intent": "诊断"
            }
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${pulsar-diagnostic.rag.query-enhancement.enabled:true}")
    private boolean enabled = true;

    @Value("${pulsar-diagnostic.rag.query-enhancement.expansion-count:3}")
    private int expansionCount = 3;

    public LLMQueryEnhancer(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EnhancedQuery enhance(String query) {
        log.debug("查询增强: query='{}'", truncate(query, 50));

        if (!enabled) {
            log.debug("查询增强已禁用");
            return EnhancedQuery.original(query);
        }

        if (query == null || query.isBlank()) {
            return EnhancedQuery.original(query);
        }

        try {
            // 对于简单查询，跳过增强以提高效率
            if (isSimpleQuery(query)) {
                log.debug("简单查询，跳过增强");
                return EnhancedQuery.original(query);
            }

            String prompt = String.format(ENHANCE_PROMPT, query);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            EnhancedQuery enhanced = parseEnhancedQuery(query, response);

            log.debug("查询增强完成: rewritten='{}', expansions={}, entities={}",
                    truncate(enhanced.rewrittenQuery(), 30),
                    enhanced.expandedQueries().size(),
                    enhanced.entities().size());

            return enhanced;

        } catch (Exception e) {
            log.warn("查询增强失败: {}, 使用原始查询", e.getMessage());
            return EnhancedQuery.original(query);
        }
    }

    /**
     * 解析 LLM 返回的增强查询
     */
    private EnhancedQuery parseEnhancedQuery(String originalQuery, String response) {
        if (response == null || response.isBlank()) {
            return EnhancedQuery.original(originalQuery);
        }

        try {
            String json = extractJson(response);
            if (json == null) {
                return EnhancedQuery.original(originalQuery);
            }

            JsonNode node = objectMapper.readTree(json);

            String rewritten = node.has("rewritten") ? node.get("rewritten").asText() : originalQuery;

            List<String> expanded = new ArrayList<>();
            if (node.has("expanded") && node.get("expanded").isArray()) {
                for (JsonNode expNode : node.get("expanded")) {
                    expanded.add(expNode.asText());
                }
            }

            List<String> entities = new ArrayList<>();
            if (node.has("entities") && node.get("entities").isArray()) {
                for (JsonNode entityNode : node.get("entities")) {
                    entities.add(entityNode.asText());
                }
            }

            String intent = node.has("intent") ? node.get("intent").asText() : "unknown";

            return EnhancedQuery.of(originalQuery, rewritten, expanded, entities, intent);

        } catch (Exception e) {
            log.debug("解析增强查询失败: {}", e.getMessage());
            return EnhancedQuery.original(originalQuery);
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
     * 判断是否为简单查询（无需增强）
     */
    private boolean isSimpleQuery(String query) {
        // 简单查询条件：
        // 1. 长度很短（小于 5 个字符）
        // 2. 只包含一个关键词
        if (query.length() < 5) {
            return true;
        }

        String[] words = query.trim().split("\\s+");
        return words.length == 1;
    }

    @Override
    public String getName() {
        return "LLMQueryEnhancer";
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
}