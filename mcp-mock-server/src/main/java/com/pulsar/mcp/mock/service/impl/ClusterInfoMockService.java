package com.pulsar.mcp.mock.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.mcp.mock.data.MockDataGenerator;
import com.pulsar.mcp.mock.service.MockDataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock service for cluster information.
 */
@Service
public class ClusterInfoMockService implements MockDataService {

    private final MockDataGenerator dataGenerator;
    private final ObjectMapper objectMapper;

    public ClusterInfoMockService(MockDataGenerator dataGenerator, ObjectMapper objectMapper) {
        this.dataGenerator = dataGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getToolName() {
        return "inspect_cluster";
    }

    @Override
    public String getToolDescription() {
        return "Inspect Pulsar cluster status and components health";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("type", "array");
        components.put("items", Map.of("type", "string"));
        components.put("description", "Components to inspect: brokers, bookies, topics, namespaces");
        schema.put("properties", Map.of("components", components));
        return schema;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            @SuppressWarnings("unchecked")
            List<String> components = arguments != null && arguments.containsKey("components")
                    ? (List<String>) arguments.get("components")
                    : List.of();
            Map<String, Object> result = dataGenerator.generateClusterInspection(components);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}