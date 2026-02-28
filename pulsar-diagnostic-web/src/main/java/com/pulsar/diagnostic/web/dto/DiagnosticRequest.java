package com.pulsar.diagnostic.web.dto;

import java.util.List;

/**
 * Request DTO for diagnostic endpoint
 */
public record DiagnosticRequest(
        String issue,
        List<String> symptoms,
        String componentType,
        String componentId
) {
    public DiagnosticRequest {
        if (issue == null && symptoms == null) {
            throw new IllegalArgumentException("Either issue or symptoms must be provided");
        }
    }
}