package com.pulsar.mcp.mock.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.mcp.mock.data.MockDataGenerator;
import com.pulsar.mcp.mock.service.MockDataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock service for listing topics.
 */
@Service
public class ListTopicsMockService implements MockDataService {

    private final MockDataGenerator dataGenerator;
    private final ObjectMapper objectMapper;

    public ListTopicsMockService(MockDataGenerator dataGenerator, ObjectMapper objectMapper) {
        this.dataGenerator = dataGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getToolName() {
        return "list_topics";
    }

    @Override
    public String getToolDescription() {
        return "List all topics in a namespace";
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
            List<String> topics = dataGenerator.generateTopicList(namespace);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("namespace", namespace != null ? namespace : "public/default");
            result.put("topics", topics);
            result.put("totalCount", topics.size());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}