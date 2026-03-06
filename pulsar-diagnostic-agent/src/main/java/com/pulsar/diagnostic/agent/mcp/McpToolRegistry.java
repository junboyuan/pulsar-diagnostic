package com.pulsar.diagnostic.agent.mcp;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for MCP tools.
 * Discovers and caches available tools from the MCP server.
 */
@Component
public class McpToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);

    private final McpClient mcpClient;
    private final Map<String, McpClient.McpToolInfo> tools = new ConcurrentHashMap<>();

    public McpToolRegistry(McpClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    @PostConstruct
    public void init() {
        refreshTools();
    }

    /**
     * Refresh the list of available tools from the MCP server.
     */
    public void refreshTools() {
        try {
            List<McpClient.McpToolInfo> toolList = mcpClient.getAvailableTools();
            if (toolList != null) {
                tools.clear();
                for (McpClient.McpToolInfo tool : toolList) {
                    tools.put(tool.getName(), tool);
                    log.debug("Registered MCP tool: {} - {}", tool.getName(), tool.getDescription());
                }
                log.info("Loaded {} MCP tools from server", tools.size());
            }
        } catch (Exception e) {
            log.warn("Could not load MCP tools from server: {}", e.getMessage());
        }
    }

    /**
     * Check if a tool is available.
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Get tool information.
     */
    public McpClient.McpToolInfo getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * Get all registered tools.
     */
    public Map<String, McpClient.McpToolInfo> getAllTools() {
        return Map.copyOf(tools);
    }

    /**
     * Call a tool by name with arguments.
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        return mcpClient.callToolSync(toolName, arguments);
    }
}