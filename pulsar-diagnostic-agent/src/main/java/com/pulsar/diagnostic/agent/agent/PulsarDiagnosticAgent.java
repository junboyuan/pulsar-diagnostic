package com.pulsar.diagnostic.agent.agent;

import com.pulsar.diagnostic.agent.service.ChatService;
import com.pulsar.diagnostic.agent.service.DiagnosticService;
import com.pulsar.diagnostic.agent.service.InspectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Main agent class for Pulsar diagnostics.
 * Uses Spring AI ChatClient with SkillsTool for intelligent task execution.
 * SkillsTool is registered with ChatClient in AIConfig, enabling automatic tool calling.
 */
@Component
public class PulsarDiagnosticAgent {

    private static final Logger log = LoggerFactory.getLogger(PulsarDiagnosticAgent.class);

    private final ChatService chatService;
    private final DiagnosticService diagnosticService;
    private final InspectionService inspectionService;

    public PulsarDiagnosticAgent(ChatService chatService,
                                  DiagnosticService diagnosticService,
                                  InspectionService inspectionService) {
        this.chatService = chatService;
        this.diagnosticService = diagnosticService;
        this.inspectionService = inspectionService;
    }

    // ==================== Chat Methods ====================

    /**
     * Chat with the agent
     */
    public String chat(String message) {
        log.info("Agent received chat request");
        return chatService.chat(message);
    }

    /**
     * Chat with conversation history
     */
    public String chat(String message, List<String> history) {
        return chatService.chat(message, history);
    }

    /**
     * Stream chat response
     */
    public Flux<String> chatStream(String message) {
        return chatService.chatStream(message);
    }

    // ==================== Diagnostic Methods ====================

    /**
     * Diagnose an issue
     */
    public String diagnose(String issue) {
        log.info("Agent received diagnostic request");
        return diagnosticService.diagnose(issue);
    }

    /**
     * Analyze symptoms
     */
    public String analyzeSymptoms(List<String> symptoms) {
        return diagnosticService.analyzeSymptoms(symptoms);
    }

    /**
     * Get recommendations for a component
     */
    public String getRecommendations(String componentType, String componentId) {
        return diagnosticService.getRecommendations(componentType, componentId);
    }

    // ==================== Inspection Methods ====================

    /**
     * Perform full cluster inspection
     */
    public String inspect() {
        log.info("Agent received inspection request");
        return inspectionService.performInspection();
    }

    /**
     * Perform focused inspection
     */
    public String inspect(String focusAreas) {
        return inspectionService.performInspection(focusAreas);
    }

    /**
     * Quick health check
     */
    public String quickHealthCheck() {
        return inspectionService.quickHealthCheck();
    }

    /**
     * Generate maintenance checklist
     */
    public String generateMaintenanceChecklist() {
        return inspectionService.generateMaintenanceChecklist();
    }
}