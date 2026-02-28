package com.pulsar.diagnostic.agent.skill;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import com.pulsar.diagnostic.agent.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all available skills.
 * Handles skill discovery, registration, and execution.
 */
@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    private final McpClient mcpClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ClusterStatusTool clusterStatusTool;
    private final TopicInfoTool topicInfoTool;
    private final BrokerMetricsTool brokerMetricsTool;
    private final LogAnalysisTool logAnalysisTool;
    private final DiagnosticTool diagnosticTool;
    private final InspectionTool inspectionTool;

    public SkillRegistry(McpClient mcpClient,
                         KnowledgeBaseService knowledgeBaseService,
                         ClusterStatusTool clusterStatusTool,
                         TopicInfoTool topicInfoTool,
                         BrokerMetricsTool brokerMetricsTool,
                         LogAnalysisTool logAnalysisTool,
                         DiagnosticTool diagnosticTool,
                         InspectionTool inspectionTool) {
        this.mcpClient = mcpClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.clusterStatusTool = clusterStatusTool;
        this.topicInfoTool = topicInfoTool;
        this.brokerMetricsTool = brokerMetricsTool;
        this.logAnalysisTool = logAnalysisTool;
        this.diagnosticTool = diagnosticTool;
        this.inspectionTool = inspectionTool;
    }

    /**
     * Register a skill
     */
    public void register(Skill skill) {
        skills.put(skill.getName(), skill);
        log.debug("Registered skill: {} - {}", skill.getName(), skill.getDescription());
    }

    /**
     * Register multiple skills
     */
    public void registerAll(List<Skill> skillList) {
        for (Skill skill : skillList) {
            register(skill);
        }
    }

    /**
     * Get a skill by name
     */
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * Get all registered skills
     */
    public Collection<Skill> getAllSkills() {
        return skills.values();
    }

    /**
     * Check if a skill exists
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }

    /**
     * Find the best matching skill for a query
     * @param query The user query
     * @return The best matching skill, or null if no match
     */
    public Skill findBestMatch(String query) {
        Skill bestMatch = null;
        double bestScore = 0.0;

        for (Skill skill : skills.values()) {
            double score = skill.canHandle(query);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = skill;
            }
        }

        // Only return if confidence is above threshold
        return bestScore > 0.5 ? bestMatch : null;
    }

    /**
     * Find all skills that can handle a query (sorted by confidence)
     */
    public List<SkillMatch> findMatchingSkills(String query) {
        List<SkillMatch> matches = new ArrayList<>();

        for (Skill skill : skills.values()) {
            double score = skill.canHandle(query);
            if (score > 0.0) {
                matches.add(new SkillMatch(skill, score));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return matches;
    }

    /**
     * Execute a skill by name
     */
    public SkillResult execute(String skillName, Map<String, Object> parameters) {
        Skill skill = skills.get(skillName);
        if (skill == null) {
            return SkillResult.builder()
                    .error("Skill not found: " + skillName)
                    .build();
        }

        // Validate parameters
        Skill.ValidationResult validation = skill.validateParameters(parameters);
        if (!validation.isValid()) {
            return SkillResult.builder()
                    .error(validation.errorMessage())
                    .build();
        }

        // Create context
        SkillContext context = createSkillContext(parameters);

        // Execute
        log.info("Executing skill: {}", skillName);
        try {
            return skill.execute(context);
        } catch (Exception e) {
            log.error("Skill execution failed: {}", skillName, e);
            return SkillResult.builder()
                    .error("Skill execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create a skill context with all clients
     */
    public SkillContext createSkillContext(Map<String, Object> parameters) {
        return SkillContext.builder()
                .mcpClient(createMcpSkillClient())
                .knowledgeClient(createKnowledgeSkillClient())
                .toolClient(createToolSkillClient())
                .parameters(parameters)
                .build();
    }

    private McpSkillClient createMcpSkillClient() {
        return new McpSkillClient() {
            @Override
            public String callTool(String toolName, Map<String, Object> arguments) {
                return mcpClient.callToolSync(toolName, arguments);
            }
        };
    }

    private KnowledgeSkillClient createKnowledgeSkillClient() {
        return new KnowledgeSkillClient() {
            @Override
            public boolean isReady() {
                return knowledgeBaseService.isReady();
            }

            @Override
            public List<String> search(String query, int topK) {
                return knowledgeBaseService.search(query, topK);
            }

            @Override
            public KnowledgeContext searchWithContext(String query, int topK) {
                var ctx = knowledgeBaseService.searchWithContext(query, topK);
                List<KnowledgeItem> items = ctx.items().stream()
                        .map(i -> new KnowledgeItem(i.id(), i.content(), i.metadata()))
                        .toList();
                return new KnowledgeContext(ctx.query(), items, ctx.context());
            }

            @Override
            public List<String> searchByCategory(String query, String category, int topK) {
                return knowledgeBaseService.searchByCategory(query, category, topK);
            }
        };
    }

    private ToolSkillClient createToolSkillClient() {
        return new ToolSkillClient() {
            @Override
            public String getClusterInfo() {
                return clusterStatusTool.getClusterInfo();
            }

            @Override
            public String performHealthCheck() {
                return clusterStatusTool.performHealthCheck();
            }

            @Override
            public String getActiveBrokers() {
                return clusterStatusTool.getActiveBrokers();
            }

            @Override
            public String getBookies() {
                return clusterStatusTool.getBookies();
            }

            @Override
            public String getTopicInfo(String topicName) {
                return topicInfoTool.getTopicInfo(topicName);
            }

            @Override
            public String getTopicStats(String topicName) {
                return topicInfoTool.getTopicStats(topicName);
            }

            @Override
            public String listTopicsInNamespace(String namespace) {
                return topicInfoTool.listTopicsInNamespace(namespace);
            }

            @Override
            public String getTopicSubscriptions(String topicName) {
                return topicInfoTool.getTopicSubscriptions(topicName);
            }

            @Override
            public String checkTopicBacklog(String topicName) {
                return topicInfoTool.checkTopicBacklog(topicName);
            }

            @Override
            public String getClusterMetrics() {
                return brokerMetricsTool.getClusterMetrics();
            }

            @Override
            public String queryMetric(String query) {
                return brokerMetricsTool.queryMetric(query);
            }

            @Override
            public String getBrokerMetrics() {
                return brokerMetricsTool.getBrokerMetrics();
            }

            @Override
            public String getAllMetrics() {
                return brokerMetricsTool.getAllMetrics();
            }

            @Override
            public String checkMetricsAvailable() {
                return brokerMetricsTool.checkMetricsAvailable();
            }

            @Override
            public String analyzeBrokerLogs(Integer maxLines) {
                return logAnalysisTool.analyzeBrokerLogs(maxLines);
            }

            @Override
            public String analyzeBookieLogs(Integer maxLines) {
                return logAnalysisTool.analyzeBookieLogs(maxLines);
            }

            @Override
            public String searchAllLogs(String pattern, Integer maxResults) {
                return logAnalysisTool.searchAllLogs(pattern, maxResults);
            }

            @Override
            public String getRecentErrors(Integer maxErrors) {
                return logAnalysisTool.getRecentErrors(maxErrors);
            }

            @Override
            public String tailLogFile(String filePath, Integer lines) {
                return logAnalysisTool.tailLogFile(filePath, lines);
            }

            @Override
            public String diagnoseBacklogIssue(String resource, String resourceType) {
                return diagnosticTool.diagnoseBacklogIssue(resource, resourceType);
            }

            @Override
            public String diagnoseConnectionIssues() {
                return diagnosticTool.diagnoseConnectionIssues();
            }

            @Override
            public String diagnosePerformanceIssues() {
                return diagnosticTool.diagnosePerformanceIssues();
            }

            @Override
            public String runComprehensiveDiagnostic() {
                return diagnosticTool.runComprehensiveDiagnostic();
            }

            @Override
            public String performFullInspection() {
                return inspectionTool.performFullInspection();
            }

            @Override
            public String performInspection(String focusArea) {
                return inspectionTool.performInspection(focusArea);
            }

            @Override
            public String quickHealthSnapshot() {
                return inspectionTool.quickHealthSnapshot();
            }
        };
    }

    /**
     * Record for skill match results
     */
    public record SkillMatch(Skill skill, double score) {}
}