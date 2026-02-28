package com.pulsar.diagnostic.core.metrics;

import com.pulsar.diagnostic.common.exception.MetricsException;
import com.pulsar.diagnostic.common.model.MetricData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Collector for Pulsar metrics from Prometheus
 */
public class PrometheusMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsCollector.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Key Pulsar metrics
    private static final List<String> PULSAR_METRICS = Arrays.asList(
            "pulsar_msgs_in_rate",
            "pulsar_msgs_out_rate",
            "pulsar_bytes_in_rate",
            "pulsar_bytes_out_rate",
            "pulsar_producers_count",
            "pulsar_consumers_count",
            "pulsar_backlog_size",
            "pulsar_broker_connection_count",
            "pulsar_bookie_journal_write_latency",
            "pulsar_bookie_journal_add_entries",
            "pulsar_broker_msg_throughput_in",
            "pulsar_broker_msg_throughput_out"
    );

    public PrometheusMetricsCollector(WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Query a single metric
     */
    public List<MetricData> queryMetric(String query) {
        return queryMetric(query, null);
    }

    /**
     * Query a metric with time range
     */
    public List<MetricData> queryMetric(String query, Long timestamp) {
        try {
            log.debug("Querying Prometheus metric: {}", query);

            String path = "/api/v1/query";
            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path).queryParam("query", query);
                        if (timestamp != null) {
                            uriBuilder.queryParam("time", timestamp);
                        }
                        return uriBuilder.build();
                    });

            String response = request.retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parsePrometheusResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Prometheus query failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw MetricsException.connectionFailed("Prometheus", e);
        } catch (Exception e) {
            log.error("Failed to query Prometheus", e);
            throw new MetricsException("Failed to query Prometheus: " + query, e);
        }
    }

    /**
     * Query a metric over a time range
     */
    public Map<Long, List<MetricData>> queryRange(String query, long start, long end, String step) {
        try {
            log.debug("Querying Prometheus range: {} from {} to {}", query, start, end);

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query_range")
                            .queryParam("query", query)
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("step", step)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return parseRangeResponse(response);

        } catch (Exception e) {
            log.error("Failed to query Prometheus range", e);
            throw new MetricsException("Failed to query Prometheus range: " + query, e);
        }
    }

    /**
     * Get all key Pulsar metrics
     */
    public Map<String, List<MetricData>> getAllPulsarMetrics() {
        Map<String, List<MetricData>> results = new HashMap<>();

        for (String metric : PULSAR_METRICS) {
            try {
                List<MetricData> data = queryMetric(metric);
                results.put(metric, data);
            } catch (Exception e) {
                log.warn("Failed to get metric: {}", metric);
                results.put(metric, Collections.emptyList());
            }
        }

        return results;
    }

    /**
     * Get cluster-level metrics summary
     */
    public ClusterMetricsSummary getClusterMetricsSummary() {
        ClusterMetricsSummary summary = new ClusterMetricsSummary();

        try {
            // Get throughput metrics
            List<MetricData> msgInRate = queryMetric("sum(rate(pulsar_msgs_in_rate[5m]))");
            if (!msgInRate.isEmpty()) {
                summary.setMessagesInRate(msgInRate.get(0).getValue());
            }

            List<MetricData> msgOutRate = queryMetric("sum(rate(pulsar_msgs_out_rate[5m]))");
            if (!msgOutRate.isEmpty()) {
                summary.setMessagesOutRate(msgOutRate.get(0).getValue());
            }

            // Get backlog
            List<MetricData> backlog = queryMetric("sum(pulsar_backlog_size)");
            if (!backlog.isEmpty()) {
                summary.setTotalBacklog((long) backlog.get(0).getValue());
            }

            // Get connection count
            List<MetricData> connections = queryMetric("sum(pulsar_broker_connection_count)");
            if (!connections.isEmpty()) {
                summary.setTotalConnections((long) connections.get(0).getValue());
            }

            // Get producer/consumer counts
            List<MetricData> producers = queryMetric("sum(pulsar_producers_count)");
            if (!producers.isEmpty()) {
                summary.setTotalProducers((long) producers.get(0).getValue());
            }

            List<MetricData> consumers = queryMetric("sum(pulsar_consumers_count)");
            if (!consumers.isEmpty()) {
                summary.setTotalConsumers((long) consumers.get(0).getValue());
            }

        } catch (Exception e) {
            log.error("Failed to get cluster metrics summary", e);
        }

        return summary;
    }

    /**
     * Get broker-specific metrics
     */
    public Map<String, BrokerMetrics> getBrokerMetrics() {
        Map<String, BrokerMetrics> metricsMap = new HashMap<>();

        try {
            // Query broker metrics with labels
            List<MetricData> cpuMetrics = queryMetric("pulsar_broker_cpu_usage");
            List<MetricData> memoryMetrics = queryMetric("pulsar_broker_memory_usage");
            List<MetricData> connectionMetrics = queryMetric("pulsar_broker_connection_count");

            // Group by broker
            for (MetricData metric : connectionMetrics) {
                String brokerId = metric.getLabels() != null ?
                        metric.getLabels().getOrDefault("broker", "unknown") : "unknown";

                BrokerMetrics brokerMetrics = metricsMap.computeIfAbsent(
                        brokerId, k -> new BrokerMetrics());
                brokerMetrics.setConnections((long) metric.getValue());
            }

        } catch (Exception e) {
            log.error("Failed to get broker metrics", e);
        }

        return metricsMap;
    }

    /**
     * Check if Prometheus is available
     */
    public boolean isAvailable() {
        try {
            String response = webClient.get()
                    .uri("/api/v1/query?query=up")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return response != null && response.contains("\"success\"");
        } catch (Exception e) {
            log.debug("Prometheus is not available", e);
            return false;
        }
    }

    /**
     * Parse Prometheus API response
     */
    private List<MetricData> parsePrometheusResponse(String response) {
        List<MetricData> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);

            if (!"success".equals(root.path("status").asText())) {
                log.warn("Prometheus query returned non-success status: {}", response);
                return results;
            }

            JsonNode resultArray = root.path("data").path("result");

            for (JsonNode result : resultArray) {
                Map<String, String> labels = new HashMap<>();
                JsonNode metricNode = result.path("metric");
                metricNode.fields().forEachRemaining(entry ->
                        labels.put(entry.getKey(), entry.getValue().asText()));

                JsonNode valueNode = result.path("value");
                if (valueNode.isArray() && valueNode.size() >= 2) {
                    long timestamp = (long) (valueNode.get(0).asDouble() * 1000);
                    double value = valueNode.get(1).asDouble();

                    String metricName = labels.getOrDefault("__name__", "unknown");

                    results.add(MetricData.builder()
                            .name(metricName)
                            .value(value)
                            .labels(labels)
                            .timestamp(timestamp)
                            .build());
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse Prometheus response", e);
        }

        return results;
    }

    /**
     * Parse range query response
     */
    private Map<Long, List<MetricData>> parseRangeResponse(String response) {
        Map<Long, List<MetricData>> results = new TreeMap<>();

        try {
            JsonNode root = objectMapper.readTree(response);

            if (!"success".equals(root.path("status").asText())) {
                return results;
            }

            JsonNode resultArray = root.path("data").path("result");

            for (JsonNode result : resultArray) {
                Map<String, String> labels = new HashMap<>();
                JsonNode metricNode = result.path("metric");
                metricNode.fields().forEachRemaining(entry ->
                        labels.put(entry.getKey(), entry.getValue().asText()));

                String metricName = labels.getOrDefault("__name__", "unknown");

                JsonNode values = result.path("values");
                for (JsonNode valueNode : values) {
                    if (valueNode.isArray() && valueNode.size() >= 2) {
                        long timestamp = (long) (valueNode.get(0).asDouble() * 1000);
                        double value = valueNode.get(1).asDouble();

                        MetricData metricData = MetricData.builder()
                                .name(metricName)
                                .value(value)
                                .labels(labels)
                                .timestamp(timestamp)
                                .build();

                        results.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(metricData);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse range response", e);
        }

        return results;
    }

    // Inner classes for metrics summary

    public static class ClusterMetricsSummary {
        private double messagesInRate;
        private double messagesOutRate;
        private double bytesInRate;
        private double bytesOutRate;
        private long totalBacklog;
        private long totalConnections;
        private long totalProducers;
        private long totalConsumers;

        public double getMessagesInRate() { return messagesInRate; }
        public void setMessagesInRate(double messagesInRate) { this.messagesInRate = messagesInRate; }
        public double getMessagesOutRate() { return messagesOutRate; }
        public void setMessagesOutRate(double messagesOutRate) { this.messagesOutRate = messagesOutRate; }
        public double getBytesInRate() { return bytesInRate; }
        public void setBytesInRate(double bytesInRate) { this.bytesInRate = bytesInRate; }
        public double getBytesOutRate() { return bytesOutRate; }
        public void setBytesOutRate(double bytesOutRate) { this.bytesOutRate = bytesOutRate; }
        public long getTotalBacklog() { return totalBacklog; }
        public void setTotalBacklog(long totalBacklog) { this.totalBacklog = totalBacklog; }
        public long getTotalConnections() { return totalConnections; }
        public void setTotalConnections(long totalConnections) { this.totalConnections = totalConnections; }
        public long getTotalProducers() { return totalProducers; }
        public void setTotalProducers(long totalProducers) { this.totalProducers = totalProducers; }
        public long getTotalConsumers() { return totalConsumers; }
        public void setTotalConsumers(long totalConsumers) { this.totalConsumers = totalConsumers; }
    }

    public static class BrokerMetrics {
        private String brokerId;
        private double cpuUsage;
        private double memoryUsage;
        private long connections;
        private long producers;
        private long consumers;
        private double messagesInRate;
        private double messagesOutRate;

        public String getBrokerId() { return brokerId; }
        public void setBrokerId(String brokerId) { this.brokerId = brokerId; }
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        public long getConnections() { return connections; }
        public void setConnections(long connections) { this.connections = connections; }
        public long getProducers() { return producers; }
        public void setProducers(long producers) { this.producers = producers; }
        public long getConsumers() { return consumers; }
        public void setConsumers(long consumers) { this.consumers = consumers; }
        public double getMessagesInRate() { return messagesInRate; }
        public void setMessagesInRate(double messagesInRate) { this.messagesInRate = messagesInRate; }
        public double getMessagesOutRate() { return messagesOutRate; }
        public void setMessagesOutRate(double messagesOutRate) { this.messagesOutRate = messagesOutRate; }
    }
}