package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.dto.McpDataResult;
import com.pulsar.diagnostic.agent.dto.OrchestratorResponse;
import com.pulsar.diagnostic.agent.intent.IntentRecognizer;
import com.pulsar.diagnostic.agent.intent.IntentResult;
import com.pulsar.diagnostic.agent.intent.RouteType;
import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.rag.RAGConfig;
import com.pulsar.diagnostic.knowledge.rag.RAGResponse;
import com.pulsar.diagnostic.knowledge.rag.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 响应编排服务
 *
 * 统一协调意图识别、知识检索和 MCP 数据获取，
 * 根据路由策略生成综合响应
 */
@Service
public class ResponseOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ResponseOrchestrator.class);

    private final IntentRecognizer intentRecognizer;
    private final RAGService ragService;
    private final McpDataService mcpDataService;
    private final ChatClient chatClient;
    private final PromptTemplates promptTemplates;

    @Value("${pulsar-diagnostic.orchestration.parallel-timeout-ms:10000}")
    private long parallelTimeoutMs = 10000;

    @Value("${pulsar-diagnostic.orchestration.knowledge-timeout-ms:5000}")
    private long knowledgeTimeoutMs = 5000;

    @Value("${pulsar-diagnostic.orchestration.mcp-timeout-ms:5000}")
    private long mcpTimeoutMs = 5000;

    public ResponseOrchestrator(
            IntentRecognizer intentRecognizer,
            RAGService ragService,
            McpDataService mcpDataService,
            ChatClient chatClient,
            PromptTemplates promptTemplates) {
        this.intentRecognizer = intentRecognizer;
        this.ragService = ragService;
        this.mcpDataService = mcpDataService;
        this.chatClient = chatClient;
        this.promptTemplates = promptTemplates;
    }

    /**
     * 处理用户问题并生成综合响应
     *
     * @param query  用户查询
     * @param history 对话历史
     * @return 编排器响应
     */
    public OrchestratorResponse process(String query, List<String> history) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理请求: query='{}'", truncate(query, 50));

        try {
            // 1. 意图识别
            IntentResult intent = intentRecognizer.recognize(query, history);
            log.info("意图识别完成: intent={}, routeType={}, confidence={}",
                    intent.intent(), intent.routeType(), intent.confidence());

            // 2. 根据路由类型执行策略
            String response = executeRoute(intent, query, history);

            // 3. 构建响应
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("请求处理完成: totalTime={}ms", totalTime);

            return OrchestratorResponse.full(
                    response,
                    intent.intent(),
                    intent.routeType(),
                    null, // knowledgeContext 在内部获取
                    null, // mcpContext 在内部获取
                    intent.confidence(),
                    totalTime
            );

        } catch (Exception e) {
            log.error("处理请求失败: {}", e.getMessage(), e);
            return OrchestratorResponse.error(e.getMessage());
        }
    }

    /**
     * 处理用户问题（无历史）
     */
    public OrchestratorResponse process(String query) {
        return process(query, null);
    }

    /**
     * 根据路由类型执行相应策略
     */
    private String executeRoute(IntentResult intent, String query, List<String> history) {
        return switch (intent.routeType()) {
            case KNOWLEDGE_ONLY -> executeKnowledgeRoute(intent, query, history);
            case MCP_ONLY -> executeMcpRoute(intent, query, history);
            case HYBRID -> executeHybridRoute(intent, query, history);
            case GENERAL_CHAT -> executeGeneralRoute(intent, query, history);
        };
    }

    /**
     * 知识库路由：仅使用 RAG
     */
    private String executeKnowledgeRoute(IntentResult intent, String query, List<String> history) {
        log.debug("执行知识库路由");

        String knowledgeContext = fetchKnowledgeContext(query);
        String historyContext = formatHistory(history);

        return generateResponse(query, knowledgeContext, null, historyContext, intent);
    }

    /**
     * MCP 路由：仅使用实时数据
     */
    private String executeMcpRoute(IntentResult intent, String query, List<String> history) {
        log.debug("执行 MCP 路由");

        McpDataResult mcpData = mcpDataService.fetchMcpData(intent, query);
        String mcpContext = mcpData.hasData() ? mcpData.aggregatedContext() : null;
        String historyContext = formatHistory(history);

        return generateResponse(query, null, mcpContext, historyContext, intent);
    }

    /**
     * 混合路由：并行获取知识和 MCP 数据
     */
    private String executeHybridRoute(IntentResult intent, String query, List<String> history) {
        log.debug("执行混合路由");

        // 并行获取知识和 MCP 数据
        CompletableFuture<String> knowledgeFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return fetchKnowledgeContext(query);
            } catch (Exception e) {
                log.warn("获取知识上下文失败: {}", e.getMessage());
                return null;
            }
        });

        CompletableFuture<McpDataResult> mcpFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mcpDataService.fetchMcpData(intent, query);
            } catch (Exception e) {
                log.warn("获取 MCP 数据失败: {}", e.getMessage());
                return McpDataResult.empty();
            }
        });

        try {
            // 等待并行结果
            knowledgeFuture.get(knowledgeTimeoutMs, TimeUnit.MILLISECONDS);
            mcpFuture.get(mcpTimeoutMs, TimeUnit.MILLISECONDS);

            String knowledgeContext = knowledgeFuture.get();
            McpDataResult mcpData = mcpFuture.get();
            String mcpContext = mcpData.hasData() ? mcpData.aggregatedContext() : null;
            String historyContext = formatHistory(history);

            log.debug("混合路由数据获取完成: knowledge={}, mcp={}",
                    knowledgeContext != null, mcpContext != null);

            return generateResponse(query, knowledgeContext, mcpContext, historyContext, intent);

        } catch (Exception e) {
            log.error("混合路由执行失败: {}", e.getMessage());
            // 降级：尝试单独获取
            return executeFallbackRoute(intent, query, history);
        }
    }

    /**
     * 通用对话路由
     */
    private String executeGeneralRoute(IntentResult intent, String query, List<String> history) {
        log.debug("执行通用对话路由");

        // 尝试获取知识上下文作为辅助
        String knowledgeContext = fetchKnowledgeContext(query);
        String historyContext = formatHistory(history);

        return generateResponse(query, knowledgeContext, null, historyContext, intent);
    }

    /**
     * 降级路由：当混合路由失败时尝试单独获取
     */
    private String executeFallbackRoute(IntentResult intent, String query, List<String> history) {
        log.info("执行降级路由");

        // 优先尝试 MCP
        McpDataResult mcpData = mcpDataService.fetchMcpData(intent, query);
        if (mcpData.hasData()) {
            return generateResponse(query, null, mcpData.aggregatedContext(), formatHistory(history), intent);
        }

        // 再尝试知识库
        String knowledgeContext = fetchKnowledgeContext(query);
        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            return generateResponse(query, knowledgeContext, null, formatHistory(history), intent);
        }

        // 最后使用通用对话
        return generateResponse(query, null, null, formatHistory(history), intent);
    }

    /**
     * 获取知识上下文
     */
    private String fetchKnowledgeContext(String query) {
        if (!ragService.isReady()) {
            log.debug("RAG 服务未就绪");
            return null;
        }

        try {
            RAGResponse response = ragService.retrieve(query, RAGConfig.defaultConfig());
            if (response.hasResults()) {
                log.debug("获取知识上下文成功: length={}", response.context().length());
                return response.context();
            }
        } catch (Exception e) {
            log.warn("获取知识上下文失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 格式化对话历史
     */
    private String formatHistory(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            String role = (i % 2 == 0) ? "用户" : "助手";
            sb.append(role).append(": ").append(history.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 使用 LLM 生成响应
     */
    private String generateResponse(String query, String knowledgeContext, String mcpContext,
                                   String historyContext, IntentResult intent) {
        // 构建系统提示
        String systemPrompt = buildSystemPrompt(intent, knowledgeContext != null, mcpContext != null);

        // 构建用户提示
        String userPrompt = buildUserPrompt(query, knowledgeContext, mcpContext, historyContext, intent);

        // 调用 LLM
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM 调用失败: {}", e.getMessage());
            return "抱歉，生成回答时遇到问题：" + e.getMessage();
        }
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt(IntentResult intent, boolean hasKnowledge, boolean hasMcp) {
        StringBuilder sb = new StringBuilder();
        sb.append(promptTemplates.SYSTEM_PROMPT);

        // 添加角色说明
        sb.append("\n\n## 当前任务\n");
        sb.append("你是一个 Pulsar 诊断助手，正在处理用户的").append(getIntentDisplayName(intent.intent())).append("请求。\n");

        // 添加数据来源说明
        sb.append("\n## 可用数据来源\n");
        if (hasKnowledge) {
            sb.append("- **知识库**: 包含 Pulsar 最佳实践、配置指南和故障排除知识\n");
        }
        if (hasMcp) {
            sb.append("- **实时数据**: 来自 Pulsar 集群的实时状态和指标数据\n");
        }
        if (!hasKnowledge && !hasMcp) {
            sb.append("- 当前没有额外的数据来源，请基于你的专业知识回答\n");
        }

        return sb.toString();
    }

    /**
     * 构建用户提示
     */
    private String buildUserPrompt(String query, String knowledgeContext, String mcpContext,
                                   String historyContext, IntentResult intent) {
        StringBuilder sb = new StringBuilder();

        // 添加对话历史
        if (historyContext != null && !historyContext.isEmpty()) {
            sb.append("### 对话历史\n").append(historyContext).append("\n");
        }

        // 添加知识上下文
        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            sb.append("### 相关知识\n").append(knowledgeContext).append("\n\n");
        }

        // 添加 MCP 数据
        if (mcpContext != null && !mcpContext.isEmpty()) {
            sb.append(mcpContext).append("\n");
        }

        // 添加用户问题
        sb.append("### 用户问题\n").append(query).append("\n\n");

        // 添加指令
        sb.append("请基于");
        if (knowledgeContext != null && mcpContext != null) {
            sb.append("上述知识和实时数据");
        } else if (knowledgeContext != null) {
            sb.append("上述知识");
        } else if (mcpContext != null) {
            sb.append("上述实时数据");
        } else {
            sb.append("你的专业知识");
        }
        sb.append("，提供详细、准确的回答。");

        return sb.toString();
    }

    /**
     * 获取意图显示名称
     */
    private String getIntentDisplayName(String intent) {
        return switch (intent) {
            case "backlog-diagnosis" -> "消息积压诊断";
            case "cluster-health-check" -> "集群健康检查";
            case "performance-analysis" -> "性能分析";
            case "connectivity-troubleshoot" -> "连接故障排查";
            case "capacity-planning" -> "容量规划";
            case "topic-consultation" -> "主题咨询";
            case "disk-diagnosis" -> "磁盘诊断";
            default -> "通用";
        };
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}