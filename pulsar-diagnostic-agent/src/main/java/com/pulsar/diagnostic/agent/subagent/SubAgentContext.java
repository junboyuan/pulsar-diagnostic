package com.pulsar.diagnostic.agent.subagent;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;

import java.time.Instant;
import java.util.Map;

/**
 * Context for subagent execution.
 * Provides access to external resources and tracks execution state.
 */
public class SubAgentContext {

    private final String executionId;
    private final String parentSkillName;
    private final Map<String, Object> parameters;
    private final McpClient mcpClient;
    private final KnowledgeBaseService knowledgeBase;
    private final Instant startTime;
    private final StringBuilder logBuilder;

    public SubAgentContext(String executionId,
                           String parentSkillName,
                           Map<String, Object> parameters,
                           McpClient mcpClient,
                           KnowledgeBaseService knowledgeBase) {
        this.executionId = executionId;
        this.parentSkillName = parentSkillName;
        this.parameters = parameters;
        this.mcpClient = mcpClient;
        this.knowledgeBase = knowledgeBase;
        this.startTime = Instant.now();
        this.logBuilder = new StringBuilder();
    }

    /**
     * Get the unique execution ID
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * Get the parent skill that initiated this subagent
     */
    public String getParentSkillName() {
        return parentSkillName;
    }

    /**
     * Get execution parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Get a parameter value
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name) {
        return (T) parameters.get(name);
    }

    /**
     * Get a parameter value with default
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name, T defaultValue) {
        Object value = parameters.get(name);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Get MCP client for tool calls
     */
    public McpClient getMcpClient() {
        return mcpClient;
    }

    /**
     * Get knowledge base service
     */
    public KnowledgeBaseService getKnowledgeBase() {
        return knowledgeBase;
    }

    /**
     * Get execution start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Log a message for debugging
     */
    public void log(String message) {
        logBuilder.append("[").append(Instant.now()).append("] ").append(message).append("\n");
    }

    /**
     * Get the execution log
     */
    public String getLog() {
        return logBuilder.toString();
    }

    /**
     * Create a builder for SubAgentContext
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String executionId;
        private String parentSkillName;
        private Map<String, Object> parameters = Map.of();
        private McpClient mcpClient;
        private KnowledgeBaseService knowledgeBase;

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder parentSkillName(String parentSkillName) {
            this.parentSkillName = parentSkillName;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder mcpClient(McpClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        public Builder knowledgeBase(KnowledgeBaseService knowledgeBase) {
            this.knowledgeBase = knowledgeBase;
            return this;
        }

        public SubAgentContext build() {
            return new SubAgentContext(
                    executionId != null ? executionId : java.util.UUID.randomUUID().toString(),
                    parentSkillName,
                    parameters,
                    mcpClient,
                    knowledgeBase
            );
        }
    }
}