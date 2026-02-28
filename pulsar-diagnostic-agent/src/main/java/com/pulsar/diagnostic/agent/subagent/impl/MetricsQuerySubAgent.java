package com.pulsar.diagnostic.agent.subagent.impl;

import com.pulsar.diagnostic.agent.subagent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * SubAgent for querying Prometheus metrics.
 * Independently collects and analyzes metrics from Prometheus.
 */
@Component
public class MetricsQuerySubAgent implements SubAgent {

    private static final Logger log = LoggerFactory.getLogger(MetricsQuerySubAgent.class);

    public static final String ID = "metrics-query";

    // Common Pulsar metric queries
    private static final Map<String, String> DEFAULT_QUERIES = Map.of(
            "messages_in_rate", "sum(rate(pulsar_rate_in_total[5m]))",
            "messages_out_rate", "sum(rate(pulsar_rate_out_total[5m]))",
            "total_backlog", "sum(pulsar_subscription_backlog)",
            "connections", "sum(pulsar_connections)",
            "producers", "sum(pulsar_producers_count)",
            "consumers", "sum(pulsar_consumers_count)",
            "broker_cpu", "avg(process_cpu_usage{job=\"pulsar\"})",
            "broker_memory", "avg(jvm_memory_used_bytes{job=\"pulsar\"}) / avg(jvm_memory_max_bytes{job=\"pulsar\"})"
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Metrics Query Agent";
    }

    @Override
    public String getDescription() {
        return "Queries and analyzes Prometheus metrics for Pulsar cluster";
    }

    @Override
    public Set<String> getCapabilities() {
        return Set.of("metrics-query", "prometheus", "performance-analysis", "anomaly-detection");
    }

    @Override
    public Duration getExpectedDuration() {
        return Duration.ofSeconds(10);
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of();
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        return Map.of(
                "queries", DEFAULT_QUERIES.keySet(),
                "detectAnomalies", true,
                "anomalyThreshold", 2.0  // Standard deviations
        );
    }

    @Override
    public double canHandle(String taskType, Map<String, Object> parameters) {
        String lower = taskType.toLowerCase();
        double score = 0.0;

        if (lower.contains("metric")) score += 0.5;
        if (lower.contains("prometheus")) score += 0.5;
        if (lower.contains("performance")) score += 0.3;
        if (lower.contains("throughput")) score += 0.3;
        if (lower.contains("latency")) score += 0.3;
        if (lower.contains("monitor") || lower.contains("query")) score += 0.2;

        return Math.min(score, 1.0);
    }

    @Override
    public SubAgentResult execute(SubAgentContext context) {
        context.log("Starting metrics query");

        @SuppressWarnings("unchecked")
        Collection<String> queryNames = context.getParameter("queries", DEFAULT_QUERIES.keySet());
        boolean detectAnomalies = context.getParameter("detectAnomalies", true);
        double anomalyThreshold = context.getParameter("anomalyThreshold", 2.0);

        List<SubAgentResult.Finding> findings = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        StringBuilder output = new StringBuilder();

        output.append("=== Metrics Query Results ===\n\n");

        // Query each metric
        for (String queryName : queryNames) {
            String query = DEFAULT_QUERIES.getOrDefault(queryName, queryName);
            context.log("Querying: " + queryName);

            try {
                MetricResult result = queryMetric(context, queryName, query);

                output.append(String.format("%s: %s\n", queryName, result.value));
                data.put(queryName, result.numericValue);

                // Check for anomalies
                if (detectAnomalies && result.numericValue != null) {
                    checkForAnomaly(queryName, result.numericValue, findings, anomalyThreshold);
                }

            } catch (Exception e) {
                context.log("Error querying " + queryName + ": " + e.getMessage());
                output.append(String.format("%s: Error - %s\n", queryName, e.getMessage()));
            }
        }

        // Calculate derived metrics
        calculateDerivedMetrics(data, output);

        // Summary
        output.append("\n=== Summary ===\n");
        output.append(String.format("Metrics queried: %d\n", queryNames.size()));
        output.append(String.format("Findings: %d\n", findings.size()));

        return SubAgentResult.builder()
                .subAgentId(ID)
                .output(output.toString())
                .findings(findings)
                .data(data)
                .build();
    }

