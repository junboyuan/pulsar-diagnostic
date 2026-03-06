package com.pulsar.mcp.mock.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.mcp.mock.data.MockDataGenerator;
import com.pulsar.mcp.mock.service.MockDataService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock service for broker metrics.
 */
@Service
public class BrokerMetricsMockService implements MockDataService {

    private final MockDataGenerator dataGenerator;
    private final ObjectMapper objectMapper;

    public BrokerMetricsMockService(MockDataGenerator dataGenerator, ObjectMapper objectMapper) {
        this.dataGenerator = dataGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getToolName() {
        return "get_broker_metrics";
    }

    @Override
    public String getToolDescription() {
        return "Get real-time broker performance metrics including CPU, memory, and message rates";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "brokerId", Map.of("type", "string", "description", "Broker ID (optional)")
        ));
        return schema;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            Map<String, Object> result = dataGenerator.generateBrokerMetrics();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}