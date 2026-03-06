package com.pulsar.mcp.mock.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.mcp.mock.data.MockDataGenerator;
import com.pulsar.mcp.mock.service.MockDataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock service for permissions queries.
 */
@Service
public class PermissionsMockService implements MockDataService {

    private final MockDataGenerator dataGenerator;
    private final ObjectMapper objectMapper;

    public PermissionsMockService(MockDataGenerator dataGenerator, ObjectMapper objectMapper) {
        this.dataGenerator = dataGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getToolName() {
        return "get_permissions";
    }

    @Override
    public String getToolDescription() {
        return "Get namespace and topic permissions information";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "namespace", Map.of("type", "string", "description", "Namespace (e.g., public/default)")
        ));
        return schema;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            String namespace = arguments != null ? (String) arguments.get("namespace") : null;
            Map<String, Object> result = dataGenerator.generatePermissions(namespace);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}