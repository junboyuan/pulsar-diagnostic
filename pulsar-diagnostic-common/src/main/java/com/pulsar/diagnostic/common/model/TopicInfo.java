package com.pulsar.diagnostic.common.model;

import com.pulsar.diagnostic.common.enums.TopicType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a Pulsar Topic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicInfo {

    private String topic;

    private String name;

    private String namespace;

    private String tenant;

    private TopicType type;

    private boolean persistent;

    private int partitions;

    private long backlogSize;

    private long messageCount;

    private long storageSize;

    private int producerCount;

    private int consumerCount;

    private int subscriptionCount;

    private List<String> subscriptions;

    private List<String> producers;

    private List<String> consumers;

    private TopicStats stats;

    private Map<String, String> schemaInfo;

    private LocalDateTime createdAt;

    private LocalDateTime lastUpdated;

    /**
     * Topic statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicStats {
        private double messagesInRate;
        private double messagesOutRate;
        private double throughputIn;
        private double throughputOut;
        private double averageMessageSize;
        private long totalMessagesPublished;
        private long totalMessagesConsumed;
        private long totalBytesPublished;
        private long totalBytesConsumed;
    }
}