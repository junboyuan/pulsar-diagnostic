package com.pulsar.diagnostic.web.dto;

/**
 * Response DTO for chat endpoint
 */
public record ChatResponse(
        String response,
        long processingTimeMs
) {
    public static ChatResponse of(String response, long processingTimeMs) {
        return new ChatResponse(response, processingTimeMs);
    }
}