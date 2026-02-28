package com.pulsar.diagnostic.web.dto;

import com.pulsar.diagnostic.common.enums.HealthStatus;

/**
 * Response DTO for system status
 */
public record SystemStatus(
        String version,
        HealthStatus clusterHealth,
        boolean prometheusAvailable,
        boolean knowledgeBaseReady,
        long uptimeMs
) {
    public static SystemStatus of(String version, HealthStatus clusterHealth,
                                   boolean prometheusAvailable, boolean knowledgeBaseReady,
                                   long uptimeMs) {
        return new SystemStatus(version, clusterHealth, prometheusAvailable,
                knowledgeBaseReady, uptimeMs);
    }
}