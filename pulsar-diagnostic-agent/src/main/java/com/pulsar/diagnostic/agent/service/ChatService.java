package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.dto.OrchestratorResponse;
import com.pulsar.diagnostic.agent.dto.QAResponse;
import com.pulsar.diagnostic.agent.intent.IntentRecognizer;
import com.pulsar.diagnostic.agent.intent.IntentResult;
import com.pulsar.diagnostic.agent.intent.RouteType;
import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务
 *
 * 处理用户聊天消息，使用 ResponseOrchestrator 进行智能路由和综合回答
 * 支持知识库问答和 MCP 实时数据集成
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptTemplates promptTemplates;
    private final IntentRecognizer intentRecognizer;
    private final KnowledgeQAService knowledgeQAService;
    private final ResponseOrchestrator responseOrchestrator;

    public ChatService(ChatClient chatClient,
                       KnowledgeBaseService knowledgeBaseService,
                       PromptTemplates promptTemplates,
                       IntentRecognizer intentRecognizer,
                       KnowledgeQAService knowledgeQAService,
                       ResponseOrchestrator responseOrchestrator) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptTemplates = promptTemplates;
        this.intentRecognizer = intentRecognizer;
        this.knowledgeQAService = knowledgeQAService;
        this.responseOrchestrator = responseOrchestrator;
    }

    /**
     * 发送聊天消息并获取响应
     * 使用 ResponseOrchestrator 进行智能路由
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 发送聊天消息（带对话历史）
     */
    public String chat(String userMessage, List<String> conversationHistory) {
        log.info("处理聊天消息: {}", truncate(userMessage, 50));

        try {
            // 使用编排器处理
            OrchestratorResponse response = responseOrchestrator.process(userMessage, conversationHistory);
            return formatOrchestratorResponse(response);

        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            return "处理请求时遇到错误：" + e.getMessage();
        }
    }

    /**
     * 知识问答模式
     */
    public QAResponse chatQA(String userMessage, List<String> conversationHistory) {
        log.info("知识问答模式: {}", truncate(userMessage, 50));
        return knowledgeQAService.ask(userMessage, conversationHistory);
    }

    /**
     * 格式化编排器响应
     */
    private String formatOrchestratorResponse(OrchestratorResponse response) {
        StringBuilder sb = new StringBuilder();

        // 添加响应内容
        sb.append(response.content());

        // 添加路由信息（调试模式）
        if (response.routeType() != RouteType.GENERAL_CHAT) {
            sb.append("\n\n---\n\n");
            sb.append("📌 **数据来源**: ");
            switch (response.routeType()) {
                case KNOWLEDGE_ONLY -> sb.append("知识库");
                case MCP_ONLY -> sb.append("实时集群数据");
                case HYBRID -> sb.append("知识库 + 实时数据");
                case GENERAL_CHAT -> sb.append("通用对话");
            }
        }

        return sb.toString();
    }

    /**
     * 处理知识问答（保留向后兼容）
     */
    private String handleKnowledgeQA(String userMessage, List<String> conversationHistory) {
        log.info("处理知识问答");

        try {
            QAResponse response = knowledgeQAService.ask(userMessage, conversationHistory);
            return formatQAResponse(response);
        } catch (Exception e) {
            log.error("知识问答处理失败", e);
            // 降级到普通对话
            return handleGeneralChat(userMessage, conversationHistory);
        }
    }

    /**
     * 格式化知识问答响应
     */
    private String formatQAResponse(QAResponse response) {
        StringBuilder sb = new StringBuilder();

        if (response.useful()) {
            sb.append(response.content());
            if (response.translation() != null && !response.translation().isEmpty()) {
                sb.append("\n\n---\n\n**English Translation**:\n").append(response.translation());
            }
        } else {
            sb.append(response.content());
            if (response.translation() != null && !response.translation().isEmpty()) {
                sb.append("\n\n").append(response.translation());
            }
        }

        return sb.toString();
    }

    /**
     * 处理通用对话（降级方案）
     */
    private String handleGeneralChat(String userMessage, List<String> conversationHistory) {
        log.info("处理通用对话（降级模式）");

        try {
            String knowledgeContext = getKnowledgeContext(userMessage);

            String systemPrompt = promptTemplates.SYSTEM_PROMPT;
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                systemPrompt += "\n\n相关知识点：\n" + knowledgeContext;
            }

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                for (int i = 0; i < conversationHistory.size(); i++) {
                    if (i % 2 == 0) {
                        messages.add(new UserMessage(conversationHistory.get(i)));
                    } else {
                        messages.add(new org.springframework.ai.chat.messages.AssistantMessage(conversationHistory.get(i)));
                    }
                }
            }

            messages.add(new UserMessage(userMessage));

            return chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("通用对话处理失败", e);
            return "处理对话时遇到错误：" + e.getMessage();
        }
    }

    /**
     * 流式聊天响应
     */
    public Flux<String> chatStream(String userMessage) {
        return chatStream(userMessage, null);
    }

    /**
     * 流式聊天响应（带对话历史）
     */
    public Flux<String> chatStream(String userMessage, List<String> conversationHistory) {
        log.info("流式处理聊天消息: {}", truncate(userMessage, 50));

        try {
            // 意图识别
            IntentResult intent = intentRecognizer.recognize(userMessage, conversationHistory);
            log.info("流式模式 - 识别到意图: {}, routeType: {}", intent.intent(), intent.routeType());

            if (intent.isGeneral()) {
                return streamGeneralChat(userMessage, conversationHistory);
            } else {
                // 非通用意图返回完整结果（通过编排器处理）
                OrchestratorResponse response = responseOrchestrator.process(userMessage, conversationHistory);
                return Flux.just(formatOrchestratorResponse(response));
            }

        } catch (Exception e) {
            log.error("流式处理失败", e);
            return Flux.just("错误：" + e.getMessage());
        }
    }

    /**
     * 流式通用对话
     */
    private Flux<String> streamGeneralChat(String userMessage, List<String> conversationHistory) {
        try {
            String knowledgeContext = getKnowledgeContext(userMessage);

            String systemPrompt = promptTemplates.SYSTEM_PROMPT;
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                systemPrompt += "\n\n相关知识点：\n" + knowledgeContext;
            }

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                for (int i = 0; i < conversationHistory.size(); i++) {
                    if (i % 2 == 0) {
                        messages.add(new UserMessage(conversationHistory.get(i)));
                    } else {
                        messages.add(new org.springframework.ai.chat.messages.AssistantMessage(conversationHistory.get(i)));
                    }
                }
            }

            messages.add(new UserMessage(userMessage));

            return chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("流式通用对话失败", e);
            return Flux.just("错误：" + e.getMessage());
        }
    }

    /**
     * 获取相关知识上下文
     */
    private String getKnowledgeContext(String query) {
        if (!knowledgeBaseService.isReady()) {
            return null;
        }

        try {
            var context = knowledgeBaseService.searchWithContext(query, 3);
            return context.context();
        } catch (Exception e) {
            log.debug("无法获取知识上下文: {}", e.getMessage());
            return null;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}