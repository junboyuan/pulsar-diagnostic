package com.pulsar.diagnostic.agent.tool;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.common.model.SubscriptionInfo;
import com.pulsar.diagnostic.common.model.TopicInfo;
import com.pulsar.diagnostic.core.admin.PulsarAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for querying topic information.
 * Uses MCP server for topic diagnostics.
 */
@Component
public class TopicInfoTool {

    private static final Logger log = LoggerFactory.getLogger(TopicInfoTool.class);

    private final McpClient mcpClient;
    private final PulsarAdminClient pulsarAdminClient;

    public TopicInfoTool(McpClient mcpClient, PulsarAdminClient pulsarAdminClient) {
        this.mcpClient = mcpClient;
        this.pulsarAdminClient = pulsarAdminClient;
    }

    /**
     * Get detailed information about a specific Pulsar topic
     * @param topicName Full topic name in format: persistent://tenant/namespace/topic
     */
    public String getTopicInfo(String topicName) {
        log.info("Tool: Getting topic info for: {} via MCP", topicName);
        try {
            // Use MCP diagnose_topic tool
            return mcpClient.callToolSync("diagnose_topic",
                    Map.of("topic", topicName,
                           "check_backlog", true,
                           "check_subscriptions", false));
        } catch (Exception e) {
            log.error("Failed to get topic info via MCP, falling back", e);
            // Fallback to direct admin client
            try {
                TopicInfo topic = pulsarAdminClient.getTopicInfo(topicName);
                return formatTopicInfo(topic);
            } catch (Exception ex) {
                return "Error getting topic info: " + ex.getMessage();
            }
        }
    }

    /**
     * Get statistics for a specific topic including message rates and throughput
     * @param topicName Full topic name in format: persistent://tenant/namespace/topic
     */
    public String getTopicStats(String topicName) {
        log.info("Tool: Getting topic stats for: {}", topicName);
        try {
            TopicInfo.TopicStats stats = pulsarAdminClient.getTopicStats(topicName);
            return formatTopicStats(stats);
        } catch (Exception e) {
            return "Error getting topic stats: " + e.getMessage();
        }
    }

