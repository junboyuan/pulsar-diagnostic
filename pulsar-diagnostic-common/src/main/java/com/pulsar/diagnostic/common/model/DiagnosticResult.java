package com.pulsar.diagnostic.common.model;

import com.pulsar.diagnostic.common.enums.DiagnosticType;
import com.pulsar.diagnostic.common.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a diagnostic result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticResult {

    private String id;

    private DiagnosticType type;

    private Severity severity;

    private String title;

    private String description;

    private String component;

    private String affectedResource;

    private List<String> symptoms;

    private List<String> possibleCauses;

    private List<String> recommendations;

    private List<String> relatedMetrics;

    private Map<String, Object> metadata;

    private LocalDateTime detectedAt;

    private boolean resolved;

    private LocalDateTime resolvedAt;

    /**
     * Create a simple diagnostic result
     */
    public static DiagnosticResult of(DiagnosticType type, Severity severity,
                                      String title, String description) {
        return DiagnosticResult.builder()
                .type(type)
                .severity(severity)
                .title(title)
                .description(description)
                .detectedAt(LocalDateTime.now())
                .build();
    }
}