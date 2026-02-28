package com.pulsar.diagnostic.common.model;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a Pulsar Broker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerInfo {

    private String brokerId;

    private String brokerUrl;

    private String webServiceUrl;

    private HealthStatus healthStatus;

    private String version;

    private long startTime;

    private LocalDateTime lastHeartbeat;

    private List<String> namespaces;

    private List<String> topics;

    private BrokerMetrics metrics;

    private Map<String, String> dynamicConfiguration;

    private Map<String, Object> loadReport;

    /**
     * Broker metrics snapshot
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrokerMetrics {
        private double cpuUsage;
        private double memoryUsage;
        private double directMemoryUsage;
        private long openFileDescriptors;
        private long totalConnections;
        private long producers;
        private long consumers;
        private double messagesInRate;
        private double messagesOutRate;
        private double throughputIn;
        private double throughputOut;
        private long backlogSize;
    }
}