package com.pulsar.diagnostic.common.enums;

/**
 * Enumeration of Pulsar cluster health status
 */
public enum HealthStatus {
    HEALTHY("healthy", "All components are functioning normally"),
    WARNING("warning", "Some components have issues but cluster is operational"),
    CRITICAL("critical", "Cluster has significant issues requiring immediate attention"),
    UNKNOWN("unknown", "Unable to determine cluster health");

    private final String code;
    private final String description;

    HealthStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}