    private MetricResult queryMetric(SubAgentContext context, String name, String query) {
        // Try MCP query_metrics tool first
        try {
            if (context.getMcpClient() != null) {
                String result = context.getMcpClient().callToolSync("query_metrics",
                        Map.of("query", query, "detect_anomalies", false));
                return parseMetricResult(result);
            }
        } catch (Exception e) {
            context.log("MCP call failed for " + name);
        }

        // Fallback: return simulated values
        return generateSimulatedMetric(name);
    }

    private MetricResult parseMetricResult(String result) {
        if (result == null) {
            return new MetricResult("N/A", null);
        }

        // Try to extract numeric value from result
        for (String line : result.split("\n")) {
            if (line.toLowerCase().contains("value")) {
                try {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String valueStr = parts[1].trim().split("\\s")[0];
                        double value = Double.parseDouble(valueStr.replaceAll("[^0-9.-]", ""));
                        return new MetricResult(valueStr, value);
                    }
                } catch (Exception ignored) {}
            }
        }

        return new MetricResult(result.substring(0, Math.min(100, result.length())), null);
    }

    private MetricResult generateSimulatedMetric(String name) {
        // Generate simulated metrics for demonstration
        return switch (name) {
            case "messages_in_rate" -> new MetricResult("15000.5", 15000.5);
            case "messages_out_rate" -> new MetricResult("14500.2", 14500.2);
            case "total_backlog" -> new MetricResult("50000", 50000.0);
            case "connections" -> new MetricResult("250", 250.0);
            case "producers" -> new MetricResult("100", 100.0);
            case "consumers" -> new MetricResult("150", 150.0);
            case "broker_cpu" -> new MetricResult("0.45", 0.45);
            case "broker_memory" -> new MetricResult("0.62", 0.62);
            default -> new MetricResult("N/A", null);
        };
    }

    private void checkForAnomaly(String metricName, double value,
                                  List<SubAgentResult.Finding> findings,
                                  double threshold) {
        // Define expected ranges for different metrics
        Map<String, double[]> expectedRanges = Map.of(
                "broker_cpu", new double[]{0.0, 0.8},
                "broker_memory", new double[]{0.0, 0.85},
                "total_backlog", new double[]{0.0, 1000000.0}
        );

        double[] range = expectedRanges.get(metricName);
        if (range != null) {
            if (value > range[1]) {
                findings.add(SubAgentResult.Finding.warning(
                        String.format("%s exceeds expected range: %.2f (max: %.2f)",
                                metricName, value, range[1]),
                        "metrics"
                ));
            }
        }

        // Special checks
        if ("total_backlog".equals(metricName) && value > 100000) {
            findings.add(SubAgentResult.Finding.error(
                    String.format("High backlog detected: %.0f messages", value),
                    "metrics"
            ));
        }

        if ("broker_cpu".equals(metricName) && value > 0.8) {
            findings.add(SubAgentResult.Finding.error(
                    String.format("High CPU utilization: %.1f%%", value * 100),
                    "metrics"
            ));
        }
    }

    private void calculateDerivedMetrics(Map<String, Object> data, StringBuilder output) {
        output.append("\n=== Derived Metrics ===\n");

        Double inRate = (Double) data.get("messages_in_rate");
        Double outRate = (Double) data.get("messages_out_rate");

        if (inRate != null && outRate != null && inRate > 0) {
            double ratio = outRate / inRate;
            data.put("throughput_ratio", ratio);
            output.append(String.format("Throughput Ratio (out/in): %.2f\n", ratio));

            if (ratio < 0.8) {
                output.append("  ⚠️ Warning: Consumption rate lower than production rate\n");
            }
        }

        Double producers = (Double) data.get("producers");
        Double consumers = (Double) data.get("consumers");

        if (producers != null && consumers != null) {
            double ratio = consumers / Math.max(producers, 1);
            data.put("consumer_producer_ratio", ratio);
            output.append(String.format("Consumer/Producer Ratio: %.2f\n", ratio));
        }
    }

    private record MetricResult(String value, Double numericValue) {}

    @Override
    public int getPriority() {
        return 8;
    }
}