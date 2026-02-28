package com.pulsar.diagnostic.agent.tool;

import com.pulsar.diagnostic.core.metrics.PrometheusMetricsCollector;
import com.pulsar.diagnostic.common.model.MetricData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for querying Prometheus metrics
 */
@Component
public class BrokerMetricsTool {

    private static final Logger log = LoggerFactory.getLogger(BrokerMetricsTool.class);

    private final PrometheusMetricsCollector metricsCollector;

    public BrokerMetricsTool(PrometheusMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Tool(description = "Get cluster-level metrics summary including throughput, backlog, and connections")
    public String getClusterMetrics() {
        log.info("Tool: Getting cluster metrics");
        try {
            PrometheusMetricsCollector.ClusterMetricsSummary summary =
                    metricsCollector.getClusterMetricsSummary();

            StringBuilder sb = new StringBuilder();
            sb.append("=== Cluster Metrics Summary ===\n");
            sb.append(String.format("Messages In Rate: %.2f msg/s\n", summary.getMessagesInRate()));
            sb.append(String.format("Messages Out Rate: %.2f msg/s\n", summary.getMessagesOutRate()));
            sb.append(String.format("Total Backlog: %d messages\n", summary.getTotalBacklog()));
            sb.append(String.format("Total Connections: %d\n", summary.getTotalConnections()));
            sb.append(String.format("Total Producers: %d\n", summary.getTotalProducers()));
            sb.append(String.format("Total Consumers: %d\n", summary.getTotalConsumers()));

            // Add analysis
            sb.append("\n=== Analysis ===\n");
            if (summary.getTotalBacklog() > 1000000) {
                sb.append("⚠️ High backlog detected - check consumer health\n");
            }
            if (summary.getMessagesInRate() < summary.getMessagesOutRate() * 0.5) {
                sb.append("ℹ️ Out rate exceeds in rate - backlog should be decreasing\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to get cluster metrics", e);
            return "Error getting cluster metrics: " + e.getMessage();
        }
    }

    @Tool(description = "Query a specific Prometheus metric")
    public String queryMetric(
            @ToolParam(description = "Prometheus query expression") String query) {
        log.info("Tool: Querying metric: {}", query);
        try {
            List<MetricData> results = metricsCollector.queryMetric(query);

            if (results.isEmpty()) {
                return "No data found for query: " + query;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Query: %s\n\n", query));

            for (MetricData metric : results) {
                sb.append(String.format("Value: %.4f\n", metric.getValue()));
                if (metric.getLabels() != null && !metric.getLabels().isEmpty()) {
                    sb.append("Labels:\n");
                    for (Map.Entry<String, String> label : metric.getLabels().entrySet()) {
                        if (!label.getKey().startsWith("__")) {
                            sb.append(String.format("  %s=%s\n", label.getKey(), label.getValue()));
                        }
                    }
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error querying metric: " + e.getMessage();
        }
    }

    @Tool(description = "Get metrics for all brokers in the cluster")
    public String getBrokerMetrics() {
        log.info("Tool: Getting broker metrics");
        try {
            Map<String, PrometheusMetricsCollector.BrokerMetrics> brokerMetrics =
                    metricsCollector.getBrokerMetrics();

            if (brokerMetrics.isEmpty()) {
                return "No broker metrics available. Check Prometheus connection.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== Broker Metrics ===\n\n");

            for (Map.Entry<String, PrometheusMetricsCollector.BrokerMetrics> entry :
                    brokerMetrics.entrySet()) {
                PrometheusMetricsCollector.BrokerMetrics metrics = entry.getValue();
                sb.append(String.format("Broker: %s\n", entry.getKey()));
                sb.append(String.format("  Connections: %d\n", metrics.getConnections()));
                sb.append(String.format("  CPU Usage: %.2f%%\n", metrics.getCpuUsage() * 100));
                sb.append(String.format("  Memory Usage: %.2f%%\n", metrics.getMemoryUsage() * 100));
                sb.append(String.format("  Producers: %d\n", metrics.getProducers()));
                sb.append(String.format("  Consumers: %d\n", metrics.getConsumers()));
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error getting broker metrics: " + e.getMessage();
        }
    }

    @Tool(description = "Get all key Pulsar metrics at once")
    public String getAllMetrics() {
        log.info("Tool: Getting all Pulsar metrics");
        try {
            Map<String, List<MetricData>> allMetrics = metricsCollector.getAllPulsarMetrics();

            StringBuilder sb = new StringBuilder();
            sb.append("=== All Pulsar Metrics ===\n\n");

            for (Map.Entry<String, List<MetricData>> entry : allMetrics.entrySet()) {
                String metricName = entry.getKey();
                List<MetricData> values = entry.getValue();

                if (!values.isEmpty()) {
                    double totalValue = values.stream()
                            .mapToDouble(MetricData::getValue)
                            .sum();

                    sb.append(String.format("%s: %.2f (from %d series)\n",
                            metricName, totalValue, values.size()));
                }
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error getting all metrics: " + e.getMessage();
        }
    }

    @Tool(description = "Check if Prometheus metrics endpoint is available")
    public String checkMetricsAvailable() {
        log.info("Tool: Checking metrics availability");
        boolean available = metricsCollector.isAvailable();

        if (available) {
            return "✅ Prometheus metrics endpoint is available and responding";
        } else {
            return "❌ Prometheus metrics endpoint is not available. Check configuration and connection.";
        }
    }
}