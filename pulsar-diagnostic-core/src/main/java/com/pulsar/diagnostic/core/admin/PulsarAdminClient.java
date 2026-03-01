package com.pulsar.diagnostic.core.admin;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import com.pulsar.diagnostic.core.config.PulsarConfig;
import io.micrometer.core.annotation.Timed;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.common.naming.NamespaceName;
import org.apache.pulsar.common.naming.TopicName;
import org.apache.pulsar.common.policies.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper for Pulsar Admin API operations - compatible with Pulsar 2.10.4
 */
@Component
public class PulsarAdminClient {

    private static final Logger log = LoggerFactory.getLogger(PulsarAdminClient.class);

    private final PulsarAdmin pulsarAdmin;
    private final PulsarConfig pulsarConfig;

    public PulsarAdminClient(PulsarAdmin pulsarAdmin, PulsarConfig pulsarConfig) {
        this.pulsarAdmin = pulsarAdmin;
        this.pulsarConfig = pulsarConfig;
    }

    // ==================== Cluster Operations ====================

    /**
     * Get cluster info
     */
    @Timed(value = "pulsar.admin.cluster.info", description = "Time to get cluster info")
    @Cacheable(value = "clusterInfo", key = "'cluster-info'")
    public com.pulsar.diagnostic.common.model.PulsarCluster getClusterInfo() {
        try {
            log.info("Fetching cluster info for: {}", pulsarConfig.getClusterName());

            List<com.pulsar.diagnostic.common.model.BrokerInfo> brokers = getBrokers();
            List<com.pulsar.diagnostic.common.model.BookieInfo> bookies = getBookies();

            HealthStatus healthStatus = determineClusterHealth(brokers, bookies);

            com.pulsar.diagnostic.common.model.PulsarCluster.ClusterStats stats = collectClusterStats();

            return com.pulsar.diagnostic.common.model.PulsarCluster.builder()
                    .clusterName(pulsarConfig.getClusterName())
                    .serviceUrl(pulsarConfig.getBrokerUrl())
                    .adminUrl(pulsarConfig.getAdminUrl())
                    .brokerServiceUrl(pulsarConfig.getBrokerUrl())
                    .healthStatus(healthStatus)
                    .brokers(brokers)
                    .bookies(bookies)
                    .stats(stats)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get cluster info", e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get cluster info", e);
        }
    }

    /**
     * Check cluster health
     */
    @Timed(value = "pulsar.admin.cluster.health", description = "Time to check cluster health")
    public HealthStatus checkHealth() {
        try {
            List<String> brokers = pulsarAdmin.brokers().getActiveBrokers(pulsarConfig.getClusterName());
            return brokers.isEmpty() ? HealthStatus.CRITICAL : HealthStatus.HEALTHY;
        } catch (PulsarAdminException e) {
            log.error("Health check failed", e);
            return HealthStatus.CRITICAL;
        }
    }

    // ==================== Broker Operations ====================

    /**
     * Get all active brokers
     */
    @Timed(value = "pulsar.admin.brokers.list", description = "Time to list brokers")
    @Cacheable(value = "brokers", key = "'all-brokers'")
    public List<com.pulsar.diagnostic.common.model.BrokerInfo> getBrokers() {
        try {
            List<String> brokerUrls = pulsarAdmin.brokers()
                    .getActiveBrokers(pulsarConfig.getClusterName());

            return brokerUrls.stream()
                    .map(this::getBrokerInfo)
                    .collect(Collectors.toList());

        } catch (PulsarAdminException e) {
            log.error("Failed to get brokers", e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get brokers", e);
        }
    }

    /**
     * Get broker info by URL
     */
    private com.pulsar.diagnostic.common.model.BrokerInfo getBrokerInfo(String brokerUrl) {
        com.pulsar.diagnostic.common.model.BrokerInfo.BrokerMetrics metrics =
                com.pulsar.diagnostic.common.model.BrokerInfo.BrokerMetrics.builder().build();

        return com.pulsar.diagnostic.common.model.BrokerInfo.builder()
                .brokerId(brokerUrl)
                .brokerUrl(brokerUrl)
                .healthStatus(HealthStatus.HEALTHY)
                .metrics(metrics)
                .build();
    }

    /**
     * Get broker namespaces
     */
    public List<String> getBrokerNamespaces(String brokerUrl) {
        try {
            Map<String, NamespaceOwnershipStatus> ownershipMap =
                    pulsarAdmin.brokers().getOwnedNamespaces(pulsarConfig.getClusterName(), brokerUrl);
            return new ArrayList<>(ownershipMap.keySet());
        } catch (PulsarAdminException e) {
            log.error("Failed to get broker namespaces for: {}", brokerUrl, e);
            return Collections.emptyList();
        }
    }

    // ==================== Bookie Operations ====================

    /**
     * Get all bookies
     */
    @Timed(value = "pulsar.admin.bookies.list", description = "Time to list bookies")
    @Cacheable(value = "bookies", key = "'all-bookies'")
    public List<com.pulsar.diagnostic.common.model.BookieInfo> getBookies() {
        try {
            List<String> bookieIds = new ArrayList<>();

            try {
                BookiesRackConfiguration bookiesConf = pulsarAdmin.bookies().getBookiesRackInfo();
                bookieIds.addAll(bookiesConf.keySet());
            } catch (Exception e) {
                log.debug("Could not get bookies rack info", e);
            }

            return bookieIds.stream()
                    .map(this::mapBookieInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get bookies", e);
            return Collections.emptyList();
        }
    }

    /**
     * Map bookie ID to BookieInfo
     */
    private com.pulsar.diagnostic.common.model.BookieInfo mapBookieInfo(String bookieId) {
        return com.pulsar.diagnostic.common.model.BookieInfo.builder()
                .bookieId(bookieId)
                .healthStatus(HealthStatus.HEALTHY)
                .build();
    }

    // ==================== Tenant Operations ====================

    /**
     * Get all tenants
     */
    @Timed(value = "pulsar.admin.tenants.list", description = "Time to list tenants")
    @Cacheable(value = "tenants", key = "'all-tenants'")
    public List<String> getTenants() {
        try {
            return pulsarAdmin.tenants().getTenants();
        } catch (PulsarAdminException e) {
            log.error("Failed to get tenants", e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get tenants", e);
        }
    }

    /**
     * Get tenant info
     */
    public TenantInfo getTenantInfo(String tenant) {
        try {
            return pulsarAdmin.tenants().getTenantInfo(tenant);
        } catch (PulsarAdminException e) {
            log.error("Failed to get tenant info for: {}", tenant, e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get tenant info: " + tenant, e);
        }
    }

    // ==================== Namespace Operations ====================

    /**
     * Get all namespaces
     */
    @Timed(value = "pulsar.admin.namespaces.list", description = "Time to list all namespaces")
    @Cacheable(value = "namespaces", key = "'all-namespaces'")
    public List<String> getNamespaces() {
        List<String> tenants = getTenants();
        List<String> namespaces = new ArrayList<>();

        for (String tenant : tenants) {
            try {
                List<String> tenantNamespaces = pulsarAdmin.namespaces()
                        .getNamespaces(tenant);
                namespaces.addAll(tenantNamespaces);
            } catch (PulsarAdminException e) {
                log.warn("Failed to get namespaces for tenant: {}", tenant);
            }
        }

        return namespaces;
    }

    /**
     * Get namespaces for a tenant
     */
    public List<String> getNamespaces(String tenant) {
        try {
            return pulsarAdmin.namespaces().getNamespaces(tenant);
        } catch (PulsarAdminException e) {
            log.error("Failed to get namespaces for tenant: {}", tenant, e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get namespaces for tenant: " + tenant, e);
        }
    }

    /**
     * Get namespace info
     */
    public com.pulsar.diagnostic.common.model.NamespaceInfo getNamespaceInfo(String namespace) {
        try {
            NamespaceName nsName = NamespaceName.get(namespace);
            Policies policies = pulsarAdmin.namespaces().getPolicies(namespace);

            int replicationClusters = 1;
            if (policies.replication_clusters != null) {
                replicationClusters = policies.replication_clusters.size();
            }

            boolean encryptionRequired = policies.encryption_required;

            return com.pulsar.diagnostic.common.model.NamespaceInfo.builder()
                    .namespace(namespace)
                    .tenant(nsName.getTenant())
                    .localName(nsName.getLocalName())
                    .replicationClusters(replicationClusters)
                    .encryptionRequired(encryptionRequired)
                    .build();

        } catch (PulsarAdminException e) {
            log.error("Failed to get namespace info for: {}", namespace, e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get namespace info: " + namespace, e);
        }
    }

    // ==================== Topic Operations ====================

    /**
     * Get topics in a namespace
     */
    @Timed(value = "pulsar.admin.topics.list", description = "Time to list topics")
    public List<String> getTopics(String namespace) {
        try {
            return pulsarAdmin.topics().getList(namespace);
        } catch (PulsarAdminException e) {
            log.error("Failed to get topics for namespace: {}", namespace, e);
            throw new com.pulsar.diagnostic.common.exception.PulsarAdminException("Failed to get topics for namespace: " + namespace, e);
        }
    }

    /**
     * Get topic info with actual statistics from Pulsar Admin API
     */
    @Timed(value = "pulsar.admin.topic.info", description = "Time to get topic info")
    @Cacheable(value = "topicInfo", key = "#topic", unless = "#result == null")
    public com.pulsar.diagnostic.common.model.TopicInfo getTopicInfo(String topic) {
        TopicName topicName = TopicName.get(topic);

        try {
            // Get partition metadata
            int partitionCount = 1;
            try {
                PartitionedTopicStats partitionMetadata = pulsarAdmin.topics().getPartitionedStats(topic, false);
                if (partitionMetadata != null && partitionMetadata.getMetadata() != null) {
                    partitionCount = partitionMetadata.getMetadata().partitions > 0
                            ? partitionMetadata.getMetadata().partitions : 1;
                }
            } catch (PulsarAdminException e) {
                // Non-partitioned topic, use default partition count of 1
                log.debug("Topic {} is non-partitioned or metadata not available", topic);
            }

            // Get topic statistics
            long backlogSize = 0;
            long storageSize = 0;
            int producerCount = 0;
            int consumerCount = 0;
            int subscriptionCount = 0;
            double msgRateIn = 0;
            double msgRateOut = 0;
            double bytesRateIn = 0;
            double bytesRateOut = 0;

            try {
                var stats = pulsarAdmin.topics().getStats(topic);

                if (stats != null) {
                    backlogSize = stats.getBacklogSize();
                    storageSize = stats.getStorageSize();
                    producerCount = stats.getPublishers() != null ? stats.getPublishers().size() : 0;
                    subscriptionCount = stats.getSubscriptions() != null ? stats.getSubscriptions().size() : 0;
                    msgRateIn = stats.getMsgRateIn();
                    msgRateOut = stats.getMsgRateOut();
                    bytesRateIn = stats.getMsgThroughputIn();
                    bytesRateOut = stats.getMsgThroughputOut();

                    // Count total consumers across all subscriptions
                    if (stats.getSubscriptions() != null) {
                        for (var sub : stats.getSubscriptions().values()) {
                            consumerCount += sub.getConsumers() != null ? sub.getConsumers().size() : 0;
                        }
                    }
                }
            } catch (PulsarAdminException e) {
                log.warn("Could not get stats for topic {}: {}", topic, e.getMessage());
            }

            // Get subscription names
            List<String> subscriptionNames = getSubscriptionNames(topic);

            // Build topic stats
            com.pulsar.diagnostic.common.model.TopicInfo.TopicStats topicStats =
                    com.pulsar.diagnostic.common.model.TopicInfo.TopicStats.builder()
                            .messagesInRate(msgRateIn)
                            .messagesOutRate(msgRateOut)
                            .throughputIn(bytesRateIn)
                            .throughputOut(bytesRateOut)
                            .averageMessageSize(storageSize > 0 ? storageSize : 0)
                            .build();

            return com.pulsar.diagnostic.common.model.TopicInfo.builder()
                    .topic(topic)
                    .name(topicName.getLocalName())
                    .namespace(topicName.getNamespaceObject().toString())
                    .tenant(topicName.getTenant())
                    .persistent(topicName.isPersistent())
                    .partitions(partitionCount)
                    .backlogSize(backlogSize)
                    .messageCount(backlogSize) // backlog approximates message count
                    .storageSize(storageSize)
                    .producerCount(producerCount)
                    .consumerCount(consumerCount)
                    .subscriptionCount(subscriptionCount)
                    .subscriptions(subscriptionNames)
                    .stats(topicStats)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get topic info for: {}", topic, e);
            // Return minimal info on error
            return com.pulsar.diagnostic.common.model.TopicInfo.builder()
                    .topic(topic)
                    .name(topicName.getLocalName())
                    .namespace(topicName.getNamespaceObject().toString())
                    .tenant(topicName.getTenant())
                    .persistent(topicName.isPersistent())
                    .partitions(1)
                    .backlogSize(0)
                    .messageCount(0)
                    .storageSize(0)
                    .producerCount(0)
                    .consumerCount(0)
                    .subscriptionCount(0)
                    .subscriptions(Collections.emptyList())
                    .stats(com.pulsar.diagnostic.common.model.TopicInfo.TopicStats.builder().build())
                    .build();
        }
    }

    /**
     * Get topic stats - with actual data
     */
    @Timed(value = "pulsar.admin.topic.stats", description = "Time to get topic stats")
    @Cacheable(value = "topicStats", key = "#topic")
    public com.pulsar.diagnostic.common.model.TopicInfo.TopicStats getTopicStats(String topic) {
        try {
            var stats = pulsarAdmin.topics().getStats(topic);

            return com.pulsar.diagnostic.common.model.TopicInfo.TopicStats.builder()
                    .messagesInRate(stats.getMsgRateIn())
                    .messagesOutRate(stats.getMsgRateOut())
                    .throughputIn(stats.getMsgThroughputIn())
                    .throughputOut(stats.getMsgThroughputOut())
                    .averageMessageSize((long) stats.getAverageMsgSize())
                    .build();
        } catch (PulsarAdminException e) {
            log.error("Failed to get topic stats for: {}", topic, e);
            return com.pulsar.diagnostic.common.model.TopicInfo.TopicStats.builder().build();
        }
    }

    /**
     * Get subscription names for a topic
     */
    private List<String> getSubscriptionNames(String topic) {
        try {
            List<String> subscriptionNames = pulsarAdmin.topics().getSubscriptions(topic);
            return subscriptionNames != null ? subscriptionNames : Collections.emptyList();
        } catch (PulsarAdminException e) {
            log.error("Failed to get subscriptions for topic: {}", topic, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get subscriptions for a topic with actual data
     */
    @Timed(value = "pulsar.admin.topic.subscriptions", description = "Time to get topic subscriptions")
    @Cacheable(value = "topicSubscriptions", key = "#topic")
    public List<com.pulsar.diagnostic.common.model.SubscriptionInfo> getSubscriptions(String topic) {
        try {
            List<String> subscriptionNames = pulsarAdmin.topics().getSubscriptions(topic);
            if (subscriptionNames == null) {
                return Collections.emptyList();
            }

            List<com.pulsar.diagnostic.common.model.SubscriptionInfo> subscriptions = new ArrayList<>();

            // Get stats to fetch subscription details
            var stats = pulsarAdmin.topics().getStats(topic);

            for (String subName : subscriptionNames) {
                long msgBacklog = 0;
                int consumerCount = 0;
                String subType = "Unknown";

                if (stats != null && stats.getSubscriptions() != null) {
                    var subStats = stats.getSubscriptions().get(subName);
                    if (subStats != null) {
                        msgBacklog = subStats.getMsgBacklog();
                        consumerCount = subStats.getConsumers() != null ? subStats.getConsumers().size() : 0;
                        subType = subStats.getType() != null ? subStats.getType().toString() : "Unknown";
                    }
                }

                subscriptions.add(com.pulsar.diagnostic.common.model.SubscriptionInfo.builder()
                        .subscriptionName(subName)
                        .topic(topic)
                        .type(subType)
                        .messageCount(msgBacklog)
                        .consumerCount(consumerCount)
                        .build());
            }

            return subscriptions;

        } catch (PulsarAdminException e) {
            log.error("Failed to get subscriptions for topic: {}", topic, e);
            return Collections.emptyList();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Determine cluster health status
     */
    private HealthStatus determineClusterHealth(
            List<com.pulsar.diagnostic.common.model.BrokerInfo> brokers,
            List<com.pulsar.diagnostic.common.model.BookieInfo> bookies) {
        if (brokers.isEmpty()) {
            return HealthStatus.CRITICAL;
        }

        long unhealthyBrokers = brokers.stream()
                .filter(b -> b.getHealthStatus() != HealthStatus.HEALTHY)
                .count();

        if (unhealthyBrokers > 0) {
            return unhealthyBrokers == brokers.size() ? HealthStatus.CRITICAL : HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * Collect cluster statistics
     */
    private com.pulsar.diagnostic.common.model.PulsarCluster.ClusterStats collectClusterStats() {
        try {
            List<String> tenants = getTenants();
            List<String> namespaces = getNamespaces();

            int activeBrokerCount = 0;
            try {
                activeBrokerCount = pulsarAdmin.brokers()
                        .getActiveBrokers(pulsarConfig.getClusterName()).size();
            } catch (Exception e) {
                log.debug("Could not get active broker count");
            }

            return com.pulsar.diagnostic.common.model.PulsarCluster.ClusterStats.builder()
                    .totalTenants(tenants.size())
                    .totalNamespaces(namespaces.size())
                    .activeBrokers(activeBrokerCount)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to collect cluster stats", e);
            return com.pulsar.diagnostic.common.model.PulsarCluster.ClusterStats.builder().build();
        }
    }
}