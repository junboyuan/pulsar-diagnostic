package com.pulsar.diagnostic.web.dto;

import java.util.List;

/**
 * Request DTO for chat endpoint
 */
public record ChatRequest(
        String message,
        List<String> history
) {
    public ChatRequest {
        // Ensure non-null
        if (message == null) {
            message = "";
        }
    }
}