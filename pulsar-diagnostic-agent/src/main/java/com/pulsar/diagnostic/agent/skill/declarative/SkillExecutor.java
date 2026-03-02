package com.pulsar.diagnostic.agent.skill.declarative;

import com.pulsar.diagnostic.agent.tool.*;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executes skills using Spring AI's Function Calling capability.
 * The LLM automatically decides which tools to call based on the skill definition.
 */
@Component
public class SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);

    private final ChatClient chatClient;
    private final SkillLoader skillLoader;
    private final KnowledgeBaseService knowledgeBaseService;

    // Tool beans - injected for function calling
    private final ClusterStatusTool clusterStatusTool;
    private final TopicInfoTool topicInfoTool;
    private final BrokerMetricsTool brokerMetricsTool;
    private final LogAnalysisTool logAnalysisTool;
    private final DiagnosticTool diagnosticTool;
    private final InspectionTool inspectionTool;

    public SkillExecutor(ChatClient chatClient,
                         SkillLoader skillLoader,
                         KnowledgeBaseService knowledgeBaseService,
                         ClusterStatusTool clusterStatusTool,
                         TopicInfoTool topicInfoTool,
                         BrokerMetricsTool brokerMetricsTool,
                         LogAnalysisTool logAnalysisTool,
                         DiagnosticTool diagnosticTool,
                         InspectionTool inspectionTool) {
        this.chatClient = chatClient;
        this.skillLoader = skillLoader;
        this.knowledgeBaseService = knowledgeBaseService;
        this.clusterStatusTool = clusterStatusTool;
        this.topicInfoTool = topicInfoTool;
        this.brokerMetricsTool = brokerMetricsTool;
        this.logAnalysisTool = logAnalysisTool;
        this.diagnosticTool = diagnosticTool;
        this.inspectionTool = inspectionTool;
    }

    /**
     * Execute a skill by name with the given parameters.
     * Uses Spring AI's Function Calling to let the LLM decide which tools to invoke.
     */
    public SkillExecutionResult execute(String skillName, Map<String, Object> parameters) {
        return execute(skillName, null, parameters);
    }

    /**
     * Execute a skill with a user query.
     */
    public SkillExecutionResult execute(String skillName, String userQuery, Map<String, Object> parameters) {
        var skillOpt = skillLoader.getSkill(skillName);
        if (skillOpt.isEmpty()) {
            return SkillExecutionResult.failure("Skill not found: " + skillName);
        }

        return execute(skillOpt.get(), userQuery, parameters);
    }

    /**
     * Execute a skill definition with the given parameters.
     */
    public SkillExecutionResult execute(SkillDefinition skill, String userQuery, Map<String, Object> parameters) {
        log.info("Executing skill: {}", skill.name());

        long startTime = System.currentTimeMillis();

        try {
            // Build the system prompt with skill context
            String systemPrompt = buildSystemPrompt(skill, parameters);

            // Build the user prompt
            String userPrompt = buildUserPrompt(skill, userQuery, parameters);

            // Get relevant knowledge for context
            String knowledgeContext = getKnowledgeContext(skill, userQuery);

            // Execute using ChatClient with function calling
            String response = executeWithFunctionCalling(systemPrompt, userPrompt, knowledgeContext, skill);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Skill {} completed in {}ms", skill.name(), duration);

            return SkillExecutionResult.success(skill.name(), response, duration);

        } catch (Exception e) {
            log.error("Skill execution failed: {}", skill.name(), e);
            return SkillExecutionResult.failure("Skill execution failed: " + e.getMessage());
        }
    }

    /**
     * Build the system prompt from skill definition.
     */
    private String buildSystemPrompt(SkillDefinition skill, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();

        sb.append(skill.systemPrompt());

        // Add parameter context
        if (parameters != null && !parameters.isEmpty()) {
            sb.append("\n\n## Parameters\n");
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                sb.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }

        // Add available tools hint
        if (skill.availableTools() != null && !skill.availableTools().isEmpty()) {
            sb.append("\n\n## Available Tools\n");
            sb.append("You have access to the following tools. Call them as needed:\n");
            for (String tool : skill.availableTools()) {
                sb.append(String.format("- %s\n", tool));
            }
            sb.append("\nUse these tools to gather information before providing your analysis.\n");
        }

        return sb.toString();
    }

    /**
     * Build the user prompt.
     */
    private String buildUserPrompt(SkillDefinition skill, String userQuery, Map<String, Object> parameters) {
        if (userQuery != null && !userQuery.isEmpty()) {
            return userQuery;
        }

        // Generate default prompt from skill
        return String.format("Please perform the %s task. Use the available tools to gather information.",
                skill.name().replace("-", " "));
    }

    /**
     * Get relevant knowledge from the knowledge base.
     */
    private String getKnowledgeContext(SkillDefinition skill, String query) {
        if (!knowledgeBaseService.isReady()) {
            return null;
        }

        String searchQuery = query != null ? query : skill.description();
        try {
            var context = knowledgeBaseService.searchWithContext(searchQuery, 3);
            if (context != null && context.context() != null && !context.context().isEmpty()) {
                return "\n\n## Relevant Knowledge\n" + context.context();
            }
        } catch (Exception e) {
            log.debug("Could not get knowledge context: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Execute using Spring AI's Function Calling.
     * The LLM will automatically decide which tools to call based on the prompt.
     */
    private String executeWithFunctionCalling(String systemPrompt, String userPrompt,
                                                String knowledgeContext, SkillDefinition skill) {
        // Build the full prompt
        StringBuilder fullPrompt = new StringBuilder();

        if (knowledgeContext != null) {
            fullPrompt.append(knowledgeContext).append("\n\n");
        }

        fullPrompt.append(userPrompt);

        // Use ChatClient with advisors for function calling
        // Spring AI will automatically inject tool functions
        return chatClient.prompt()
                .system(systemPrompt)
                .user(fullPrompt.toString())
                .call()
                .content();
    }

    /**
     * Execute a skill based on user query (auto-select best matching skill).
     */
    public SkillExecutionResult executeBestMatch(String query, Map<String, Object> parameters) {
        var matchOpt = skillLoader.findBestMatch(query);

        if (matchOpt.isEmpty()) {
            // No matching skill - use general diagnostic
            log.info("No specific skill matched, using general diagnosis");
            return execute("cluster_health_check", query, parameters);
        }

        SkillDefinition skill = matchOpt.get();
        log.info("Matched skill '{}' with confidence for query: {}",
                skill.name(), query.substring(0, Math.min(50, query.length())));

        return execute(skill, query, parameters);
    }

    // ==================== Tool Access Methods ====================
    // These methods are called by Spring AI's Function Calling mechanism

    public String getClusterInfo() {
        return clusterStatusTool.getClusterInfo();
    }

    public String performHealthCheck() {
        return clusterStatusTool.performHealthCheck();
    }

    public String getActiveBrokers() {
        return clusterStatusTool.getActiveBrokers();
    }

    public String getBookies() {
        return clusterStatusTool.getBookies();
    }

    public String getTopicInfo(String topicName) {
        return topicInfoTool.getTopicInfo(topicName);
    }

    public String getTopicStats(String topicName) {
        return topicInfoTool.getTopicStats(topicName);
    }

    public String getTopicSubscriptions(String topicName) {
        return topicInfoTool.getTopicSubscriptions(topicName);
    }

    public String checkTopicBacklog(String topicName) {
        return topicInfoTool.checkTopicBacklog(topicName);
    }

    public String getClusterMetrics() {
        return brokerMetricsTool.getClusterMetrics();
    }

    public String getBrokerMetrics() {
        return brokerMetricsTool.getBrokerMetrics();
    }

    public String queryMetric(String query) {
        return brokerMetricsTool.queryMetric(query);
    }

    public String getAllMetrics() {
        return brokerMetricsTool.getAllMetrics();
    }

    public String analyzeBrokerLogs(Integer maxLines) {
        return logAnalysisTool.analyzeBrokerLogs(maxLines);
    }

    public String diagnoseBacklogIssue(String resource, String resourceType) {
        return diagnosticTool.diagnoseBacklogIssue(resource, resourceType);
    }

    public String diagnoseConnectionIssues() {
        return diagnosticTool.diagnoseConnectionIssues();
    }

    public String diagnosePerformanceIssues() {
        return diagnosticTool.diagnosePerformanceIssues();
    }

    public String performFullInspection() {
        return inspectionTool.performFullInspection();
    }

    public String performInspection(String focusArea) {
        return inspectionTool.performInspection(focusArea);
    }
}