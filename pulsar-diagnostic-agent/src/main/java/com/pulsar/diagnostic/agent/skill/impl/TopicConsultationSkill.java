package com.pulsar.diagnostic.agent.skill.impl;

import com.pulsar.diagnostic.agent.skill.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for topic diagnosis and consultation.
 * Analyzes topic configuration, performance, and provides optimization recommendations.
 */
@Component
public class TopicConsultationSkill extends AbstractSkill {

    public static final String NAME = "topic-consultation";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Consult on Pulsar topic design, configuration optimization, and troubleshooting";
    }

    @Override
    public String getCategory() {
        return "consultation";
    }

    @Override
    public List<SkillParameter> getRequiredParameters() {
        return List.of(
                SkillParameter.required("topic", "Full topic name (persistent://tenant/namespace/topic)", "string")
        );
    }

    @Override
    public List<SkillParameter> getOptionalParameters() {
        return List.of(
                SkillParameter.optional("focus", "Focus area: configuration, performance, subscriptions, all", "string", "all")
        );
    }

    @Override
    public List<String> getExamplePrompts() {
        return List.of(
                "analyze topic",
                "topic diagnosis",
                "optimize topic",
                "topic configuration",
                "check topic health"
        );
    }

    @Override
    protected SkillResult doExecute(SkillContext context) {
        String topic = context.getParameter("topic");
        String focus = context.getParameter("focus", "all");

        context.log("Starting topic consultation for: " + topic);
        context.log("Focus area: " + focus);

        List<SkillResult.Finding> findings = new ArrayList<>();
        List<SkillResult.Recommendation> recommendations = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        // Step 1: Get topic info
        context.log("Step 1: Gathering topic information...");
        String topicInfo = context.tools().getTopicInfo(topic);
        output.append("=== Topic Information ===\n").append(topicInfo).append("\n");

        analyzeTopicInfo(topicInfo, findings);

        // Step 2: Get topic stats
        context.log("Step 2: Analyzing topic statistics...");
        String topicStats = context.tools().getTopicStats(topic);
        output.append("\n=== Topic Statistics ===\n").append(topicStats).append("\n");

        analyzeTopicStats(topicStats, findings, recommendations);

        // Step 3: Check subscriptions
        if ("all".equals(focus) || "subscriptions".equals(focus)) {
            context.log("Step 3: Analyzing subscriptions...");
            String subscriptions = context.tools().getTopicSubscriptions(topic);
            output.append("\n=== Subscriptions ===\n").append(subscriptions).append("\n");

            analyzeSubscriptions(subscriptions, findings, recommendations);
        }

        // Step 4: Check backlog
        if ("all".equals(focus) || "performance".equals(focus)) {
            context.log("Step 4: Checking backlog status...");
            String backlog = context.tools().checkTopicBacklog(topic);
            output.append("\n=== Backlog Analysis ===\n").append(backlog).append("\n");

            analyzeBacklog(backlog, findings, recommendations);
        }

        // Step 5: Diagnose via MCP
        context.log("Step 5: Running MCP diagnosis...");
        String diagnosis = context.mcp().diagnoseTopic(topic, true, true);
        output.append("\n=== MCP Diagnosis ===\n").append(diagnosis).append("\n");

        // Step 6: Get best practices
        if (context.knowledge().isReady()) {
            context.log("Step 6: Getting topic best practices...");
            String bestPractices = context.knowledge().getBestPractices("topic");
            if (bestPractices != null && !bestPractices.isEmpty()) {
                output.append("\n=== Best Practices ===\n").append(bestPractices).append("\n");
            }
        }

        // Generate recommendations
        generateTopicRecommendations(findings, recommendations);

        return SkillResult.builder()
                .success(true)
                .output(output.toString())
                .findings(findings)
                .recommendations(recommendations)
                .metadata(Map.of(
                        "topic", topic,
                        "focus", focus,
                        "findingsCount", findings.size()
                ))
                .build();
    }

    private void analyzeTopicInfo(String info, List<SkillResult.Finding> findings) {
        if (info == null) return;

        String lower = info.toLowerCase();

        // Check partition count
        if (lower.contains("partitions: 0") || lower.contains("partitions: 1")) {
            // Non-partitioned topic - might be OK but worth noting for high-throughput
            findings.add(SkillResult.Finding.info(
                    "Non-Partitioned Topic",
                    "Topic has single partition - consider partitioning for high throughput",
                    "topic"
            ));
        }

        // Check persistence
        if (!lower.contains("persistent: yes")) {
            findings.add(SkillResult.Finding.info(
                    "Non-Persistent Topic",
                    "Topic is non-persistent - messages are not durably stored",
                    "topic"
            ));
        }

        // Check producer/consumer count
        if (lower.contains("producers: 0")) {
            findings.add(SkillResult.Finding.warning(
                    "No Producers",
                    "Topic has no active producers",
                    "topic"
            ));
        }
    }

    private void analyzeTopicStats(String stats, List<SkillResult.Finding> findings,
                                    List<SkillResult.Recommendation> recommendations) {
        if (stats == null) return;

        // Check message rates
        String inRate = extractValue(stats, "Messages In Rate");
        String outRate = extractValue(stats, "Messages Out Rate");

        if (inRate != null && outRate != null) {
            double in = parseDouble(inRate, 0);
            double out = parseDouble(outRate, 0);

            if (in > 0 && out == 0) {
                findings.add(SkillResult.Finding.warning(
                        "No Message Consumption",
                        "Messages are being produced but not consumed",
                        "topic"
                ));
                recommendations.add(SkillResult.Recommendation.high(
                        "Verify consumer status",
                        "Check if consumers are running and properly configured"
                ));
            }

            if (in > 0 && out > 0 && out / in < 0.5) {
                findings.add(SkillResult.Finding.warning(
                        "Consumption Lag",
                        String.format("Out rate (%.2f) significantly lower than in rate (%.2f)", out, in),
                        "topic"
                ));
            }
        }

        // Check storage size
        String storage = extractValue(stats, "Storage Size");
        if (storage != null) {
            double bytes = parseDouble(storage, 0);
            if (bytes > 10_000_000_000L) { // 10GB
                findings.add(SkillResult.Finding.info(
                        "Large Topic Storage",
                        String.format("Topic storage is %.2f GB", bytes / 1_000_000_000.0),
                        "topic"
                ));
            }
        }
    }

    private void analyzeSubscriptions(String subs, List<SkillResult.Finding> findings,
                                       List<SkillResult.Recommendation> recommendations) {
        if (subs == null) return;

        String lower = subs.toLowerCase();

        if (lower.contains("no subscriptions")) {
            findings.add(SkillResult.Finding.error(
                    "No Subscriptions",
                    "Topic has no subscriptions - messages will accumulate indefinitely",
                    "topic"
            ));
            recommendations.add(SkillResult.Recommendation.high(
                    "Create subscription",
                    "Add a subscription to consume messages from this topic"
            ));
        }

        // Check for subscription types
        if (!lower.contains("failover") && !lower.contains("shared") && !lower.contains("exclusive")) {
            // OK - might have default subscription type
        }

        // Check consumer count
        for (String line : subs.split("\n")) {
            if (line.toLowerCase().contains("consumers: 0")) {
                findings.add(SkillResult.Finding.warning(
                        "Subscription Without Consumers",
                        "A subscription has no active consumers",
                        "subscription"
                ));
            }
        }
    }

    private void analyzeBacklog(String backlog, List<SkillResult.Finding> findings,
                                 List<SkillResult.Recommendation> recommendations) {
        if (backlog == null) return;

        String lower = backlog.toLowerCase();

        if (lower.contains("warning") || lower.contains("large backlog")) {
            findings.add(SkillResult.Finding.warning(
                    "Large Backlog Detected",
                    "Topic has significant message backlog",
                    "topic"
            ));
            recommendations.add(SkillResult.Recommendation.high(
                    "Address backlog growth",
                    "Scale consumers or investigate processing bottlenecks"
            ));
        }

        // Extract backlog size if available
        for (String line : backlog.split("\n")) {
            if (line.toLowerCase().contains("backlog")) {
                String size = extractValue(line, "Backlog");
                if (size != null) {
                    double messages = parseDouble(size, 0);
                    if (messages > 100000) {
                        findings.add(SkillResult.Finding.warning(
                                "High Backlog Count",
                                String.format("Backlog has %s messages", size),
                                "topic"
                        ));
                    }
                }
            }
        }
    }

    private void generateTopicRecommendations(List<SkillResult.Finding> findings,
                                               List<SkillResult.Recommendation> recommendations) {
        boolean hasBacklogIssue = findings.stream().anyMatch(f -> f.title().contains("Backlog"));
        boolean hasConsumerIssue = findings.stream().anyMatch(f -> f.title().contains("Consumer") || f.title().contains("Subscription"));
        boolean hasPerformanceIssue = findings.stream().anyMatch(f -> f.title().contains("Consumption") || f.title().contains("Lag"));

        if (hasBacklogIssue && !hasConsumerIssue) {
            recommendations.add(SkillResult.Recommendation.medium(
                    "Review consumer processing",
                    "Investigate why messages are not being consumed at expected rate"
            ));
        }

        if (hasPerformanceIssue) {
            recommendations.add(SkillResult.Recommendation.medium(
                    "Consider topic partitioning",
                    "Partitioning can improve parallelism for high-throughput topics"
            ));
        }

        if (findings.isEmpty()) {
            recommendations.add(SkillResult.Recommendation.low(
                    "Topic is healthy",
                    "No significant issues detected with this topic"
            ));
        }
    }

    @Override
    public double canHandle(String query) {
        String lower = query.toLowerCase();
        double score = 0.0;

        if (lower.contains("topic")) score += 0.3;
        if (lower.contains("persistent://")) score += 0.5;
        if (lower.contains("subscription")) score += 0.2;
        if (lower.contains("partition")) score += 0.2;
        if (lower.contains("producer") || lower.contains("consumer")) score += 0.2;

        return Math.min(score, 1.0);
    }
}