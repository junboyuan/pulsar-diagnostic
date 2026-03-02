package com.pulsar.diagnostic.agent.agent;

import com.pulsar.diagnostic.agent.service.ChatService;
import com.pulsar.diagnostic.agent.service.DiagnosticService;
import com.pulsar.diagnostic.agent.service.InspectionService;
import com.pulsar.diagnostic.agent.skill.declarative.SkillExecutor;
import com.pulsar.diagnostic.agent.skill.declarative.SkillLoader;
import com.pulsar.diagnostic.agent.skill.declarative.SkillDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Main agent class for Pulsar diagnostics.
 * Uses declarative skill framework for task execution.
 */
@Component
public class PulsarDiagnosticAgent {

    private static final Logger log = LoggerFactory.getLogger(PulsarDiagnosticAgent.class);

    private final ChatService chatService;
    private final DiagnosticService diagnosticService;
    private final InspectionService inspectionService;
    private final SkillExecutor skillExecutor;
    private final SkillLoader skillLoader;

    public PulsarDiagnosticAgent(ChatService chatService,
                                  DiagnosticService diagnosticService,
                                  InspectionService inspectionService,
                                  SkillExecutor skillExecutor,
                                  SkillLoader skillLoader) {
        this.chatService = chatService;
        this.diagnosticService = diagnosticService;
        this.inspectionService = inspectionService;
        this.skillExecutor = skillExecutor;
        this.skillLoader = skillLoader;
    }

    /**
     * Execute a skill by name with parameters.
     * This is the preferred way to execute tasks.
     */
    public String executeSkill(String skillName, Map<String, Object> parameters) {
        log.info("Executing skill: {}", skillName);
        var result = skillExecutor.execute(skillName, parameters);
        return result.getOutputOrError();
    }

    /**
     * Execute a skill with a user query.
     */
    public String executeSkill(String skillName, String query, Map<String, Object> parameters) {
        log.info("Executing skill {} with query", skillName);
        var result = skillExecutor.execute(skillName, query, parameters);
        return result.getOutputOrError();
    }

    /**
     * Auto-select and execute the best matching skill for a query.
     */
    public String executeBestMatch(String query) {
        log.info("Finding best skill match for query");
        var result = skillExecutor.executeBestMatch(query, Map.of());
        return result.getOutputOrError();
    }

    /**
     * Get all available skills.
     */
    public List<SkillDefinition> getAvailableSkills() {
        return List.copyOf(skillLoader.getAllSkills());
    }

    /**
     * Find skills matching a query.
     */
    public List<SkillLoader.SkillMatch> findMatchingSkills(String query) {
        return skillLoader.findMatchingSkills(query);
    }

    // ==================== Legacy Methods ====================
    // These delegate to existing services for backward compatibility

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