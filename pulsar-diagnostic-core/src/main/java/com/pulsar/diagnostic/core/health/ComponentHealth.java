package com.pulsar.diagnostic.core.health;

import com.pulsar.diagnostic.common.enums.ComponentType;
import com.pulsar.diagnostic.common.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents health status of a component
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentHealth {

    private ComponentType type;

    private String id;

    private HealthStatus status;

    private String message;

    private LocalDateTime lastChecked;

    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    private List<String> issues;

    /**
     * Add a metric
     */
    public void addMetric(String name, Object value) {
        if (metrics == null) {
            metrics = new HashMap<>();
        }
        metrics.put(name, value);
    }

    /**
     * Check if component is healthy
     */
    public boolean isHealthy() {
        return status == HealthStatus.HEALTHY;
    }

    /**
     * Check if component has issues
     */
    public boolean hasIssues() {
        return status == HealthStatus.WARNING || status == HealthStatus.CRITICAL;
    }
}