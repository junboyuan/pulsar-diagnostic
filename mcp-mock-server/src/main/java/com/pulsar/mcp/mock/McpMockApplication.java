package com.pulsar.mcp.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MCP Mock Server Application
 *
 * Provides mock MCP endpoints for Pulsar diagnostic testing.
 */
@SpringBootApplication
@EnableScheduling
public class McpMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpMockApplication.class, args);
    }
}