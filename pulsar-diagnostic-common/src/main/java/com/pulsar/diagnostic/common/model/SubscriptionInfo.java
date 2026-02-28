package com.pulsar.diagnostic.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Subscription in Pulsar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionInfo {

    private String subscriptionName;

    private String topic;

    private String type;

    private long backlogSize;

    private long messageCount;

    private int consumerCount;

    private boolean isReplicated;

    private boolean isDurable;

    private boolean isActive;

    private String lastConsumedMessageId;

    private String lastPublishedMessageId;

    private long unackedMessages;

    private long blockedConsumerCount;
}