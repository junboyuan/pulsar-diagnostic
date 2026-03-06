package com.pulsar.mcp.mock.model;

import java.util.Map;

/**
 * MCP tool request model.
 */
public class McpToolRequest {

    private Map<String, Object> arguments;

    public McpToolRequest() {
    }

    public McpToolRequest(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}