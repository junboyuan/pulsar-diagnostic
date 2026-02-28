package com.pulsar.diagnostic.agent.mcp;

/**
 * Exception thrown when MCP tool execution fails.
 */
public class McpToolException extends RuntimeException {

    private final String toolName;
    private final int statusCode;

    public McpToolException(String toolName, String message) {
        super(String.format("MCP tool '%s' failed: %s", toolName, message));
        this.toolName = toolName;
        this.statusCode = -1;
    }

    public McpToolException(String toolName, String message, Throwable cause) {
        super(String.format("MCP tool '%s' failed: %s", toolName, message), cause);
        this.toolName = toolName;
        this.statusCode = -1;
    }

    public McpToolException(String toolName, int statusCode, String message) {
        super(String.format("MCP tool '%s' failed with status %d: %s", toolName, statusCode, message));
        this.toolName = toolName;
        this.statusCode = statusCode;
    }

    public String getToolName() {
        return toolName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}