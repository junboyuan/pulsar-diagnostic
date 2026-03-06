package com.pulsar.mcp.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for mock data scenarios.
 */
@Configuration
@ConfigurationProperties(prefix = "mock")
public class MockDataConfig {

    /**
     * Scenario to simulate (normal, backlog, produce-slow, disk-full, auth-failure)
     */
    private String scenario = "normal";

    /**
     * Enable dynamic data changes
     */
    private boolean dynamicData = true;

    /**
     * Interval in seconds for dynamic data updates
     */
    private int updateIntervalSeconds = 5;

    /**
     * Custom parameters for scenarios
     */
    private Map<String, Object> parameters = new HashMap<>();

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public boolean isDynamicData() {
        return dynamicData;
    }

    public void setDynamicData(boolean dynamicData) {
        this.dynamicData = dynamicData;
    }

    public int getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public void setUpdateIntervalSeconds(int updateIntervalSeconds) {
        this.updateIntervalSeconds = updateIntervalSeconds;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}