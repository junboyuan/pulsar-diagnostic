package com.pulsar.diagnostic.agent.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图识别服务
 *
 * 分析用户输入，识别意图并判断数据来源路由
 * 支持对话历史和指代消解
 */
@Service
public class IntentRecognizer {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognizer.class);

    private static final String INTENT_PROMPT_FILE = "prompts/intent-recognition.md";
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*\"intent\"[^{}]*\\}", Pattern.DOTALL);

    // 技能关键词映射
    private static final Map<String, String[]> SKILL_KEYWORDS = Map.of(
            "backlog-diagnosis", new String[]{"积压", "backlog", "消费延迟", "消息堆积", "lag", "积压诊断", "消费慢"},
            "cluster-health-check", new String[]{"健康", "状态", "检查", "health", "监控", "集群状态", "健康检查"},
            "performance-analysis", new String[]{"性能", "吞吐量", "延迟", "慢", "优化", "performance", "性能分析", "瓶颈"},
            "connectivity-troubleshoot", new String[]{"连接", "网络", "认证", "超时", "connect", "连接失败", "网络问题"},
            "capacity-planning", new String[]{"容量", "扩容", "规划", "资源", "capacity", "容量规划", "扩容建议"},
            "topic-consultation", new String[]{"主题", "分区", "保留", "配置", "topic", "主题设计", "分区策略"}
    );

    // 意图到 MCP 工具的映射
    private static final Map<String, String[]> INTENT_MCP_TOOLS = Map.of(
            "backlog-diagnosis", new String[]{"get_topic_backlog", "get_consumer_stats"},
            "cluster-health-check", new String[]{"inspect_cluster", "get_broker_metrics"},
            "performance-analysis", new String[]{"get_broker_metrics", "get_topic_metrics"},
            "connectivity-troubleshoot", new String[]{"check_connectivity", "get_connection_stats"},
            "capacity-planning", new String[]{"get_cluster_metrics", "get_resource_usage"},
            "topic-consultation", new String[]{"get_topic_info", "list_topics"}
    );

    // 需要 MCP 数据的关键词
    private static final String[] MCP_INDICATOR_KEYWORDS = {
            "当前", "现在", "多少", "显示", "列出", "查看", "查询", "状态", "列表",
            "为什么", "怎么", "如何", "诊断", "分析", "排查", "检查"
    };

    private static final Set<String> VALID_INTENTS = Set.of(
            "backlog-diagnosis",
            "cluster-health-check",
            "performance-analysis",
            "connectivity-troubleshoot",
            "capacity-planning",
            "topic-consultation",
            "general"
    );

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private String intentPrompt;

    public IntentRecognizer(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        loadIntentPrompt();
    }

    private void loadIntentPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(INTENT_PROMPT_FILE);
            if (resource.exists()) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                intentPrompt = content.toString();
                log.info("已加载意图识别提示词模板");
            } else {
                log.warn("意图识别提示词文件不存在，使用默认逻辑");
                intentPrompt = null;
            }
        } catch (IOException e) {
            log.error("加载意图识别提示词失败: {}", e.getMessage());
            intentPrompt = null;
        }
    }

    /**
     * 识别用户输入的意图
     *
     * @param userInput 用户输入
     * @return 意图识别结果
     */
    public IntentResult recognize(String userInput) {
        return recognize(userInput, null);
    }

    /**
     * 识别用户输入的意图（带对话历史）
     *
     * @param userInput           用户输入
     * @param conversationHistory 对话历史
     * @return 意图识别结果
     */
    public IntentResult recognize(String userInput, List<String> conversationHistory) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentResult.general();
        }

        log.info("识别用户意图: {}", truncate(userInput, 50));

        // 如果有对话历史，直接使用 LLM 进行深度识别（支持指代消解）
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            return recognizeWithLLM(userInput, conversationHistory);
        }

        // 首先尝试关键词快速匹配
        IntentResult quickResult = quickMatch(userInput);
        if (quickResult != null && quickResult.isConfident()) {
            log.info("关键词快速匹配成功: {} (置信度: {})", quickResult.intent(), quickResult.confidence());
            return quickResult;
        }

        // 使用 LLM 进行深度识别
        return recognizeWithLLM(userInput, null);
    }

    /**
     * 关键词快速匹配
     */
    private IntentResult quickMatch(String input) {
        String lowerInput = input.toLowerCase();
        String bestIntent = null;
        int bestScore = 0;

        for (Map.Entry<String, String[]> entry : SKILL_KEYWORDS.entrySet()) {
            String skill = entry.getKey();
            String[] keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (lowerInput.contains(keyword.toLowerCase())) {
                    score++;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestIntent = skill;
            }
        }

        if (bestIntent != null && bestScore >= 1) {
            double confidence = Math.min(0.95, 0.5 + (bestScore * 0.15));
            boolean needsMcp = detectMcpNeed(input, bestIntent);
            RouteType routeType = determineRouteType(bestIntent, needsMcp);
            List<String> suggestedTools = getSuggestedTools(bestIntent, needsMcp);

            return IntentResult.of(
                    bestIntent,
                    confidence,
                    "关键词匹配: " + bestScore + " 个关键词",
                    "调用" + bestIntent + "技能处理",
                    input,
                    "",
                    needsMcp,
                    suggestedTools,
                    routeType
            );
        }

        return null;
    }

    /**
     * 检测是否需要 MCP 数据
     */
    private boolean detectMcpNeed(String input, String intent) {
        String lowerInput = input.toLowerCase();

        // 检查 MCP 指示关键词
        for (String keyword : MCP_INDICATOR_KEYWORDS) {
            if (lowerInput.contains(keyword)) {
                // 排除纯概念性问题
                if (lowerInput.contains("什么是") || lowerInput.contains("概念") ||
                    lowerInput.contains("原理") || lowerInput.contains("介绍")) {
                    return false;
                }
                return true;
            }
        }

        // 根据意图类型判断
        return "backlog-diagnosis".equals(intent) ||
               "cluster-health-check".equals(intent) ||
               "performance-analysis".equals(intent) ||
               "connectivity-troubleshoot".equals(intent);
    }

    /**
     * 确定路由类型
     */
    private RouteType determineRouteType(String intent, boolean needsMcp) {
        if ("general".equals(intent)) {
            return RouteType.GENERAL_CHAT;
        }

        if (needsMcp) {
            // 诊断类问题需要混合模式
            if ("backlog-diagnosis".equals(intent) ||
                "performance-analysis".equals(intent) ||
                "connectivity-troubleshoot".equals(intent)) {
                return RouteType.HYBRID;
            }
            // 状态查询类只需要 MCP
            if ("cluster-health-check".equals(intent)) {
                return RouteType.HYBRID; // 也需要知识来解释状态
            }
            return RouteType.HYBRID;
        }

        // 咨询类问题只需要知识
        if ("topic-consultation".equals(intent) || "capacity-planning".equals(intent)) {
            return RouteType.KNOWLEDGE_ONLY;
        }

        return RouteType.KNOWLEDGE_ONLY;
    }

    /**
     * 获取建议的 MCP 工具
     */
    private List<String> getSuggestedTools(String intent, boolean needsMcp) {
        if (!needsMcp) {
            return List.of();
        }

        String[] tools = INTENT_MCP_TOOLS.get(intent);
        if (tools != null) {
            return Arrays.asList(tools);
        }
        return List.of();
    }

    /**
     * 使用 LLM 进行意图识别
     */
    private IntentResult recognizeWithLLM(String userInput, List<String> conversationHistory) {
        if (intentPrompt == null) {
            // 没有 LLM 提示词，使用关键词匹配结果或返回通用意图
            IntentResult quickResult = quickMatch(userInput);
            return quickResult != null ? quickResult : IntentResult.general();
        }

        try {
            String prompt = buildPrompt(userInput, conversationHistory);
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            IntentResult result = parseResponse(response, userInput);
            log.info("LLM意图识别结果: intent={}, routeType={}, needsMcp={}, confidence={}",
                    result.intent(), result.routeType(), result.needsMcpData(), result.confidence());

            return result;

        } catch (Exception e) {
            log.error("LLM意图识别失败: {}", e.getMessage());
            // 降级到关键词匹配
            IntentResult quickResult = quickMatch(userInput);
            return quickResult != null ? quickResult : IntentResult.general();
        }
    }

    /**
     * 构建意图识别提示词
     */
    private String buildPrompt(String userInput, List<String> conversationHistory) {
        // 提取意图识别模板部分
        String template = extractTemplate();

        // 格式化对话历史
        String historyContext = formatConversationHistory(conversationHistory);

        return template
                .replace("{conversation_history}", historyContext)
                .replace("{user_input}", userInput);
    }

    /**
     * 格式化对话历史
     */
    private String formatConversationHistory(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "无对话历史";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            String role = (i % 2 == 0) ? "用户" : "助手";
            sb.append("- ").append(role).append(": ").append(history.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 提取意图识别模板
     */
    private String extractTemplate() {
        if (intentPrompt == null) {
            return buildDefaultTemplate();
        }

        // 查找模板部分
        int templateStart = intentPrompt.indexOf("## 意图识别模板");
        if (templateStart >= 0) {
            String template = intentPrompt.substring(templateStart);
            // 移除标题行
            int newlineIndex = template.indexOf('\n');
            if (newlineIndex >= 0) {
                template = template.substring(newlineIndex + 1);
            }
            return template.trim();
        }

        return intentPrompt;
    }

    /**
     * 构建默认模板
     */
    private String buildDefaultTemplate() {
        return """
                请分析用户输入并返回JSON格式的意图识别结果。

                ## 对话历史
                {conversation_history}

                ## 用户输入
                {user_input}

                ## 输出格式
                请返回以下JSON格式：
                ```json
                {
                  "intent": "意图类型",
                  "confidence": 0.9,
                  "reasoning": "分类理由",
                  "suggested_action": "建议操作",
                  "resolved_question": "指代消解后的问题",
                  "translation": "英文翻译",
                  "needs_mcp_data": true,
                  "suggested_mcp_tools": ["tool1", "tool2"],
                  "route_type": "hybrid"
                }
                ```

                意图类型: backlog-diagnosis, cluster-health-check, performance-analysis, connectivity-troubleshoot, capacity-planning, topic-consultation, general
                路由类型: knowledge_only, mcp_only, hybrid, general

                如果问题涉及实时状态、数量、诊断分析，需要设置 needs_mcp_data 为 true。
                """;
    }

    /**
     * 解析 LLM 响应
     */
    private IntentResult parseResponse(String response, String originalInput) {
        if (response == null || response.isEmpty()) {
            return IntentResult.general();
        }

        try {
            // 提取 JSON 部分
            String json = extractJson(response);
            if (json == null) {
                log.warn("无法从响应中提取JSON: {}", truncate(response, 100));
                return IntentResult.general();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            String intent = (String) map.getOrDefault("intent", "general");
            double confidence = parseDouble(map.get("confidence"), 0.5);
            String reasoning = (String) map.getOrDefault("reasoning", "");
            String suggestedAction = (String) map.getOrDefault("suggested_action", "");
            String resolvedQuestion = (String) map.getOrDefault("resolved_question", originalInput);
            String translation = (String) map.getOrDefault("translation", "");

            // 解析路由相关字段
            boolean needsMcpData = parseBoolean(map.get("needs_mcp_data"), false);
            List<String> suggestedTools = parseStringList(map.get("suggested_mcp_tools"));
            RouteType routeType = parseRouteType(map.get("route_type"));

            // 验证意图是否有效
            if (!VALID_INTENTS.contains(intent)) {
                log.warn("无效的意图: {}, 使用general", intent);
                intent = "general";
            }

            // 如果 LLM 没有返回工具建议，根据意图补充
            if (needsMcpData && suggestedTools.isEmpty()) {
                suggestedTools = getSuggestedTools(intent, true);
            }

            // 如果 LLM 没有返回路由类型，根据意图补充
            if (routeType == RouteType.GENERAL_CHAT && !"general".equals(intent)) {
                routeType = determineRouteType(intent, needsMcpData);
            }

            return IntentResult.of(intent, confidence, reasoning, suggestedAction,
                    resolvedQuestion, translation, needsMcpData, suggestedTools, routeType);

        } catch (Exception e) {
            log.error("解析意图识别响应失败: {}", e.getMessage());
            return IntentResult.general();
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 尝试直接匹配 JSON 对象
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

        // 尝试提取 {} 之间的内容
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
        if (start < 0) return -1;

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

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of();
    }

    private RouteType parseRouteType(Object value) {
        if (value == null) return RouteType.GENERAL_CHAT;
        return RouteType.fromCode(value.toString());
    }

    /**
     * 获取所有有效的意图列表
     */
    public Set<String> getValidIntents() {
        return VALID_INTENTS;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}