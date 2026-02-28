package com.pulsar.diagnostic.agent.subagent;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for all available subagents.
 * Handles discovery, registration, and execution of subagents.
 */
@Component
public class SubAgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(SubAgentRegistry.class);

    private final Map<String, SubAgent> subAgents = new ConcurrentHashMap<>();
    private final McpClient mcpClient;
    private final KnowledgeBaseService knowledgeBase;

    public SubAgentRegistry(McpClient mcpClient, KnowledgeBaseService knowledgeBase) {
        this.mcpClient = mcpClient;
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Register a subagent
     */
    public void register(SubAgent subAgent) {
        subAgents.put(subAgent.getId(), subAgent);
        log.debug("Registered subagent: {} - {}", subAgent.getId(), subAgent.getName());
    }

    /**
     * Register multiple subagents
     */
    public void registerAll(List<SubAgent> agents) {
        for (SubAgent agent : agents) {
            register(agent);
        }
    }

    /**
     * Get a subagent by ID
     */
    public SubAgent getSubAgent(String id) {
        return subAgents.get(id);
    }

    /**
     * Get all registered subagents
     */
    public Collection<SubAgent> getAllSubAgents() {
        return subAgents.values();
    }

    /**
     * Check if a subagent exists
     */
    public boolean hasSubAgent(String id) {
        return subAgents.containsKey(id);
    }

    /**
     * Find subagents by capability
     */
    public List<SubAgent> findByCapability(String capability) {
        return subAgents.values().stream()
                .filter(agent -> agent.getCapabilities().contains(capability))
                .collect(Collectors.toList());
    }

    /**
     * Find subagents that can handle a task
     */
    public List<SubAgentMatch> findMatchingAgents(String taskType, Map<String, Object> parameters) {
        List<SubAgentMatch> matches = new ArrayList<>();

        for (SubAgent agent : subAgents.values()) {
            double score = agent.canHandle(taskType, parameters);
            if (score > 0.0) {
                matches.add(new SubAgentMatch(agent, score));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return matches;
    }

    /**
     * Find the best matching subagent for a task
     */
    public SubAgent findBestMatch(String taskType, Map<String, Object> parameters) {
        List<SubAgentMatch> matches = findMatchingAgents(taskType, parameters);
        return matches.isEmpty() ? null : matches.get(0).agent();
    }

    /**
     * Execute a subagent by ID
     */
    public SubAgentResult execute(String subAgentId, Map<String, Object> parameters, String parentSkillName) {
        SubAgent agent = subAgents.get(subAgentId);
        if (agent == null) {
            return SubAgentResult.builder()
                    .error("SubAgent not found: " + subAgentId)
                    .build();
        }

        // Validate parameters
        SubAgent.ValidationResult validation = agent.validateParameters(parameters);
        if (!validation.isValid()) {
            return SubAgentResult.builder()
                    .error(validation.errorMessage())
                    .build();
        }

        // Create context
        SubAgentContext context = createSubAgentContext(parameters, parentSkillName);

        // Execute
        log.info("Executing subagent: {} for parent skill: {}", subAgentId, parentSkillName);
        long startTime = System.currentTimeMillis();

        try {
            SubAgentResult result = agent.execute(context);
            long duration = System.currentTimeMillis() - startTime;

            return SubAgentResult.builder()
                    .success(result.isSuccess())
                    .subAgentId(subAgentId)
                    .output(result.getOutput())
                    .error(result.getError())
                    .findings(result.getFindings())
                    .data(result.getData())
                    .duration(java.time.Duration.ofMillis(duration))
                    .executionLog(context.getLog())
                    .build();

        } catch (Exception e) {
            log.error("Subagent execution failed: {}", subAgentId, e);
            return SubAgentResult.builder()
                    .subAgentId(subAgentId)
                    .error("Execution failed: " + e.getMessage())
                    .executionLog(context.getLog())
                    .build();
        }
    }

    /**
     * Execute multiple subagents in parallel
     */
    public Map<String, SubAgentResult> executeParallel(List<String> subAgentIds,
                                                        Map<String, Object> parameters,
                                                        String parentSkillName) {
        log.info("Executing {} subagents in parallel", subAgentIds.size());

        Map<String, SubAgentResult> results = new ConcurrentHashMap<>();

        List<java.util.concurrent.CompletableFuture<Void>> futures = subAgentIds.stream()
                .map(id -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    SubAgentResult result = execute(id, parameters, parentSkillName);
                    results.put(id, result);
                }))
                .toList();

        // Wait for all to complete
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

        return results;
    }

    /**
     * Create a subagent context
     */
    public SubAgentContext createSubAgentContext(Map<String, Object> parameters, String parentSkillName) {
        return SubAgentContext.builder()
                .executionId(UUID.randomUUID().toString())
                .parentSkillName(parentSkillName)
                .parameters(parameters)
                .mcpClient(mcpClient)
                .knowledgeBase(knowledgeBase)
                .build();
    }

    /**
     * Get available capabilities across all subagents
     */
    public Set<String> getAvailableCapabilities() {
        return subAgents.values().stream()
                .flatMap(agent -> agent.getCapabilities().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Record for subagent match results
     */
    public record SubAgentMatch(SubAgent agent, double score) {}
}