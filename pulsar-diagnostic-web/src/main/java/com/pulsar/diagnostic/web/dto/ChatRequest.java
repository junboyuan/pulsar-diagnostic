package com.pulsar.diagnostic.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for chat endpoint
 */
public record ChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 10000, message = "Message must not exceed 10000 characters")
        String message,

        @Size(max = 50, message = "Conversation history cannot exceed 50 messages")
        List<@Size(max = 5000, message = "Each history message must not exceed 5000 characters") String> history
) {
    public ChatRequest {
        if (message == null) {
            message = "";
        }
    }
}