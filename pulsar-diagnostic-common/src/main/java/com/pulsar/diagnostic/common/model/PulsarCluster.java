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
 * Represents a Pulsar cluster
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PulsarCluster {

    private String clusterName;

    private String serviceUrl;

    private String adminUrl;

    private String brokerServiceUrl;

    private HealthStatus healthStatus;

    private List<BrokerInfo> brokers;

    private List<BookieInfo> bookies;

    private ClusterStats stats;

    private LocalDateTime lastUpdated;

    private Map<String, String> properties;

    /**
     * Cluster statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterStats {
        private long totalTopics;
        private long totalNamespaces;
        private long totalTenants;
        private long totalConnections;
        private double messagesInRate;
        private double messagesOutRate;
        private double throughputIn;
        private double throughputOut;
        private long totalBacklogSize;
        private int activeBrokers;
        private int totalBookies;
        private int availableBookies;
    }
}