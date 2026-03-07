package com.pulsar.diagnostic.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.agent.mcp.McpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for AI components and MCP integration.
 * Tools are registered via ToolConfig's ToolCallback beans.
 */
@Configuration
public class AIConfig {

    private static final Logger log = LoggerFactory.getLogger(AIConfig.class);

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:gpt-4}")
    private String chatModel;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-ada-002}")
    private String embeddingModel;

    /**
     * Create ObjectMapper bean for JSON processing.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Create OpenAI API with custom base URL for DashScope compatibility.
     */
    @Bean
    @Primary
    public OpenAiApi openAiApi() {
        log.info("Creating OpenAiApi with baseUrl={}, apiKey={}...", baseUrl, apiKey.substring(0, Math.min(10, apiKey.length())));
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    /**
     * Create ChatModel with custom OpenAI API.
     */
    @Bean
    @Primary
    public ChatModel chatModel(OpenAiApi openAiApi) {
        log.info("Creating OpenAiChatModel with model={}", chatModel);
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .model(chatModel)
                        .temperature(0.7)
                        .build())
                .build();
    }

    /**
     * Create EmbeddingModel with custom OpenAI API.
     */
    @Bean
    @Primary
    public OpenAiEmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        log.info("Creating OpenAiEmbeddingModel with model={}", embeddingModel);
        return new OpenAiEmbeddingModel(openAiApi,
                org.springframework.ai.document.MetadataMode.EMBED,
                org.springframework.ai.openai.OpenAiEmbeddingOptions.builder()
                        .model(embeddingModel)
                        .build());
    }

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