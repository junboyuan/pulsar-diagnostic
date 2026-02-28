package com.pulsar.diagnostic.core.health;

import com.pulsar.diagnostic.common.enums.ComponentType;
import com.pulsar.diagnostic.common.enums.HealthStatus;
import com.pulsar.diagnostic.common.model.BrokerInfo;
import com.pulsar.diagnostic.common.model.BookieInfo;
import com.pulsar.diagnostic.common.model.PulsarCluster;
import com.pulsar.diagnostic.core.admin.PulsarAdminClient;
import com.pulsar.diagnostic.core.metrics.PrometheusMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for performing health checks on Pulsar cluster
 */
@Component
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final PulsarAdminClient pulsarAdminClient;
    private final PrometheusMetricsCollector metricsCollector;

    // Cached health status
    private final Map<String, ComponentHealth> componentHealthMap = new ConcurrentHashMap<>();
    private volatile LocalDateTime lastCheckTime;

    public HealthCheckService(PulsarAdminClient pulsarAdminClient,
                              PrometheusMetricsCollector metricsCollector) {
        this.pulsarAdminClient = pulsarAdminClient;
        this.metricsCollector = metricsCollector;
    }

    /**
     * Perform comprehensive health check
     */
    public ClusterHealth performHealthCheck() {
        log.info("Performing health check...");
        List<ComponentHealth> components = new ArrayList<>();

        // Check brokers
        components.addAll(checkBrokers());

        // Check bookies
        components.addAll(checkBookies());

        // Check ZooKeeper (via admin API)
        components.add(checkZooKeeper());

        // Determine overall status
        HealthStatus overallStatus = determineOverallStatus(components);

        lastCheckTime = LocalDateTime.now();

        return ClusterHealth.builder()
                .status(overallStatus)
                .components(components)
                .checkTime(lastCheckTime)
                .summary(generateSummary(components))
                .build();
    }

    /**
     * Check broker health
     */
    public List<ComponentHealth> checkBrokers() {
        List<ComponentHealth> results = new ArrayList<>();

        try {
            List<BrokerInfo> brokers = pulsarAdminClient.getBrokers();

            for (BrokerInfo broker : brokers) {
                ComponentHealth health = ComponentHealth.builder()
                        .type(ComponentType.BROKER)
                        .id(broker.getBrokerId())
                        .status(broker.getHealthStatus())
                        .message(broker.getHealthStatus().getDescription())
                        .lastChecked(LocalDateTime.now())
                        .build();

                // Add metrics if available
                if (broker.getMetrics() != null) {
                    health.addMetric("connections", broker.getMetrics().getTotalConnections());
                    health.addMetric("producers", broker.getMetrics().getProducers());
                    health.addMetric("consumers", broker.getMetrics().getConsumers());
                }

                results.add(health);
                componentHealthMap.put("broker:" + broker.getBrokerId(), health);
            }

        } catch (Exception e) {
            log.error("Failed to check broker health", e);
            results.add(ComponentHealth.builder()
                    .type(ComponentType.BROKER)
                    .id("cluster")
                    .status(HealthStatus.UNKNOWN)
                    .message("Failed to check: " + e.getMessage())
                    .lastChecked(LocalDateTime.now())
                    .build());
        }

        return results;
    }

    /**
     * Check bookie health
     */
    public List<ComponentHealth> checkBookies() {
        List<ComponentHealth> results = new ArrayList<>();

        try {
            List<BookieInfo> bookies = pulsarAdminClient.getBookies();

            for (BookieInfo bookie : bookies) {
                ComponentHealth health = ComponentHealth.builder()
                        .type(ComponentType.BOOKIE)
                        .id(bookie.getBookieId())
                        .status(bookie.getHealthStatus())
                        .message(bookie.isReadOnly() ? "Read-only mode" : "Healthy")
                        .lastChecked(LocalDateTime.now())
                        .build();

                health.addMetric("diskUsagePercent", bookie.getDiskUsagePercent());
                health.addMetric("ledgerCount", bookie.getLedgerCount());

                results.add(health);
                componentHealthMap.put("bookie:" + bookie.getBookieId(), health);
            }

        } catch (Exception e) {
            log.error("Failed to check bookie health", e);
            results.add(ComponentHealth.builder()
                    .type(ComponentType.BOOKIE)
                    .id("cluster")
                    .status(HealthStatus.UNKNOWN)
                    .message("Failed to check: " + e.getMessage())
                    .lastChecked(LocalDateTime.now())
                    .build());
        }

        return results;
    }

    /**
     * Check ZooKeeper health (via admin API)
     */
    public ComponentHealth checkZooKeeper() {
        try {
            // If we can get cluster info, ZooKeeper is healthy
            PulsarCluster cluster = pulsarAdminClient.getClusterInfo();

            return ComponentHealth.builder()
                    .type(ComponentType.ZOOKEEPER)
                    .id("cluster")
                    .status(HealthStatus.HEALTHY)
                    .message("ZooKeeper ensemble is healthy")
                    .lastChecked(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to check ZooKeeper health", e);
            return ComponentHealth.builder()
                    .type(ComponentType.ZOOKEEPER)
                    .id("cluster")
                    .status(HealthStatus.CRITICAL)
                    .message("Failed to connect: " + e.getMessage())
                    .lastChecked(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Quick health check for heartbeat
     */
    public boolean isClusterHealthy() {
        try {
            HealthStatus status = pulsarAdminClient.checkHealth();
            return status == HealthStatus.HEALTHY;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get cached component health
     */
    public ComponentHealth getCachedHealth(String componentId) {
        return componentHealthMap.get(componentId);
    }

    /**
     * Get all cached health data
     */
    public Map<String, ComponentHealth> getAllCachedHealth() {
        return new ConcurrentHashMap<>(componentHealthMap);
    }

    /**
     * Get last check time
     */
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    /**
     * Determine overall cluster health from components
     */
    private HealthStatus determineOverallStatus(List<ComponentHealth> components) {
        if (components.isEmpty()) {
            return HealthStatus.UNKNOWN;
        }

        boolean hasCritical = false;
        boolean hasWarning = false;

        for (ComponentHealth health : components) {
            if (health.getStatus() == HealthStatus.CRITICAL) {
                hasCritical = true;
            } else if (health.getStatus() == HealthStatus.WARNING) {
                hasWarning = true;
            }
        }

        if (hasCritical) {
            return HealthStatus.CRITICAL;
        } else if (hasWarning) {
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * Generate health summary
     */
    private String generateSummary(List<ComponentHealth> components) {
        long healthy = components.stream()
                .filter(c -> c.getStatus() == HealthStatus.HEALTHY).count();
        long warning = components.stream()
                .filter(c -> c.getStatus() == HealthStatus.WARNING).count();
        long critical = components.stream()
                .filter(c -> c.getStatus() == HealthStatus.CRITICAL).count();

        return String.format("Total: %d, Healthy: %d, Warning: %d, Critical: %d",
                components.size(), healthy, warning, critical);
    }

    /**
     * Scheduled health check (every 30 seconds)
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledHealthCheck() {
        try {
            performHealthCheck();
        } catch (Exception e) {
            log.error("Scheduled health check failed", e);
        }
    }
}