package com.pulsar.diagnostic.agent.service;

import com.pulsar.diagnostic.agent.dto.QAResponse;
import com.pulsar.diagnostic.agent.prompt.PromptTemplates;
import com.pulsar.diagnostic.knowledge.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * KnowledgeQAService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeQAServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private KnowledgeBaseService.KnowledgeContext knowledgeContext;

    private PromptTemplates promptTemplates;
    private KnowledgeQAService knowledgeQAService;

    @BeforeEach
    void setUp() {
        promptTemplates = new PromptTemplates();
        promptTemplates.loadPrompts();
        knowledgeQAService = new KnowledgeQAService(chatClient, knowledgeBaseService, promptTemplates);
    }

    @Test
    @DisplayName("Should return useful response when LLM returns valid JSON")
    void ask_shouldReturnUsefulResponse() {
        // Given
        String userMessage = "Topic积压怎么排查？";
        String llmResponse = """
                ```json
                {
                  "useful": true,
                  "content": "Topic积压排查步骤：1. 检查消费者状态；2. 查看积压量；3. 分析消费速率。",
                  "translation": "Topic backlog troubleshooting steps: 1. Check consumer status; 2. View backlog amount; 3. Analyze consumption rate."
                }
                ```
                """;

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertTrue(response.useful());
        assertTrue(response.content().contains("Topic积压排查"));
        assertTrue(response.translation().contains("backlog troubleshooting"));
    }

    @Test
    @DisplayName("Should return not useful response when knowledge base cannot answer")
    void ask_shouldReturnNotUsefulResponse() {
        // Given
        String userMessage = "今天天气怎么样？";
        String llmResponse = """
                {
                  "useful": false,
                  "content": "根据现有知识库无法回答此问题，建议查阅Pulsar官方文档或提供更多上下文。",
                  "translation": "Unable to answer this question based on the current knowledge base."
                }
                """;

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertFalse(response.useful());
        assertTrue(response.content().contains("无法回答"));
    }

    @Test
    @DisplayName("Should retrieve knowledge when knowledge base is ready")
    void ask_shouldRetrieveKnowledgeWhenReady() {
        // Given
        String userMessage = "如何配置消息保留策略？";
        String knowledgeContent = "消息保留策略可以通过 retentionTimeInMinutes 和 retentionSizeInMB 配置。";
        String llmResponse = """
                {
                  "useful": true,
                  "content": "消息保留策略可配置时间和大小两个维度。",
                  "translation": "Message retention policy can be configured in time and size dimensions."
                }
                """;

        when(knowledgeBaseService.isReady()).thenReturn(true);
        when(knowledgeBaseService.searchWithContext(any(String.class), anyInt()))
                .thenReturn(knowledgeContext);
        when(knowledgeContext.items()).thenReturn(List.of(
                new KnowledgeBaseService.KnowledgeItem("1", knowledgeContent, null)
        ));
        when(knowledgeContext.context()).thenReturn(knowledgeContent);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertTrue(response.useful());
        verify(knowledgeBaseService).searchWithContext(userMessage, 5);
    }

    @Test
    @DisplayName("Should handle conversation history correctly")
    void ask_shouldHandleConversationHistory() {
        // Given
        String userMessage = "它怎么解决？";
        List<String> history = List.of(
                "我的topic消息积压了",
                "已了解，让我帮你分析积压原因"
        );
        String llmResponse = """
                {
                  "useful": true,
                  "content": "根据上下文，您问的是topic积压的解决方案。",
                  "translation": "Based on context, you are asking about topic backlog solutions."
                }
                """;

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage, history);

        // Then
        assertTrue(response.useful());
        // 验证 user 方法被调用（对话历史会被格式化到 prompt 中）
        verify(requestSpec).user(any(String.class));
    }

    @Test
    @DisplayName("Should return unknown response when LLM call fails")
    void ask_shouldReturnUnknownWhenException() {
        // Given
        String userMessage = "测试问题";

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("LLM call failed"));

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertFalse(response.useful());
        assertTrue(response.content().contains("无法回答"));
    }

    @Test
    @DisplayName("Should return unknown response when LLM returns null")
    void ask_shouldReturnUnknownWhenNullResponse() {
        // Given
        String userMessage = "测试问题";

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(null);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertFalse(response.useful());
    }

    @Test
    @DisplayName("Should handle JSON without code block")
    void ask_shouldHandleJsonWithoutCodeBlock() {
        // Given
        String userMessage = "测试问题";
        String llmResponse = "这是回复内容：{\"useful\": true, \"content\": \"测试回答\", \"translation\": \"test answer\"} 结束";

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertTrue(response.useful());
        assertEquals("测试回答", response.content());
        assertEquals("test answer", response.translation());
    }

    @Test
    @DisplayName("Should handle complex nested JSON")
    void ask_shouldHandleNestedJson() {
        // Given
        String userMessage = "测试问题";
        String llmResponse = """
                {
                  "useful": true,
                  "content": "包含嵌套引号的内容：\\"引号内容\\"",
                  "translation": "Content with nested quotes"
                }
                """;

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertTrue(response.useful());
        assertTrue(response.content().contains("引号内容"));
    }

    @Test
    @DisplayName("Should return raw response when JSON extraction fails")
    void ask_shouldReturnRawResponseWhenNoJson() {
        // Given
        String userMessage = "测试问题";
        String llmResponse = "这是一段没有JSON格式的纯文本回复。";

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        // 当无法提取JSON时，服务会返回 unknown() 响应（useful=false）
        // 因为提示词要求LLM必须返回JSON格式
        assertFalse(response.useful());
    }

    @Test
    @DisplayName("Should handle empty conversation history")
    void ask_shouldHandleEmptyHistory() {
        // Given
        String userMessage = "测试问题";
        String llmResponse = "{\"useful\": true, \"content\": \"回答\", \"translation\": \"answer\"}";

        when(knowledgeBaseService.isReady()).thenReturn(false);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage, List.of());

        // Then
        assertTrue(response.useful());
    }

    @Test
    @DisplayName("Should handle knowledge base retrieval failure gracefully")
    void ask_shouldHandleKnowledgeRetrievalFailure() {
        // Given
        String userMessage = "测试问题";
        String llmResponse = "{\"useful\": true, \"content\": \"回答\", \"translation\": \"answer\"}";

        when(knowledgeBaseService.isReady()).thenReturn(true);
        when(knowledgeBaseService.searchWithContext(any(), anyInt()))
                .thenThrow(new RuntimeException("Knowledge retrieval failed"));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // When
        QAResponse response = knowledgeQAService.ask(userMessage);

        // Then
        assertTrue(response.useful()); // 应该继续处理，只是没有知识上下文
    }
}