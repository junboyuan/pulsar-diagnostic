package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for chat interactions with the Pulsar Diagnostic Agent
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptTemplates promptTemplates;

    public ChatService(ChatClient chatClient,
                       KnowledgeBaseService knowledgeBaseService,
                       PromptTemplates promptTemplates) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptTemplates = promptTemplates;
    }

    /**
     * Send a chat message and get a response
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * Send a chat message with conversation history
     */
    public String chat(String userMessage, List<String> conversationHistory) {
        log.info("Processing chat message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));

        try {
            // Get relevant knowledge
            String knowledgeContext = getKnowledgeContext(userMessage);

            // Build the prompt
            String systemPrompt = promptTemplates.SYSTEM_PROMPT;
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                systemPrompt += "\n\nRelevant Knowledge:\n" + knowledgeContext;
            }

            // Create messages
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            // Add conversation history if provided
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

            // Call the chat model
            String response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            return response;

        } catch (Exception e) {
            log.error("Failed to process chat message", e);
            return "I encountered an error processing your request: " + e.getMessage();
        }
    }

    /**
     * Stream a chat response
     */
    public Flux<String> chatStream(String userMessage) {
        return chatStream(userMessage, null);
    }

    /**
     * Stream a chat response with conversation history
     */
    public Flux<String> chatStream(String userMessage, List<String> conversationHistory) {
        log.info("Streaming chat message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));

        try {
            // Get relevant knowledge
            String knowledgeContext = getKnowledgeContext(userMessage);

            // Build the prompt
            String systemPrompt = promptTemplates.SYSTEM_PROMPT;
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                systemPrompt += "\n\nRelevant Knowledge:\n" + knowledgeContext;
            }

            // Create messages
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

            // Stream the response
            return chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("Failed to stream chat message", e);
            return Flux.just("Error: " + e.getMessage());
        }
    }

    /**
     * Get relevant knowledge context for a query
     */
    private String getKnowledgeContext(String query) {
        if (!knowledgeBaseService.isReady()) {
            return null;
        }

        try {
            var context = knowledgeBaseService.searchWithContext(query, 3);
            return context.context();
        } catch (Exception e) {
            log.debug("Could not get knowledge context: {}", e.getMessage());
            return null;
        }
    }
}