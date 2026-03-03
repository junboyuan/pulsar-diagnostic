package com.pulsar.diagnostic.agent.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板管理器
 *
 * 从配置文件加载提示词模板，支持外部化配置和中文描述
 */
@Component
public class PromptTemplates {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplates.class);

    private static final String PROMPT_FILE = "prompts/prompt-templates.md";
    private static final Pattern SECTION_PATTERN = Pattern.compile("^##\\s+(\\S+)\\s*\\((\\S+)\\)\\s*\\n([\\s\\S]*?)(?=^##\\s|$)", Pattern.MULTILINE);

    private final Map<String, String> prompts = new HashMap<>();

    // 默认提示词（作为后备）
    private static final String DEFAULT_SYSTEM_PROMPT = """
        你是Apache Pulsar消息流平台专家级诊断AI代理。你的职责是帮助用户处理：
        - Pulsar集群监控和健康分析
        - 排查主题、Broker、Bookie和消费者问题
        - 性能分析和优化建议
        - 配置指导和最佳实践
        - 集群检查和健康报告
        """;

    private static final String DEFAULT_DIAGNOSTIC_PROMPT = """
        你处于诊断模式。你的任务是识别并诊断Pulsar集群中的问题。

        请遵循以下步骤：
        1. 检查整体集群健康状况
        2. 识别存在问题的组件
        3. 收集与问题相关的详细指标和日志
        4. 分析根本原因
        5. 提供清晰的发现和建议
        """;

    private static final String DEFAULT_INSPECTION_PROMPT = """
        你处于检查模式。你的任务是对Pulsar集群进行全面健康检查。

        检查以下方面：
        1. Broker健康
        2. Bookie健康
        3. 主题健康
        4. 网络健康
        5. 资源健康
        6. 配置健康
        """;

    @PostConstruct
    public void loadPrompts() {
        log.info("加载提示词模板: {}", PROMPT_FILE);

        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_FILE);
            if (!resource.exists()) {
                log.warn("提示词模板文件不存在，使用默认模板");
                loadDefaultPrompts();
                return;
            }

            String content = readResourceContent(resource);
            parsePromptFile(content);

            log.info("已加载 {} 个提示词模板: {}", prompts.size(), prompts.keySet());

        } catch (IOException e) {
            log.error("加载提示词模板失败: {}", e.getMessage());
            loadDefaultPrompts();
        }
    }

    private String readResourceContent(ClassPathResource resource) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void parsePromptFile(String content) {
        Matcher matcher = SECTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String key = matcher.group(2).trim();
            String promptContent = matcher.group(3).trim();

            prompts.put(key, promptContent);
            log.debug("解析提示词: {} -> {}", title, key);
        }
    }

    private void loadDefaultPrompts() {
        prompts.put("SYSTEM_PROMPT", DEFAULT_SYSTEM_PROMPT);
        prompts.put("DIAGNOSTIC_SYSTEM_PROMPT", DEFAULT_DIAGNOSTIC_PROMPT);
        prompts.put("INSPECTION_SYSTEM_PROMPT", DEFAULT_INSPECTION_PROMPT);
    }

    /**
     * 获取系统提示词
     */
    public String getSystemPrompt() {
        return prompts.getOrDefault("SYSTEM_PROMPT", DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * 获取诊断模式提示词
     */
    public String getDiagnosticPrompt() {
        return prompts.getOrDefault("DIAGNOSTIC_SYSTEM_PROMPT", DEFAULT_DIAGNOSTIC_PROMPT);
    }

    /**
     * 获取检查模式提示词
     */
    public String getInspectionPrompt() {
        return prompts.getOrDefault("INSPECTION_SYSTEM_PROMPT", DEFAULT_INSPECTION_PROMPT);
    }

    /**
     * 获取知识上下文模板
     */
    public String getKnowledgeContextTemplate() {
        return prompts.getOrDefault("KNOWLEDGE_CONTEXT_TEMPLATE",
                "请使用以下知识：\n\n{knowledge}\n\n---\n\n基于此知识，帮助用户处理请求。");
    }

    /**
     * 生成带上下文的诊断提示词
     */
    public String generateDiagnosticPrompt(String userQuery, String knowledgeContext) {
        String basePrompt = getDiagnosticPrompt();

        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            return basePrompt + "\n\n相关知识点：\n" + knowledgeContext +
                   "\n\n用户问题：" + userQuery;
        }
        return basePrompt + "\n\n用户问题：" + userQuery;
    }

    /**
     * 生成检查提示词
     */
    public String generateInspectionPrompt(String focusAreas) {
        String basePrompt = getInspectionPrompt();

        if (focusAreas != null && !focusAreas.isEmpty()) {
            return basePrompt + "\n\n重点关注以下方面：" + focusAreas;
        }
        return basePrompt;
    }

    /**
     * 生成带知识的聊天提示词
     */
    public String generateChatPrompt(String userQuery, String knowledgeContext) {
        String basePrompt = getSystemPrompt();

        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            return basePrompt + "\n\n相关知识点：\n" + knowledgeContext +
                   "\n\n用户：" + userQuery;
        }
        return basePrompt + "\n\n用户：" + userQuery;
    }

    /**
     * 获取所有提示词模板
     */
    public Map<String, String> getAllPrompts() {
        return Map.copyOf(prompts);
    }

    /**
     * 获取指定名称的提示词
     */
    public String getPrompt(String name) {
        return prompts.get(name);
    }

    // 保持向后兼容的静态常量
    public static final String SYSTEM_PROMPT = """
        你是Apache Pulsar消息流平台专家级诊断AI代理。你的职责是帮助用户处理：
        - Pulsar集群监控和健康分析
        - 排查主题、Broker、Bookie和消费者问题
        - 性能分析和优化建议
        - 配置指导和最佳实践
        - 集群检查和健康报告

        你可以使用以下工具：
        - 查询集群状态和组件健康状况
        - 获取主题详细信息，包括积压、生产者和消费者
        - 分析Prometheus指标
        - 读取和分析各Pulsar组件的日志
        - 执行全面的集群检查

        诊断问题时请遵循以下步骤：
        1. 使用可用工具收集相关信息
        2. 系统分析数据
        3. 识别根本原因和影响因素
        4. 提供清晰的解释和可操作的建议

        请始终保持详尽但简洁。适当时使用知识库获取上下文。
        如果需要更多信息来诊断问题，请使用适当的工具收集。
        """;

    public static final String DIAGNOSTIC_SYSTEM_PROMPT = """
        你处于诊断模式。你的任务是识别并诊断Pulsar集群中的问题。

        请遵循以下步骤：
        1. 检查整体集群健康状况
        2. 识别存在问题的组件
        3. 收集与问题相关的详细指标和日志
        4. 分析根本原因
        5. 提供清晰的发现和建议

        请按以下格式回复：
        - 问题摘要：问题简述
        - 根本原因分析：导致问题的原因详细解释
        - 受影响组件：受影响的资源列表
        - 建议措施：解决问题的可操作步骤
        - 额外上下文：相关指标或日志片段
        """;

    public static final String INSPECTION_SYSTEM_PROMPT = """
        你处于检查模式。你的任务是对Pulsar集群进行全面健康检查。

        检查以下方面：
        1. Broker健康：状态、资源使用、连接数
        2. Bookie健康：磁盘使用、Ledger状态
        3. 主题健康：积压、生产者、消费者
        4. 网络健康：连接速率、错误数
        5. 资源健康：CPU、内存、磁盘使用
        6. 配置健康：策略合规性

        对每个方面，报告：
        - 状态：健康 / 警告 / 严重
        - 详情：关键指标和观察结果
        - 问题：发现的任何问题
        - 建议：建议的改进措施
        """;

    public static final String KNOWLEDGE_CONTEXT_TEMPLATE = """
        请使用以下来自Pulsar文档和最佳实践的知识：

        {knowledge}

        ---

        基于此知识和可用工具，帮助用户处理请求。
        """;
}