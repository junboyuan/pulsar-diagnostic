package com.pulsar.diagnostic.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a Prometheus metric
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricData {

    private String name;

    private String description;

    private String type;

    private double value;

    private Map<String, String> labels;

    private long timestamp;

    /**
     * Create a simple metric with just name and value
     */
    public static MetricData of(String name, double value) {
        return MetricData.builder()
                .name(name)
                .value(value)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Create a metric with labels
     */
    public static MetricData of(String name, double value, Map<String, String> labels) {
        return MetricData.builder()
                .name(name)
                .value(value)
                .labels(labels)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}