package com.pulsar.diagnostic.agent.tool;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.common.model.PulsarCluster;
import com.pulsar.diagnostic.core.admin.PulsarAdminClient;
import com.pulsar.diagnostic.core.health.ClusterHealth;
import com.pulsar.diagnostic.core.health.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for checking cluster status and health.
 * Uses MCP server for cluster inspection.
 */
@Component
public class ClusterStatusTool {

    private static final Logger log = LoggerFactory.getLogger(ClusterStatusTool.class);

    private final McpClient mcpClient;
    private final PulsarAdminClient pulsarAdminClient;
    private final HealthCheckService healthCheckService;

    public ClusterStatusTool(McpClient mcpClient,
                             PulsarAdminClient pulsarAdminClient,
                             HealthCheckService healthCheckService) {
        this.mcpClient = mcpClient;
        this.pulsarAdminClient = pulsarAdminClient;
        this.healthCheckService = healthCheckService;
    }

    /**
     * Get overall Pulsar cluster information including brokers, bookies, and health status
     */
    public String getClusterInfo() {
        log.info("Tool: Getting cluster info via MCP");
        try {
            // Use MCP inspect_cluster tool
            return mcpClient.callToolSync("inspect_cluster",
                    Map.of("components", List.of("all")));
        } catch (Exception e) {
            log.error("Failed to get cluster info via MCP, falling back to direct call", e);
            // Fallback to direct admin client
            try {
                PulsarCluster cluster = pulsarAdminClient.getClusterInfo();
                return formatClusterInfo(cluster);
            } catch (Exception ex) {
                return "Error getting cluster info: " + ex.getMessage();
            }
        }
    }

    /**
     * Perform a comprehensive health check on the Pulsar cluster
     */
    public String performHealthCheck() {
        log.info("Tool: Performing health check");
        try {
            ClusterHealth health = healthCheckService.performHealthCheck();
            return formatHealthCheck(health);
        } catch (Exception e) {
            log.error("Failed to perform health check", e);
            return "Error performing health check: " + e.getMessage();
        }
    }

    /**
     * Quick check if the Pulsar cluster is healthy
     */
    public String isClusterHealthy() {
        log.info("Tool: Quick health check");
        try {
            boolean healthy = healthCheckService.isClusterHealthy();
            return healthy ? "Cluster is HEALTHY" : "Cluster has ISSUES - please run detailed health check";
        } catch (Exception e) {
            return "Error checking cluster health: " + e.getMessage();
        }
    }

    /**
     * Get list of all active brokers in the cluster
     */
    public String getActiveBrokers() {
        log.info("Tool: Getting active brokers via MCP");
        try {
            return mcpClient.callToolSync("inspect_cluster",
                    Map.of("components", List.of("brokers")));
        } catch (Exception e) {
            log.error("Failed to get brokers via MCP, falling back", e);
            try {
                var brokers = pulsarAdminClient.getBrokers();
                if (brokers.isEmpty()) {
                    return "No active brokers found in the cluster";
                }
                StringBuilder sb = new StringBuilder("Active Brokers:\n");
                for (var broker : brokers) {
                    sb.append(String.format("- %s (Status: %s)\n",
                            broker.getBrokerId(),
                            broker.getHealthStatus()));
                }
                return sb.toString();
            } catch (Exception ex) {
                return "Error getting brokers: " + ex.getMessage();
            }
        }
    }

    /**
     * Get list of all bookies (BookKeeper nodes) in the cluster
     */
    public String getBookies() {
        log.info("Tool: Getting bookies via MCP");
        try {
            return mcpClient.callToolSync("inspect_cluster",
                    Map.of("components", List.of("bookies")));
        } catch (Exception e) {
            log.error("Failed to get bookies via MCP, falling back", e);
            try {
                var bookies = pulsarAdminClient.getBookies();
                if (bookies.isEmpty()) {
                    return "No bookies found in the cluster";
                }

                StringBuilder sb = new StringBuilder("Bookies:\n");
                for (var bookie : bookies) {
                    sb.append(String.format("- %s (Status: %s, Read-only: %s)\n",
                            bookie.getBookieId(),
                            bookie.getHealthStatus(),
                            bookie.isReadOnly()));
                }
                return sb.toString();
            } catch (Exception ex) {
                return "Error getting bookies: " + ex.getMessage();
            }
        }
    }

    private String formatClusterInfo(PulsarCluster cluster) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Pulsar Cluster Information ===\n");
        sb.append(String.format("Cluster Name: %s\n", cluster.getClusterName()));
        sb.append(String.format("Admin URL: %s\n", cluster.getAdminUrl()));
        sb.append(String.format("Broker URL: %s\n", cluster.getBrokerServiceUrl()));
        sb.append(String.format("Overall Health: %s\n", cluster.getHealthStatus()));

        if (cluster.getBrokers() != null) {
            sb.append(String.format("\nBrokers: %d active\n", cluster.getBrokers().size()));
        }

        if (cluster.getBookies() != null) {
            sb.append(String.format("Bookies: %d total\n", cluster.getBookies().size()));
        }

        if (cluster.getStats() != null) {
            var stats = cluster.getStats();
            sb.append("\nCluster Statistics:\n");
            sb.append(String.format("- Tenants: %d\n", stats.getTotalTenants()));
            sb.append(String.format("- Namespaces: %d\n", stats.getTotalNamespaces()));
        }

        return sb.toString();
    }

    private String formatHealthCheck(ClusterHealth health) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Cluster Health Check ===\n");
        sb.append(String.format("Overall Status: %s\n", health.getStatus()));
        sb.append(String.format("Summary: %s\n", health.getSummary()));
        sb.append(String.format("Check Time: %s\n", health.getCheckTime()));

        if (health.getComponents() != null && !health.getComponents().isEmpty()) {
            sb.append("\nComponent Details:\n");
            for (var component : health.getComponents()) {
                sb.append(String.format("- %s [%s]: %s\n",
                        component.getType(),
                        component.getId(),
                        component.getStatus()));
                if (component.getMessage() != null) {
                    sb.append(String.format("  Message: %s\n", component.getMessage()));
                }
            }
        }

        return sb.toString();
    }
}