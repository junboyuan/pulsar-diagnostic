package com.pulsar.diagnostic.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Pulsar Namespace
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceInfo {

    private String namespace;

    private String tenant;

    private String localName;

    private long topicCount;

    private long totalBacklog;

    private long messageInRate;

    private long messageOutRate;

    private int replicationClusters;

    private boolean encryptionRequired;

    private int messageTTL;

    private int retentionTimeSeconds;

    private long retentionSizeMB;

    private int backlogQuotaLimit;

    private int maxProducersPerTopic;

    private int maxConsumersPerTopic;

    private int maxConsumersPerSubscription;
}