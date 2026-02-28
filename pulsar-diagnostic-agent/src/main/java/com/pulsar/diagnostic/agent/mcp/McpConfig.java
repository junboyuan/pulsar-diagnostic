package com.pulsar.diagnostic.agent.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for MCP server connection.
 */
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpConfig {

    /**
     * MCP server URL (HTTP bridge endpoint)
     */
    private String serverUrl = "http://localhost:8000";

    /**
     * Connection timeout
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Whether MCP integration is enabled
     */
    private boolean enabled = true;

    /**
     * Maximum retries on failure
     */
    private int maxRetries = 3;

    /**
     * Retry delay in milliseconds
     */
    private long retryDelayMs = 1000;

    // Getters and Setters
    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
}