package com.pulsar.diagnostic.agent.skill.impl;

import com.pulsar.diagnostic.agent.skill.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for analyzing cluster performance.
 * Collects metrics, identifies bottlenecks, and provides optimization recommendations.
 */
@Component
public class PerformanceAnalysisSkill extends AbstractSkill {

    public static final String NAME = "performance-analysis";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Analyze Pulsar cluster performance, identify bottlenecks, and provide optimization recommendations";
    }

    @Override
    public String getCategory() {
        return "analysis";
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of(
                SkillParameter.optional("component", "Component to focus on: brokers, bookies, topics, all", "string", "all"),
                SkillParameter.optional("duration", "Analysis duration in minutes", "integer", "5")
        );
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of(
                "analyze performance",
                "check performance",
                "why is pulsar slow",
                "optimize throughput",
                "performance bottleneck"
        );
    }

    @Override
    protected SkillResult doExecute(SkillContext context) {
        String component = context.getParameter("component", "all");
        int duration = context.getParameter("duration", 5);

        context.log("Starting performance analysis...");
        context.log("Component: " + component + ", Duration: " + duration + "min");

        List<SkillResult.Finding> findings = new ArrayList<>();
        List<SkillResult.Recommendation> recommendations = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        // Step 1: Get cluster metrics
        context.log("Step 1: Collecting cluster metrics...");
        String clusterMetrics = context.tools().getClusterMetrics();
        output.append("=== Cluster Metrics ===\n").append(clusterMetrics).append("\n");

        analyzeClusterMetrics(clusterMetrics, findings);

        // Step 2: Get broker metrics
        if ("all".equals(component) || "brokers".equals(component)) {
            context.log("Step 2: Analyzing broker performance...");
            String brokerMetrics = context.tools().getBrokerMetrics();
            output.append("\n=== Broker Metrics ===\n").append(brokerMetrics).append("\n");

            analyzeBrokerMetrics(brokerMetrics, findings, recommendations);
        }

        // Step 3: Query specific performance metrics
        context.log("Step 3: Querying performance metrics...");
        queryPerformanceMetrics(context, output, findings);

        // Step 4: Analyze logs for performance issues
        if ("all".equals(component)) {
            context.log("Step 4: Checking logs for performance issues...");
            String brokerLogs = context.tools().analyzeBrokerLogs(200);
            output.append("\n=== Log Analysis ===\n").append(brokerLogs).append("\n");

            checkForPerformancePatterns(brokerLogs, findings);
        }

        // Step 5: Get knowledge recommendations
        if (context.knowledge().isReady()) {
            context.log("Step 5: Getting optimization recommendations...");
            String bestPractices = context.knowledge().getBestPractices("performance");
            if (bestPractices != null && !bestPractices.isEmpty()) {
                output.append("\n=== Performance Best Practices ===\n").append(bestPractices).append("\n");
            }
        }

        // Add recommendations based on findings
        addPerformanceRecommendations(findings, recommendations);

        return SkillResult.builder()
                .success(true)
                .output(output.toString())
                .findings(findings)
                .recommendations(recommendations)
                .metadata(Map.of(
                        "component", component,
                        "duration", duration,
                        "findingsCount", findings.size()
                ))
                .build();
    }

    private void analyzeClusterMetrics(String metrics, List<SkillResult.Finding> findings) {
        // Check for throughput imbalance
        String inRateStr = extractValue(metrics, "Messages In Rate");
        String outRateStr = extractValue(metrics, "Messages Out Rate");

        if (inRateStr != null && outRateStr != null) {
            double inRate = parseDouble(inRateStr, 0);
            double outRate = parseDouble(outRateStr, 0);

            if (inRate > 0 && outRate > 0) {
                double ratio = outRate / inRate;
                if (ratio < 0.5 && inRate > 100) {
                    findings.add(SkillResult.Finding.warning(
                            "Throughput Imbalance",
                            String.format("Out rate (%.2f) is significantly lower than in rate (%.2f)", outRate, inRate),
                            "cluster"
                    ));
                }
            }
        }

        // Check connections
        String connStr = extractValue(metrics, "Total Connections");
        if (connStr != null) {
            double connections = parseDouble(connStr, -1);
            if (connections == 0) {
                findings.add(SkillResult.Finding.warning(
                        "No Active Connections",
                        "Cluster has zero active connections",
                        "cluster"
                ));
            }
        }
    }

    private void analyzeBrokerMetrics(String metrics, List<SkillResult.Finding> findings,
                                       List<SkillResult.Recommendation> recommendations) {
        // Check CPU usage
        if (metrics.toLowerCase().contains("cpu")) {
            for (String line : metrics.split("\n")) {
                if (line.toLowerCase().contains("cpu")) {
                    double cpu = parseDouble(extractValue(line, "CPU"), 0);
                    if (cpu > 80) {
                        findings.add(SkillResult.Finding.error(
                                "High CPU Usage",
                                String.format("Broker CPU usage is %.1f%%", cpu),
                                "broker"
                        ));
                    } else if (cpu > 60) {
                        findings.add(SkillResult.Finding.warning(
                                "Elevated CPU Usage",
                                String.format("Broker CPU usage is %.1f%%", cpu),
                                "broker"
                        ));
                    }
                }
            }
        }

        // Check memory usage
        if (metrics.toLowerCase().contains("memory")) {
            for (String line : metrics.split("\n")) {
                if (line.toLowerCase().contains("memory")) {
                    double mem = parseDouble(extractValue(line, "Memory"), 0);
                    if (mem > 85) {
                        findings.add(SkillResult.Finding.error(
                                "High Memory Usage",
                                String.format("Broker memory usage is %.1f%%", mem),
                                "broker"
                        ));
                        recommendations.add(SkillResult.Recommendation.high(
                                "Increase broker memory",
                                "Consider increasing JVM heap size or adding more brokers"
                        ));
                    }
                }
            }
        }
    }

    private void queryPerformanceMetrics(SkillContext context, StringBuilder output,
                                          List<SkillResult.Finding> findings) {
        // Query end-to-end latency
        String latencyQuery = "pulsar_broker_end_to_end_latency";
        String latencyResult = context.mcp().queryMetrics(latencyQuery, true);
        if (!hasError(latencyResult)) {
            output.append("\n=== Latency Metrics ===\n").append(latencyResult).append("\n");
        }

        // Query message throughput
        String throughputQuery = "rate(pulsar_rate_in_total[5m])";
        String throughputResult = context.mcp().queryMetrics(throughputQuery, true);
        if (!hasError(throughputResult)) {
            output.append("\n=== Throughput Trends ===\n").append(throughputResult).append("\n");
        }
    }

    private void checkForPerformancePatterns(String logs, List<SkillResult.Finding> findings) {
        String lower = logs.toLowerCase();

        if (lower.contains("gc overhead") || lower.contains("outofmemoryerror")) {
            findings.add(SkillResult.Finding.error(
                    "Memory Pressure Detected",
                    "JVM memory issues found in broker logs",
                    "broker"
            ));
        }

        if (lower.contains("too many open files")) {
            findings.add(SkillResult.Finding.error(
                    "File Descriptor Limit",
                    "Broker hitting file descriptor limits",
                    "broker"
            ));
        }

        if (lower.contains("connection refused") || lower.contains("connection reset")) {
            findings.add(SkillResult.Finding.warning(
                    "Connection Issues",
                    "Connection problems detected in logs",
                    "network"
            ));
        }
    }

    private void addPerformanceRecommendations(List<SkillResult.Finding> findings,
                                                List<SkillResult.Recommendation> recommendations) {
        boolean hasCpuIssue = findings.stream().anyMatch(f -> f.title().contains("CPU"));
        boolean hasMemoryIssue = findings.stream().anyMatch(f -> f.title().contains("Memory"));
        boolean hasThroughputIssue = findings.stream().anyMatch(f -> f.title().contains("Throughput"));

        if (hasCpuIssue) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Optimize CPU usage",
                    "Review broker configuration, consider batch processing optimization"
            ));
        }

        if (hasMemoryIssue) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Tune JVM memory",
                    "Adjust -Xmx and -Xms settings, enable G1GC for better memory management"
            ));
        }

        if (hasThroughputIssue) {
            recommendations.add(SkillResult.Recommendation.medium(
                    "Scale cluster",
                    "Consider adding more brokers or increasing partition count"
            ));
        }

        if (findings.isEmpty()) {
            recommendations.add(SkillResult.Recommendation.low(
                    "Regular monitoring",
                    "Continue monitoring performance metrics regularly"
            ));
        }
    }

    @Override
    public double canHandle(String query) {
        String lower = query.toLowerCase();
        double score = 0.0;

        if (lower.contains("performance")) score += 0.4;
        if (lower.contains("slow")) score += 0.2;
        if (lower.contains("throughput")) score += 0.3;
        if (lower.contains("latency")) score += 0.3;
        if (lower.contains("optimize")) score += 0.2;
        if (lower.contains("bottleneck")) score += 0.3;
        if (lower.contains("cpu") || lower.contains("memory")) score += 0.2;

        return Math.min(score, 1.0);
    }
}