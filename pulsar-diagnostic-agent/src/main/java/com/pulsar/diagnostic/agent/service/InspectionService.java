package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for cluster inspection operations
 */
@Service
public class InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptTemplates promptTemplates;

    public InspectionService(ChatClient chatClient,
                             KnowledgeBaseService knowledgeBaseService,
                             PromptTemplates promptTemplates) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptTemplates = promptTemplates;
    }

    /**
     * Perform a comprehensive cluster inspection
     */
    public String performInspection() {
        return performInspection(null);
    }

    /**
     * Perform inspection focused on specific areas
     */
    public String performInspection(String focusAreas) {
        log.info("Performing inspection with focus: {}", focusAreas);

        try {
            String systemPrompt = promptTemplates.INSPECTION_SYSTEM_PROMPT;

            String userPrompt;
            if (focusAreas != null && !focusAreas.isEmpty()) {
                userPrompt = String.format("""
                    Perform a cluster inspection with focus on: %s

                    Use the available tools to:
                    1. Check the health of relevant components
                    2. Gather metrics and logs
                    3. Identify any issues or warnings
                    4. Provide recommendations

                    Format the report clearly with sections for each checked area.
                    """, focusAreas);
            } else {
                userPrompt = """
                    Perform a comprehensive cluster inspection.

                    Use the available tools to:
                    1. Check broker health and status
                    2. Check bookie health and status
                    3. Review topic statistics and backlog
                    4. Analyze cluster metrics
                    5. Check for errors in logs
                    6. Verify configuration

                    Provide a detailed report with:
                    - Status for each checked area
                    - Any issues or warnings found
                    - Recommendations for improvements
                    """;
            }

            // Call the model
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return response;

        } catch (Exception e) {
            log.error("Failed to perform inspection", e);
            return "Error during inspection: " + e.getMessage();
        }
    }

    /**
     * Perform quick health check
     */
    public String quickHealthCheck() {
        log.info("Performing quick health check");

        try {
            String prompt = """
                Perform a quick health check of the Pulsar cluster.

                Use the available tools to:
                1. Check if the cluster is accessible
                2. Get a summary of component status
                3. Identify any critical issues

                Provide a brief summary of the cluster health status.
                """;

            return chatClient.prompt()
                    .system(promptTemplates.INSPECTION_SYSTEM_PROMPT)
                    .user(prompt)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Failed to perform quick health check", e);
            return "Error during health check: " + e.getMessage();
        }
    }

    /**
     * Generate a maintenance checklist
     */
    public String generateMaintenanceChecklist() {
        log.info("Generating maintenance checklist");

        try {
            String prompt = """
                Based on the current cluster status, generate a maintenance checklist.

                Use the available tools to:
                1. Assess current cluster state
                2. Identify maintenance tasks needed
                3. Prioritize tasks by urgency

                Provide a checklist with:
                - Immediate tasks (critical issues)
                - Short-term tasks (warnings, optimizations)
                - Long-term tasks (best practices, improvements)
                """;

            return chatClient.prompt()
                    .system(promptTemplates.INSPECTION_SYSTEM_PROMPT)
                    .user(prompt)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Failed to generate maintenance checklist", e);
            return "Error generating checklist: " + e.getMessage();
        }
    }
}