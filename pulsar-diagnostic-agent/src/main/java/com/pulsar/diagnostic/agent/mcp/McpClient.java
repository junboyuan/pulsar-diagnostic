package com.pulsar.diagnostic.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for calling MCP server tools.
 */
@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final McpConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public McpClient(McpConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getServerUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Check if MCP integration is enabled and available.
     */
    public boolean isAvailable() {
        if (!config.isEnabled()) {
            return false;
        }
        try {
            String result = callTool("inspect_cluster", Map.of("components", List.of("brokers")))
                    .block(Duration.ofSeconds(5));
            return result != null && !result.contains("Error");
        } catch (Exception e) {
            log.debug("MCP server not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Call an MCP tool with the given arguments.
     *
     * @param toolName  Name of the MCP tool to call
     * @param arguments Arguments to pass to the tool
     * @return Mono containing the tool result as a string
     */
    public Mono<String> callTool(String toolName, Map<String, Object> arguments) {
        if (!config.isEnabled()) {
            return Mono.just("MCP integration is disabled");
        }

        log.debug("Calling MCP tool: {} with arguments: {}", toolName, arguments);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("arguments", arguments != null ? arguments : Collections.emptyMap());

        return webClient.post()
                .uri("/api/tools/{toolName}", toolName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(config.getTimeout())
                .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                        .filter(throwable -> {
                            log.warn("Retrying MCP call for {} due to: {}", toolName, throwable.getMessage());
                            return true;
                        }))
                .map(this::parseToolResponse)
                .onErrorResume(e -> {
                    log.error("Failed to call MCP tool {}: {}", toolName, e.getMessage());
                    return Mono.just(String.format("Error calling MCP tool %s: %s", toolName, e.getMessage()));
                });
    }

    /**
     * Call an MCP tool synchronously.
     */
    public String callToolSync(String toolName, Map<String, Object> arguments) {
        return callTool(toolName, arguments).block(config.getTimeout());
    }

    /**
     * Get list of available tools from MCP server.
     */
    public Mono<List<McpToolInfo>> getAvailableTools() {
        return webClient.get()
                .uri("/api/tools")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(this::parseToolsList)
                .onErrorResume(e -> {
                    log.error("Failed to get available tools: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    private String parseToolResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.has("result")) {
                return root.get("result").asText();
            } else if (root.has("error")) {
                return "Error: " + root.get("error").asText();
            }
            return response;
        } catch (Exception e) {
            log.debug("Could not parse MCP response as JSON, returning raw response");
            return response;
        }
    }

    private List<McpToolInfo> parseToolsList(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode tools = root.get("tools");
            if (tools == null || !tools.isArray()) {
                return Collections.emptyList();
            }

            return objectMapper.readerForListOf(McpToolInfo.class).readValue(tools);
        } catch (Exception e) {
            log.error("Failed to parse tools list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Tool information from MCP server.
     */
    public static class McpToolInfo {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}