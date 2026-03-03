package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.intent.IntentRecognizer;
import com.pulsar.diagnostic.agent.intent.IntentResult;
import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.agent.skill.declarative.SkillExecutor;
import com.pulsar.diagnostic.agent.skill.declarative.SkillExecutionResult;
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
import java.util.Map;

/**
 * 聊天服务
 *
 * 处理用户聊天消息，包含意图识别和技能路由
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptTemplates promptTemplates;
    private final IntentRecognizer intentRecognizer;
    private final SkillExecutor skillExecutor;

    public ChatService(ChatClient chatClient,
                       KnowledgeBaseService knowledgeBaseService,
                       PromptTemplates promptTemplates,
                       IntentRecognizer intentRecognizer,
                       SkillExecutor skillExecutor) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptTemplates = promptTemplates;
        this.intentRecognizer = intentRecognizer;
        this.skillExecutor = skillExecutor;
    }

    /**
     * 发送聊天消息并获取响应
     * 流程：意图识别 -> 技能匹配 -> 执行处理 -> 返回结果
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 发送聊天消息（带对话历史）
     */
    public String chat(String userMessage, List<String> conversationHistory) {
        log.info("处理聊天消息: {}", userMessage.substring(0, Math.min(50, userMessage.length())));

        try {
            // 1. 意图识别
            IntentResult intent = intentRecognizer.recognize(userMessage);
            log.info("识别到意图: {} (置信度: {}, 理由: {})",
                    intent.intent(), intent.confidence(), intent.reasoning());

            // 2. 根据意图路由处理
            if (intent.isGeneral()) {
                // 通用对话
                return handleGeneralChat(userMessage, conversationHistory);
            } else {
                // 调用特定技能处理
                return handleSkillExecution(userMessage, intent);
            }

        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            return "处理请求时遇到错误：" + e.getMessage();
        }
    }

    /**
     * 处理通用对话
     */
    private String handleGeneralChat(String userMessage, List<String> conversationHistory) {
        log.info("处理通用对话");

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
     * 处理技能执行
     */
    private String handleSkillExecution(String userMessage, IntentResult intent) {
        log.info("执行技能: {}", intent.intent());

        try {
            // 执行匹配的技能
            SkillExecutionResult result = skillExecutor.execute(
                    intent.intent(),
                    userMessage,
                    Map.of()
            );

            if (result.success()) {
                return formatSkillResponse(intent, result);
            } else {
                return "执行技能时遇到问题：" + result.error() +
                       "\n\n让我尝试用通用方式帮您分析...";
            }

        } catch (Exception e) {
            log.error("技能执行失败: {}", intent.intent(), e);
            // 降级到通用对话
            return "技能执行遇到问题，让我尝试用其他方式帮您...\n\n" +
                   handleGeneralChat(userMessage, null);
        }
    }

    /**
     * 格式化技能响应
     */
    private String formatSkillResponse(IntentResult intent, SkillExecutionResult result) {
        StringBuilder sb = new StringBuilder();

        // 添加意图识别信息（可选，用于调试）
        sb.append("## 诊断结果\n\n");
        sb.append("**识别意图**: ").append(getIntentDisplayName(intent.intent())).append("\n");
        sb.append("**置信度**: ").append(String.format("%.0f%%", intent.confidence() * 100)).append("\n");
        sb.append("**分析理由**: ").append(intent.reasoning()).append("\n\n");

        // 添加技能输出
        sb.append("---\n\n");
        sb.append(result.output());

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
            default -> intent;
        };
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
        log.info("流式处理聊天消息: {}", userMessage.substring(0, Math.min(50, userMessage.length())));

        try {
            // 意图识别
            IntentResult intent = intentRecognizer.recognize(userMessage);
            log.info("流式模式 - 识别到意图: {}", intent.intent());

            if (intent.isGeneral()) {
                return streamGeneralChat(userMessage, conversationHistory);
            } else {
                // 技能执行返回完整结果，转换为流
                String result = handleSkillExecution(userMessage, intent);
                return Flux.just(result);
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
}