    /**
     * List all topics in a namespace
     * @param namespace Namespace name in format: tenant/namespace
     */
    public String listTopicsInNamespace(String namespace) {
        log.info("Tool: Listing topics in namespace: {}", namespace);
        try {
            List<String> topics = pulsarAdminClient.getTopics(namespace);
            if (topics.isEmpty()) {
                return "No topics found in namespace: " + namespace;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Topics in namespace '%s' (%d total):\n", namespace, topics.size()));
            for (String topic : topics) {
                sb.append("- ").append(topic).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error listing topics: " + e.getMessage();
        }
    }

    /**
     * Get subscriptions for a specific topic
     * @param topicName Full topic name
     */
    public String getTopicSubscriptions(String topicName) {
        log.info("Tool: Getting subscriptions for topic: {} via MCP", topicName);
        try {
            // Use MCP diagnose_topic tool with subscription check
            return mcpClient.callToolSync("diagnose_topic",
                    Map.of("topic", topicName,
                           "check_backlog", false,
                           "check_subscriptions", true));
        } catch (Exception e) {
            log.error("Failed to get subscriptions via MCP, falling back", e);
            try {
                List<SubscriptionInfo> subscriptions = pulsarAdminClient.getSubscriptions(topicName);
                if (subscriptions.isEmpty()) {
                    return "No subscriptions found for topic: " + topicName;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Subscriptions for '%s' (%d total):\n\n", topicName, subscriptions.size()));

                for (SubscriptionInfo sub : subscriptions) {
                    sb.append(String.format("Subscription: %s\n", sub.getSubscriptionName()));
                    sb.append(String.format("  Type: %s\n", sub.getType()));
                    sb.append(String.format("  Message Backlog: %d\n", sub.getMessageCount()));
                    sb.append(String.format("  Consumers: %d\n", sub.getConsumerCount()));
                    sb.append(String.format("  Active: %s\n\n", sub.isActive() ? "Yes" : "No"));
                }
                return sb.toString();
            } catch (Exception ex) {
                return "Error getting subscriptions: " + ex.getMessage();
            }
        }
    }

    /**
     * Check if a topic has message backlog (unconsumed messages)
     * @param topicName Full topic name
     */
    public String checkTopicBacklog(String topicName) {
        log.info("Tool: Checking backlog for topic: {} via MCP", topicName);
        try {
            return mcpClient.callToolSync("diagnose_topic",
                    Map.of("topic", topicName,
                           "check_backlog", true,
                           "check_subscriptions", false));
        } catch (Exception e) {
            log.error("Failed to check backlog via MCP, falling back", e);
            try {
                TopicInfo topic = pulsarAdminClient.getTopicInfo(topicName);

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Backlog Analysis for '%s':\n", topicName));
                sb.append(String.format("  Total Backlog Size: %d bytes\n", topic.getBacklogSize()));
                sb.append(String.format("  Message Count: %d\n", topic.getMessageCount()));
                sb.append(String.format("  Storage Size: %d bytes\n", topic.getStorageSize()));

                if (topic.getBacklogSize() > 100000000) { // 100MB
                    sb.append("\n⚠️ WARNING: Large backlog detected!\n");
                } else if (topic.getBacklogSize() > 10000000) { // 10MB
                    sb.append("\n⚠️ NOTICE: Moderate backlog present\n");
                } else {
                    sb.append("\n✅ Backlog is within normal range\n");
                }

                return sb.toString();
            } catch (Exception ex) {
                return "Error checking backlog: " + ex.getMessage();
            }
        }
    }

    private String formatTopicInfo(TopicInfo topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Topic Information ===\n");
        sb.append(String.format("Topic: %s\n", topic.getTopic()));
        sb.append(String.format("Name: %s\n", topic.getName()));
        sb.append(String.format("Namespace: %s\n", topic.getNamespace()));
        sb.append(String.format("Tenant: %s\n", topic.getTenant()));
        sb.append(String.format("Persistent: %s\n", topic.isPersistent() ? "Yes" : "No"));
        sb.append(String.format("Partitions: %d\n", topic.getPartitions()));

        sb.append("\n=== Statistics ===\n");
        sb.append(String.format("Producers: %d\n", topic.getProducerCount()));
        sb.append(String.format("Consumers: %d\n", topic.getConsumerCount()));
        sb.append(String.format("Subscriptions: %d\n", topic.getSubscriptionCount()));
        sb.append(String.format("Message Count: %d\n", topic.getMessageCount()));
        sb.append(String.format("Backlog Size: %d bytes\n", topic.getBacklogSize()));
        sb.append(String.format("Storage Size: %d bytes\n", topic.getStorageSize()));

        if (topic.getSubscriptions() != null && !topic.getSubscriptions().isEmpty()) {
            sb.append("\nSubscriptions:\n");
            for (String sub : topic.getSubscriptions()) {
                sb.append("- ").append(sub).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatTopicStats(TopicInfo.TopicStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Topic Statistics ===\n");
        sb.append(String.format("Messages In Rate: %.2f msg/s\n", stats.getMessagesInRate()));
        sb.append(String.format("Messages Out Rate: %.2f msg/s\n", stats.getMessagesOutRate()));
        sb.append(String.format("Throughput In: %.2f bytes/s\n", stats.getThroughputIn()));
        sb.append(String.format("Throughput Out: %.2f bytes/s\n", stats.getThroughputOut()));
        sb.append(String.format("Average Message Size: %.2f bytes\n", stats.getAverageMessageSize()));
        sb.append(String.format("Total Messages Published: %d\n", stats.getTotalMessagesPublished()));
        return sb.toString();
    }
}