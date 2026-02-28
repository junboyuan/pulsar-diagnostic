package com.pulsar.diagnostic.common.model;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an inspection report for a Pulsar cluster
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionReport {

    private String reportId;

    private String clusterName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private HealthStatus overallStatus;

    private int totalChecks;

    private int passedChecks;

    private int warningChecks;

    private int failedChecks;

    private List<InspectionItem> items;

    private String summary;

    private List<String> recommendations;

    /**
     * Represents a single inspection check item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InspectionItem {
        private String category;
        private String name;
        private String description;
        private HealthStatus status;
        private String message;
        private String details;
        private String remediation;
    }
}