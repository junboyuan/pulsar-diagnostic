package com.pulsar.diagnostic.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for inspection endpoint
 */
public record InspectionRequest(
        @Size(max = 1000, message = "Focus areas must not exceed 1000 characters")
        String focusAreas
) {
    public InspectionRequest {
        // focusAreas is optional
    }
}