package com.pulsar.mcp.mock.model;

/**
 * MCP tool response model.
 */
public class McpToolResponse {

    private String result;
    private String error;

    public McpToolResponse() {
    }

    public McpToolResponse(String result) {
        this.result = result;
    }

    public static McpToolResponse success(String result) {
        return new McpToolResponse(result);
    }

    public static McpToolResponse error(String error) {
        McpToolResponse response = new McpToolResponse();
        response.setError(error);
        return response;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}