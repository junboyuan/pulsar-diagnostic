package com.pulsar.mcp.mock.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.mcp.mock.data.MockDataGenerator;
import com.pulsar.mcp.mock.service.MockDataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock service for topic information queries.
 */
@Service
public class TopicInfoMockService implements MockDataService {

    private final MockDataGenerator dataGenerator;
    private final ObjectMapper objectMapper;

    public TopicInfoMockService(MockDataGenerator dataGenerator, ObjectMapper objectMapper) {
        this.dataGenerator = dataGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getToolName() {
        return "get_topic_info";
    }

    @Override
    public String getToolDescription() {
        return "Get detailed topic information including configuration and stats";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "topic", Map.of("type", "string", "description", "Topic name")
        ));
        return schema;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            String topic = arguments != null ? (String) arguments.get("topic") : null;
            Map<String, Object> result = dataGenerator.generateTopicInfo(topic);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}