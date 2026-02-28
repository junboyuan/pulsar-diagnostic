package com.pulsar.diagnostic.web.dto;

/**
 * Request DTO for inspection endpoint
 */
public record InspectionRequest(
        String focusAreas
) {
    public InspectionRequest {
        // focusAreas is optional
    }
}