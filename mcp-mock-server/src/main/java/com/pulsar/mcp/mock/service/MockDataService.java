package com.pulsar.mcp.mock.service;

import java.util.Map;

/**
 * Interface for mock data services.
 */
public interface MockDataService {

    /**
     * Get the tool name this service handles.
     */
    String getToolName();

    /**
     * Get tool description.
     */
    String getToolDescription();

    /**
     * Get tool parameters schema.
     */
    Map<String, Object> getParametersSchema();

    /**
     * Execute the tool with given arguments.
     */
    String execute(Map<String, Object> arguments);
}