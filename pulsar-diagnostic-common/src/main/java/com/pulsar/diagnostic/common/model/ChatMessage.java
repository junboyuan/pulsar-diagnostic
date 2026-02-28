package com.pulsar.diagnostic.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a chat conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String id;

    private String conversationId;

    private String role;

    private String content;

    private LocalDateTime timestamp;

    private List<ToolCall> toolCalls;

    private ToolResult toolResult;

    /**
     * Represents a tool call in the conversation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments;
    }

    /**
     * Represents a tool execution result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private String toolCallId;
        private String name;
        private String result;
        private boolean success;
        private String error;
    }

    /**
     * Create a user message
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an assistant message
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a system message
     */
    public static ChatMessage system(String content) {
        return ChatMessage.builder()
                .role("system")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
}