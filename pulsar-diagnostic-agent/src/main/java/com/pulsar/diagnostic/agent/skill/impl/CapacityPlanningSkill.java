package com.pulsar.diagnostic.agent.skill.impl;

import com.pulsar.diagnostic.agent.skill.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for capacity planning and resource analysis.
 * Analyzes current resource usage and provides scaling recommendations.
 */
@Component
public class CapacityPlanningSkill extends AbstractSkill {

    public static final String NAME = "capacity-planning";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Analyze cluster capacity, predict resource needs, and provide scaling recommendations";
    }

    @Override
    public String getCategory() {
        return "consultation";
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of(
                SkillParameter.optional("growthRate", "Expected monthly growth rate (percentage)", "integer", "20"),
                SkillParameter.optional("targetUtilization", "Target resource utilization (percentage)", "integer", "70"),
                SkillParameter.optional("planningHorizon", "Planning horizon in months", "integer", "6")
        );
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of(
                "capacity planning",
                "do I need to scale",
                "resource planning",
                "cluster capacity",
                "scaling recommendations",
                "how many brokers do I need"
        );
    }

    @Override
    protected SkillResult doExecute(SkillContext context) {
        int growthRate = context.getParameter("growthRate", 20);
        int targetUtilization = context.getParameter("targetUtilization", 70);
        int planningHorizon = context.getParameter("planningHorizon", 6);

        context.log("Starting capacity planning analysis...");
        context.log("Growth rate: " + growthRate + "%, Target utilization: " + targetUtilization + "%, Horizon: " + planningHorizon + " months");

        List<SkillResult.Finding> findings = new ArrayList<>();
        List<SkillResult.Recommendation> recommendations = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        // Step 1: Get current cluster info
        context.log("Step 1: Gathering cluster information...");
        String clusterInfo = context.tools().getClusterInfo();
        output.append("=== Current Cluster State ===\n").append(clusterInfo).append("\n");

        // Step 2: Get broker metrics
        context.log("Step 2: Analyzing broker resources...");
        String brokerMetrics = context.tools().getBrokerMetrics();
        output.append("\n=== Broker Resources ===\n").append(brokerMetrics).append("\n");

        ResourceAnalysis brokerAnalysis = analyzeBrokerResources(brokerMetrics, findings);

        // Step 3: Get cluster metrics
        context.log("Step 3: Analyzing cluster load...");
        String clusterMetrics = context.tools().getClusterMetrics();
        output.append("\n=== Cluster Load ===\n").append(clusterMetrics).append("\n");

        ClusterLoad clusterLoad = analyzeClusterLoad(clusterMetrics, findings);

        // Step 4: Calculate capacity projections
        context.log("Step 4: Calculating capacity projections...");
        CapacityProjection projection = calculateProjection(clusterLoad, brokerAnalysis, growthRate, planningHorizon);

        output.append("\n=== Capacity Projections ===\n");
        output.append(String.format("Current Topics: %d → Projected: %d\n",
                clusterLoad.topics, projection.projectedTopics));
        output.append(String.format("Current Throughput: %.2f msg/s → Projected: %.2f msg/s\n",
                clusterLoad.throughput, projection.projectedThroughput));
        output.append(String.format("Current Connections: %d → Projected: %d\n",
                clusterLoad.connections, projection.projectedConnections));
        output.append(String.format("Required Brokers (at %d%% utilization): %d\n",
                targetUtilization, projection.requiredBrokers));
        output.append(String.format("Additional Brokers Needed: %d\n", projection.additionalBrokers));

        // Step 5: Check bookie capacity
        context.log("Step 5: Analyzing bookie capacity...");
        String bookies = context.tools().getBookies();
        output.append("\n=== Bookie Capacity ===\n").append(bookies).append("\n");

        int bookieCount = countBookies(bookies);
        int requiredBookies = calculateRequiredBookies(bookieCount, projection.projectedThroughput);
        output.append(String.format("\nRequired Bookies (for quorum + redundancy): %d\n", requiredBookies));
        output.append(String.format("Additional Bookies Needed: %d\n", Math.max(0, requiredBookies - bookieCount)));

        // Step 6: Generate recommendations
        generateCapacityRecommendations(projection, brokerAnalysis, targetUtilization, recommendations);

        // Step 7: Get best practices
        if (context.knowledge().isReady()) {
            context.log("Step 6: Getting capacity planning best practices...");
            String bestPractices = context.knowledge().getBestPractices("capacity");
            if (bestPractices != null && !bestPractices.isEmpty()) {
                output.append("\n=== Best Practices ===\n").append(bestPractices).append("\n");
            }
        }

        return SkillResult.builder()
                .success(true)
                .output(output.toString())
                .findings(findings)
                .recommendations(recommendations)
                .metadata(Map.of(
                        "growthRate", growthRate,
                        "targetUtilization", targetUtilization,
                        "planningHorizon", planningHorizon,
                        "currentBrokers", brokerAnalysis.brokerCount,
                        "requiredBrokers", projection.requiredBrokers,
                        "projectedThroughput", projection.projectedThroughput
                ))
                .build();
    }

    private ResourceAnalysis analyzeBrokerResources(String metrics, List<SkillResult.Finding> findings) {
        ResourceAnalysis analysis = new ResourceAnalysis();

        if (metrics == null) return analysis;

        // Count brokers
        int brokerCount = 0;
        double totalCpu = 0;
        double totalMemory = 0;

        for (String line : metrics.split("\n")) {
            if (line.toLowerCase().contains("broker:")) {
                brokerCount++;
            }
            if (line.toLowerCase().contains("cpu")) {
                double cpu = parseDouble(extractValue(line, "CPU"), 0);
                totalCpu = Math.max(totalCpu, cpu);
            }
            if (line.toLowerCase().contains("memory")) {
                double mem = parseDouble(extractValue(line, "Memory"), 0);
                totalMemory = Math.max(totalMemory, mem);
            }
        }

        analysis.brokerCount = brokerCount;
        analysis.maxCpuUtilization = totalCpu;
        analysis.maxMemoryUtilization = totalMemory;

        if (totalCpu > 80) {
            findings.add(SkillResult.Finding.warning(
                    "High CPU Utilization",
                    String.format("Broker CPU utilization at %.1f%% - limited headroom for growth", totalCpu),
                    "broker"
            ));
        }

        if (totalMemory > 80) {
            findings.add(SkillResult.Finding.warning(
                    "High Memory Utilization",
                    String.format("Broker memory utilization at %.1f%% - limited headroom for growth", totalMemory),
                    "broker"
            ));
        }

        return analysis;
    }

    private ClusterLoad analyzeClusterLoad(String metrics, List<SkillResult.Finding> findings) {
        ClusterLoad load = new ClusterLoad();

        if (metrics == null) return load;

        String inRate = extractValue(metrics, "Messages In Rate");
        String outRate = extractValue(metrics, "Messages Out Rate");
        String connections = extractValue(metrics, "Total Connections");
        String backlog = extractValue(metrics, "Total Backlog");

        load.throughput = parseDouble(inRate, 0) + parseDouble(outRate, 0);
        load.connections = (int) parseDouble(connections, 0);
        load.backlog = (long) parseDouble(backlog, 0);

        return load;
    }

    private CapacityProjection calculateProjection(ClusterLoad current, ResourceAnalysis resources,
                                                    int growthRate, int months) {
        CapacityProjection projection = new CapacityProjection();

        double growthFactor = Math.pow(1 + growthRate / 100.0, months);

        projection.projectedTopics = (int) (current.topics * growthFactor);
        projection.projectedThroughput = current.throughput * growthFactor;
        projection.projectedConnections = (int) (current.connections * growthFactor);
        projection.projectedBacklog = (long) (current.backlog * growthFactor);

        // Estimate required brokers (rough estimate: 10K msg/s per broker at moderate load)
        double msgsPerBrokerPerSec = 10000;
        projection.requiredBrokers = (int) Math.ceil(projection.projectedThroughput / msgsPerBrokerPerSec);
        projection.requiredBrokers = Math.max(projection.requiredBrokers, 3); // Minimum for HA

        projection.additionalBrokers = Math.max(0, projection.requiredBrokers - resources.brokerCount);

        return projection;
    }

    private int countBookies(String bookies) {
        if (bookies == null) return 0;
        int count = 0;
        for (String line : bookies.split("\n")) {
            if (line.contains("-") && !line.toLowerCase().contains("no bookies")) {
                count++;
            }
        }
        return count;
    }

    private int calculateRequiredBookies(int current, double projectedThroughput) {
        // BookKeeper typically needs at least 3 for quorum
        // Scale based on throughput: roughly 1 bookie per 20K msg/s write rate
        int minBookies = 3;
        int throughputBased = (int) Math.ceil(projectedThroughput / 20000 * 2); // Factor 2 for redundancy
        return Math.max(minBookies, Math.max(current, throughputBased));
    }

    private void generateCapacityRecommendations(CapacityProjection projection, ResourceAnalysis resources,
                                                  int targetUtilization, List<SkillResult.Recommendation> recommendations) {

        if (projection.additionalBrokers > 0) {
            recommendations.add(SkillResult.Recommendation.high(
                    String.format("Add %d broker(s)", projection.additionalBrokers),
                    String.format("Projected load requires %d brokers for target utilization", projection.requiredBrokers)
            ));
        }

        if (resources.maxCpuUtilization > targetUtilization) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Upgrade broker CPU resources",
                    String.format("Current CPU utilization (%.1f%%) exceeds target (%d%%)", resources.maxCpuUtilization, targetUtilization)
            ));
        }

        if (resources.maxMemoryUtilization > targetUtilization) {
            recommendations.add(SkillResult.Recommendation.high(
                    "Increase broker memory",
                    String.format("Current memory utilization (%.1f%%) exceeds target (%d%%)", resources.maxMemoryUtilization, targetUtilization)
            ));
        }

        if (projection.projectedThroughput > resources.brokerCount * 15000) {
            recommendations.add(SkillResult.Recommendation.medium(
                    "Consider partition scaling",
                    "Topic partitioning can improve parallelism and throughput"
            ));
        }

        if (projection.projectedConnections > resources.brokerCount * 5000) {
            recommendations.add(SkillResult.Recommendation.medium(
                    "Review connection pooling",
                    "High projected connections - consider connection pooling strategies"
            ));
        }
    }

    private static class ResourceAnalysis {
        int brokerCount = 0;
        double maxCpuUtilization = 0;
        double maxMemoryUtilization = 0;
    }

    private static class ClusterLoad {
        int topics = 0;
        double throughput = 0;
        int connections = 0;
        long backlog = 0;
    }

    private static class CapacityProjection {
        int projectedTopics;
        double projectedThroughput;
        int projectedConnections;
        long projectedBacklog;
        int requiredBrokers;
        int additionalBrokers;
    }

    @Override
    public double canHandle(String query) {
        String lower = query.toLowerCase();
        double score = 0.0;

        if (lower.contains("capacity")) score += 0.4;
        if (lower.contains("scale") || lower.contains("scaling")) score += 0.3;
        if (lower.contains("planning")) score += 0.3;
        if (lower.contains("resource")) score += 0.2;
        if (lower.contains("grow") || lower.contains("growth")) score += 0.2;
        if (lower.contains("how many broker")) score += 0.4;

        return Math.min(score, 1.0);
    }
}