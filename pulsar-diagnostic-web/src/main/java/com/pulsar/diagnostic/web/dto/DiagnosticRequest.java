package com.pulsar.diagnostic.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for diagnostic endpoint
 */
public record DiagnosticRequest(
        @NotBlank(message = "Issue description is required")
        @Size(max = 5000, message = "Issue description must not exceed 5000 characters")
        String issue,

        @Size(max = 20, message = "Cannot have more than 20 symptoms")
        List<@Size(max = 1000, message = "Each symptom must not exceed 1000 characters") String> symptoms,

        @Size(max = 100, message = "Component type must not exceed 100 characters")
        String componentType,

        @Size(max = 500, message = "Component ID must not exceed 500 characters")
        String componentId
) {
    public DiagnosticRequest {
        if (issue == null && symptoms == null) {
            throw new IllegalArgumentException("Either issue or symptoms must be provided");
        }
    }
}