package com.pulsar.mcp.mock.model;

import java.util.Map;

/**
 * MCP tool information model.
 */
public class McpToolInfo {

    private String name;
    private String description;
    private Map<String, Object> parameters;

    public McpToolInfo() {
    }

    public McpToolInfo(String name, String description, Map<String, Object> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}