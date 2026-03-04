package com.pulsar.diagnostic.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.agent.mcp.McpConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI components and MCP integration.
 * Tools are registered via ToolConfig's ToolCallback beans.
 */
@Configuration
public class AIConfig {

    /**
     * Create ChatClient bean for conversational AI.
     * Tools from ToolConfig are automatically registered by Spring AI.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Create MCP client bean for HTTP communication with MCP server
     */
    @Bean
    public McpClient mcpClient(McpConfig config, ObjectMapper objectMapper) {
        return new McpClient(config, objectMapper);
    }
}