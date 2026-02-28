package com.pulsar.diagnostic.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Pulsar connection
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pulsar")
public class PulsarConfig {

    /**
     * Pulsar Admin URL (e.g., http://localhost:8080)
     */
    private String adminUrl = "http://localhost:8080";

    /**
     * Pulsar Broker URL (e.g., pulsar://localhost:6650)
     */
    private String brokerUrl = "pulsar://localhost:6650";

    /**
     * Prometheus URL for metrics (e.g., http://localhost:9090)
     */
    private String prometheusUrl = "http://localhost:9090";

    /**
     * Log file paths configuration
     */
    private LogPaths logPaths = new LogPaths();

    /**
     * Connection timeout in seconds
     */
    private int connectionTimeoutSeconds = 30;

    /**
     * Request timeout in seconds
     */
    private int requestTimeoutSeconds = 60;

    /**
     * Enable TLS
     */
    private boolean tlsEnabled = false;

    /**
     * TLS certificate path
     */
    private String tlsCertificatePath;

    /**
     * Authentication token
     */
    private String authToken;

    /**
     * Cluster name
     */
    private String clusterName = "standalone";

    /**
     * Log paths configuration
     */
    @Data
    public static class LogPaths {
        private String broker = "/var/log/pulsar/broker";
        private String bookie = "/var/log/pulsar/bookkeeper";
        private String zookeeper = "/var/log/pulsar/zookeeper";
        private String proxy = "/var/log/pulsar/proxy";
    }

    /**
     * Get all configured log paths as a map
     */
    public Map<String, String> getAllLogPaths() {
        Map<String, String> paths = new HashMap<>();
        if (logPaths.getBroker() != null) {
            paths.put("broker", logPaths.getBroker());
        }
        if (logPaths.getBookie() != null) {
            paths.put("bookie", logPaths.getBookie());
        }
        if (logPaths.getZookeeper() != null) {
            paths.put("zookeeper", logPaths.getZookeeper());
        }
        if (logPaths.getProxy() != null) {
            paths.put("proxy", logPaths.getProxy());
        }
        return paths;
    }
}