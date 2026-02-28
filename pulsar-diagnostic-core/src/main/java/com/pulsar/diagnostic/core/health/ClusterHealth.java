package com.pulsar.diagnostic.core.health;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents overall cluster health
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterHealth {

    private HealthStatus status;

    private List<ComponentHealth> components;

    private LocalDateTime checkTime;

    private String summary;

    /**
     * Get healthy component count
     */
    public long getHealthyCount() {
        if (components == null) return 0;
        return components.stream()
                .filter(ComponentHealth::isHealthy)
                .count();
    }

    /**
     * Get unhealthy component count
     */
    public long getUnhealthyCount() {
        if (components == null) return 0;
        return components.stream()
                .filter(ComponentHealth::hasIssues)
                .count();
    }

    /**
     * Check if cluster is healthy
     */
    public boolean isHealthy() {
        return status == HealthStatus.HEALTHY;
    }
}