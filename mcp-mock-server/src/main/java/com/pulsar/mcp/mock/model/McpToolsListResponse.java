package com.pulsar.mcp.mock.model;

import java.util.List;

/**
 * MCP tools list response model.
 */
public class McpToolsListResponse {

    private List<McpToolInfo> tools;

    public McpToolsListResponse() {
    }

    public McpToolsListResponse(List<McpToolInfo> tools) {
        this.tools = tools;
    }

    public List<McpToolInfo> getTools() {
        return tools;
    }

    public void setTools(List<McpToolInfo> tools) {
        this.tools = tools;
    }
}