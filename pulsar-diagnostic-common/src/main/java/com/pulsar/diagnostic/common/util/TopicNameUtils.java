package com.pulsar.diagnostic.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing Pulsar topic names
 */
public final class TopicNameUtils {

    private TopicNameUtils() {
        // Prevent instantiation
    }

    // Topic name pattern: persistent://tenant/namespace/topic or non-persistent://tenant/namespace/topic
    private static final Pattern TOPIC_PATTERN = Pattern.compile(
            "(persistent|non-persistent)://([^/]+)/([^/]+)/([^/]+)(?:/(.+))?");

    // Short topic name pattern: tenant/namespace/topic
    private static final Pattern SHORT_TOPIC_PATTERN = Pattern.compile(
            "^([^/]+)/([^/]+)/([^/]+)$");

    /**
     * Parse topic name into components
     */
    public static TopicName parse(String topic) {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Topic name cannot be blank");
        }

        // Handle partitioned topic suffix
        String normalizedTopic = removePartitionSuffix(topic);

        Matcher matcher = TOPIC_PATTERN.matcher(normalizedTopic);
        if (matcher.matches()) {
            return TopicName.builder()
                    .domain(matcher.group(1))
                    .tenant(matcher.group(2))
                    .namespace(matcher.group(3))
                    .localName(matcher.group(4))
                    .fullTopicName(topic)
                    .build();
        }

        // Try short format
        matcher = SHORT_TOPIC_PATTERN.matcher(normalizedTopic);
        if (matcher.matches()) {
            return TopicName.builder()
                    .domain("persistent")
                    .tenant(matcher.group(1))
                    .namespace(matcher.group(2))
                    .localName(matcher.group(3))
                    .fullTopicName(topic)
                    .build();
        }

        throw new IllegalArgumentException("Invalid topic name format: " + topic);
    }

    /**
     * Check if topic name is valid
     */
    public static boolean isValidTopicName(String topic) {
        try {
            parse(topic);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get namespace from full topic name
     */
    public static String getNamespace(String topic) {
        TopicName topicName = parse(topic);
        return topicName.getTenant() + "/" + topicName.getNamespace();
    }

    /**
     * Get tenant from full topic name
     */
    public static String getTenant(String topic) {
        return parse(topic).getTenant();
    }

    /**
     * Get local topic name (without tenant/namespace)
     */
    public static String getLocalName(String topic) {
        return parse(topic).getLocalName();
    }

    /**
     * Build full topic name from components
     */
    public static String buildTopicName(String domain, String tenant, String namespace, String localName) {
        return String.format("%s://%s/%s/%s", domain, tenant, namespace, localName);
    }

    /**
     * Build persistent topic name
     */
    public static String buildPersistentTopic(String tenant, String namespace, String localName) {
        return buildTopicName("persistent", tenant, namespace, localName);
    }

    /**
     * Remove partition suffix from topic name
     */
    public static String removePartitionSuffix(String topic) {
        if (topic == null) {
            return null;
        }
        // Remove partition index suffix like -partition-0
        return topic.replaceAll("-partition-\\d+$", "");
    }

    /**
     * Check if topic is partitioned based on name
     */
    public static boolean isPartitionedTopicName(String topic) {
        return topic != null && topic.contains("-partition-");
    }

    /**
     * Topic name components
     */
    @lombok.Data
    @lombok.Builder
    public static class TopicName {
        private String domain;
        private String tenant;
        private String namespace;
        private String localName;
        private String fullTopicName;

        public String getNamespaceString() {
            return tenant + "/" + namespace;
        }

        public boolean isPersistent() {
            return "persistent".equals(domain);
        }
    }
}