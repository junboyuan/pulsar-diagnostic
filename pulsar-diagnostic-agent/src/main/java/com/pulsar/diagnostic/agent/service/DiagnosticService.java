package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for diagnostic operations
 */
@Service
public class DiagnosticService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptTemplates promptTemplates;

    public DiagnosticService(ChatClient chatClient,
                             KnowledgeBaseService knowledgeBaseService,
                             PromptTemplates promptTemplates) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptTemplates = promptTemplates;
    }

    /**
     * Diagnose a specific issue
     */
    public String diagnose(String issueDescription) {
        log.info("Running diagnosis for: {}", issueDescription);

        try {
            // Get relevant knowledge
            String knowledgeContext = getKnowledgeContext(issueDescription);

            // Build system prompt for diagnosis
            String systemPrompt = promptTemplates.DIAGNOSTIC_SYSTEM_PROMPT;

            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                systemPrompt += "\n\nRelevant Knowledge for Diagnosis:\n" + knowledgeContext;
            }

            // Create the diagnostic request
            String userPrompt = String.format("""
                Please diagnose the following issue:

                %s

                Use the available tools to gather information and provide a comprehensive diagnosis.
                """, issueDescription);

            // Call the model
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return formatDiagnosticResponse(response);

        } catch (Exception e) {
            log.error("Failed to run diagnosis", e);
            return "Error during diagnosis: " + e.getMessage();
        }
    }

    /**
     * Analyze symptoms and provide diagnosis
     */
    public String analyzeSymptoms(List<String> symptoms) {
        log.info("Analyzing {} symptoms", symptoms.size());

        try {
            String symptomList = String.join("\n- ", symptoms);

            String prompt = String.format("""
                Analyze the following symptoms and provide a diagnosis:

                Symptoms:
                - %s

                Use available tools to investigate and provide your analysis.
                """, symptomList);

            return diagnose(prompt);

        } catch (Exception e) {
            log.error("Failed to analyze symptoms", e);
            return "Error analyzing symptoms: " + e.getMessage();
        }
    }

    /**
     * Get recommendations for a specific component
     */
    public String getRecommendations(String componentType, String componentId) {
        log.info("Getting recommendations for {}: {}", componentType, componentId);

        try {
            String prompt = String.format("""
                Provide recommendations and best practices for the following Pulsar component:

                Component Type: %s
                Component ID: %s

                Check the current status and configuration, then provide specific recommendations.
                """, componentType, componentId);

            return diagnose(prompt);

        } catch (Exception e) {
            log.error("Failed to get recommendations", e);
            return "Error getting recommendations: " + e.getMessage();
        }
    }

    /**
     * Get relevant knowledge context
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

    /**
     * Format the diagnostic response
     */
    private String formatDiagnosticResponse(String response) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║                  DIAGNOSTIC REPORT                       ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n\n");
        sb.append(response);
        return sb.toString();
    }
}