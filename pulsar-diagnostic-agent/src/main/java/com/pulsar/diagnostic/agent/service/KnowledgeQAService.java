package com.pulsar.diagnostic.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.agent.dto.QAResponse;
import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识问答服务
 *
 * 专门处理基于知识库的问答，使用结构化输出
 */
@Service
public class KnowledgeQAService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQAService.class);

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\{\\s*\"useful\"\\s*:\\s*(true|false)\\s*,\\s*\"content\"\\s*:\\s*\"[^\"]*\"\\s*,\\s*\"translation\"\\s*:\\s*\"[^\"]*\"\\s*\\}",
            Pattern.DOTALL
    );

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;

    public KnowledgeQAService(ChatClient chatClient,
                              KnowledgeBaseService knowledgeBaseService,
                              PromptTemplates promptTemplates) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptTemplates = promptTemplates;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行知识问答
     *
     * @param userMessage         用户问题
     * @param conversationHistory 对话历史（可选）
     * @return 结构化的问答响应
     */
    public QAResponse ask(String userMessage, List<String> conversationHistory) {
        log.info("处理知识问答: {}", truncate(userMessage, 50));

        try {
            // 1. 检索相关知识
            String knowledgeContext = retrieveKnowledge(userMessage);
            log.debug("检索到知识上下文长度: {}", knowledgeContext != null ? knowledgeContext.length() : 0);

            // 2. 构建历史上下文
            String historyContext = formatConversationHistory(conversationHistory);

            // 3. 生成提示词
            String prompt = promptTemplates.generateQAPrompt(
                    knowledgeContext != null ? knowledgeContext : "暂无相关知识库内容",
                    historyContext,
                    userMessage
            );

            // 4. 调用 LLM
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 5. 解析响应
            QAResponse qaResponse = parseResponse(response);

            log.info("知识问答完成: useful={}, contentLength={}",
                    qaResponse.useful(), qaResponse.content().length());

            return qaResponse;

        } catch (Exception e) {
            log.error("知识问答处理失败", e);
            return QAResponse.unknown();
        }
    }

    /**
     * 执行知识问答（无历史）
     */
    public QAResponse ask(String userMessage) {
        return ask(userMessage, null);
    }

    /**
     * 检索相关知识
     */
    private String retrieveKnowledge(String query) {
        if (!knowledgeBaseService.isReady()) {
            log.debug("知识库未就绪");
            return null;
        }

        try {
            var context = knowledgeBaseService.searchWithContext(query, 5);
            if (context.items().isEmpty()) {
                return null;
            }
            return context.context();
        } catch (Exception e) {
            log.debug("知识检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 格式化对话历史
     */
    private String formatConversationHistory(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "暂无对话历史";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            String role = (i % 2 == 0) ? "用户" : "助手";
            sb.append(role).append(": ").append(history.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析 LLM 响应为 QAResponse
     */
    private QAResponse parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return QAResponse.unknown();
        }

        try {
            // 提取 JSON
            String json = extractJson(response);
            if (json == null) {
                log.warn("无法从响应中提取JSON: {}", truncate(response, 100));
                // 尝试直接作为内容返回
                return new QAResponse(true, response, "");
            }

            // 解析 JSON
            JsonNode node = objectMapper.readTree(json);

            boolean useful = node.has("useful") && node.get("useful").asBoolean();
            String content = node.has("content") ? node.get("content").asText() : "";
            String translation = node.has("translation") ? node.get("translation").asText() : "";

            return new QAResponse(useful, content, translation);

        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
            return QAResponse.unknown();
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 尝试匹配标准 JSON 格式
        Matcher matcher = JSON_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }

        // 尝试提取 ```json 代码块
        int jsonStart = response.indexOf("```json");
        if (jsonStart >= 0) {
            int jsonEnd = response.indexOf("```", jsonStart + 7);
            if (jsonEnd > jsonStart) {
                return response.substring(jsonStart + 7, jsonEnd).trim();
            }
        }

        // 尝试提取第一个完整的 JSON 对象
        int start = response.indexOf('{');
        int end = findMatchingBrace(response, start);
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    /**
     * 找到匹配的右大括号
     */
    private int findMatchingBrace(String str, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}