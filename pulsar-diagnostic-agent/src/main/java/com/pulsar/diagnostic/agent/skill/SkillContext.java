package com.pulsar.diagnostic.agent.skill;

import java.util.Map;

/**
 * Context for skill execution.
 * Provides access to tools, knowledge base, and MCP client.
 */
public class SkillContext {

    private final McpSkillClient mcpClient;
    private final KnowledgeSkillClient knowledgeClient;
    private final ToolSkillClient toolClient;
    private final Map<String, Object> parameters;
    private final StringBuilder executionLog;

    public SkillContext(McpSkillClient mcpClient,
                        KnowledgeSkillClient knowledgeClient,
                        ToolSkillClient toolClient,
                        Map<String, Object> parameters) {
        this.mcpClient = mcpClient;
        this.knowledgeClient = knowledgeClient;
        this.toolClient = toolClient;
        this.parameters = parameters;
        this.executionLog = new StringBuilder();
    }

    /**
     * Get MCP client for calling MCP server tools
     */
    public McpSkillClient mcp() {
        return mcpClient;
    }

    /**
     * Get knowledge client for querying knowledge base
     */
    public KnowledgeSkillClient knowledge() {
        return knowledgeClient;
    }

    /**
     * Get tool client for direct tool access
     */
    public ToolSkillClient tools() {
        return toolClient;
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
     * Get all parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Log an execution step
     */
    public void log(String message) {
        executionLog.append(message).append("\n");
    }

    /**
     * Get execution log
     */
    public String getExecutionLog() {
        return executionLog.toString();
    }

    /**
     * Create a builder for SkillContext
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private McpSkillClient mcpClient;
        private KnowledgeSkillClient knowledgeClient;
        private ToolSkillClient toolClient;
        private Map<String, Object> parameters = Map.of();

        public Builder mcpClient(McpSkillClient mcpClient) {
            this.mcpClient = mcpClient;
            return this;
        }

        public Builder knowledgeClient(KnowledgeSkillClient knowledgeClient) {
            this.knowledgeClient = knowledgeClient;
            return this;
        }

        public Builder toolClient(ToolSkillClient toolClient) {
            this.toolClient = toolClient;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public SkillContext build() {
            return new SkillContext(mcpClient, knowledgeClient, toolClient, parameters);
        }
    }
}