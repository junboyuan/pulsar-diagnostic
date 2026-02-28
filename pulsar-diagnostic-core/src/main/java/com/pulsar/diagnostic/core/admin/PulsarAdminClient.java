package com.pulsar.diagnostic.core.admin;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import com.pulsar.diagnostic.common.exception.PulsarAdminException;
import com.pulsar.diagnostic.common.model.*;
import com.pulsar.diagnostic.core.config.PulsarConfig;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.common.naming.NamespaceName;
import org.apache.pulsar.common.naming.TopicName;
import org.apache.pulsar.common.policies.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper for Pulsar Admin API operations
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
    public PulsarCluster getClusterInfo() {
        try {
            log.info("Fetching cluster info for: {}", pulsarConfig.getClusterName());

            List<BrokerInfo> brokers = getBrokers();
            List<BookieInfo> bookies = getBookies();

            HealthStatus healthStatus = determineClusterHealth(brokers, bookies);

            PulsarCluster.ClusterStats stats = collectClusterStats();

            return PulsarCluster.builder()
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
            throw new PulsarAdminException("Failed to get cluster info", e);
        }
    }

    /**
     * Check cluster health
     */
    public HealthStatus checkHealth() {
        try {
            // Try to get brokers list as a health check
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
    public List<BrokerInfo> getBrokers() {
        try {
            List<String> brokerUrls = pulsarAdmin.brokers()
                    .getActiveBrokers(pulsarConfig.getClusterName());

            return brokerUrls.stream()
                    .map(this::getBrokerInfo)
                    .collect(Collectors.toList());

        } catch (PulsarAdminException e) {
            log.error("Failed to get brokers", e);
            throw new PulsarAdminException("Failed to get brokers", e);
        }
    }

    /**
     * Get broker info by URL
     */
    private BrokerInfo getBrokerInfo(String brokerUrl) {
        try {
            BrokerInfo.BrokerMetrics metrics = BrokerInfo.BrokerMetrics.builder().build();

            return BrokerInfo.builder()
                    .brokerId(brokerUrl)
                    .brokerUrl(brokerUrl)
                    .healthStatus(HealthStatus.HEALTHY)
                    .metrics(metrics)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to get broker info for: {}", brokerUrl, e);
            return BrokerInfo.builder()
                    .brokerId(brokerUrl)
                    .brokerUrl(brokerUrl)
                    .healthStatus(HealthStatus.UNKNOWN)
                    .build();
        }
    }

    /**
     * Get broker namespaces
     */
    public List<String> getBrokerNamespaces(String brokerUrl) {
        try {
            return pulsarAdmin.brokers().getOwnedNamespaces(pulsarConfig.getClusterName(), brokerUrl);
        } catch (PulsarAdminException e) {
            log.error("Failed to get broker namespaces for: {}", brokerUrl, e);
            return Collections.emptyList();
        }
    }

    // ==================== Bookie Operations ====================

    /**
     * Get all bookies
     */
    public List<BookieInfo> getBookies() {
        try {
            BookiesList bookies = pulsarAdmin.bookies().getBookies();

            return bookies.getBookies().stream()
                    .map(this::mapBookieInfo)
                    .collect(Collectors.toList());

        } catch (PulsarAdminException e) {
            log.error("Failed to get bookies", e);
            return Collections.emptyList();
        }
    }

    /**
     * Map BookieRawInfo to BookieInfo
     */
    private BookieInfo mapBookieInfo(org.apache.pulsar.common.policies.data.BookieRawInfo rawInfo) {
        return BookieInfo.builder()
                .bookieId(rawInfo.getBookieId())
                .address(rawInfo.getAddress())
                .healthStatus(HealthStatus.HEALTHY)
                .build();
    }

    // ==================== Tenant Operations ====================

    /**
     * Get all tenants
     */
    public List<String> getTenants() {
        try {
            return pulsarAdmin.tenants().getTenants();
        } catch (PulsarAdminException e) {
            log.error("Failed to get tenants", e);
            throw new PulsarAdminException("Failed to get tenants", e);
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
            throw new PulsarAdminException("Failed to get tenant info: " + tenant, e);
        }
    }

    // ==================== Namespace Operations ====================

    /**
     * Get all namespaces
     */
    public List<String> getNamespaces() {
        try {
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
        } catch (PulsarAdminException e) {
            log.error("Failed to get namespaces", e);
            throw new PulsarAdminException("Failed to get namespaces", e);
        }
    }

    /**
     * Get namespaces for a tenant
     */
    public List<String> getNamespaces(String tenant) {
        try {
            return pulsarAdmin.namespaces().getNamespaces(tenant);
        } catch (PulsarAdminException e) {
            log.error("Failed to get namespaces for tenant: {}", tenant, e);
            throw new PulsarAdminException("Failed to get namespaces for tenant: " + tenant, e);
        }
    }

    /**
     * Get namespace info
     */
    public NamespaceInfo getNamespaceInfo(String namespace) {
        try {
            NamespaceName nsName = NamespaceName.get(namespace);
            Policies policies = pulsarAdmin.namespaces().getPolicies(namespace);

            return NamespaceInfo.builder()
                    .namespace(namespace)
                    .tenant(nsName.getTenant())
                    .localName(nsName.getLocalName())
                    .replicationClusters(policies.replication_clusters != null ?
                            policies.replication_clusters.size() : 1)
                    .encryptionRequired(policies.encryption_required != null ?
                            policies.encryption_required : false)
                    .build();

        } catch (PulsarAdminException e) {
            log.error("Failed to get namespace info for: {}", namespace, e);
            throw new PulsarAdminException("Failed to get namespace info: " + namespace, e);
        }
    }

    // ==================== Topic Operations ====================

    /**
     * Get topics in a namespace
     */
    public List<String> getTopics(String namespace) {
        try {
            return pulsarAdmin.topics().getList(namespace);
        } catch (PulsarAdminException e) {
            log.error("Failed to get topics for namespace: {}", namespace, e);
            throw new PulsarAdminException("Failed to get topics for namespace: " + namespace, e);
        }
    }

    /**
     * Get topic info
     */
    public TopicInfo getTopicInfo(String topic) {
        try {
            TopicName topicName = TopicName.get(topic);
            PersistentTopicStats stats = pulsarAdmin.topics().getStats(topic);
            PersistentTopicInternalStats internalStats = pulsarAdmin.topics().getInternalStats(topic);

            return TopicInfo.builder()
                    .topic(topic)
                    .name(topicName.getLocalName())
                    .namespace(topicName.getNamespaceObject().toString())
                    .tenant(topicName.getTenant())
                    .persistent(topicName.isPersistent())
                    .partitions(stats.partitions > 1 ? stats.partitions : 1)
                    .backlogSize(internalStats.totalSize)
                    .messageCount(stats.totalMsgCtr)
                    .storageSize(internalStats.totalSize)
                    .producerCount(stats.publishers != null ? stats.publishers.size() : 0)
                    .consumerCount(calculateTotalConsumers(stats))
                    .subscriptionCount(stats.subscriptions != null ? stats.subscriptions.size() : 0)
                    .subscriptions(stats.subscriptions != null ?
                            new ArrayList<>(stats.subscriptions.keySet()) : Collections.emptyList())
                    .stats(mapTopicStats(stats))
                    .build();

        } catch (PulsarAdminException e) {
            log.error("Failed to get topic info for: {}", topic, e);
            throw new PulsarAdminException("Failed to get topic info: " + topic, e);
        }
    }

    /**
     * Get topic stats
     */
    public TopicInfo.TopicStats getTopicStats(String topic) {
        try {
            PersistentTopicStats stats = pulsarAdmin.topics().getStats(topic);
            return mapTopicStats(stats);
        } catch (PulsarAdminException e) {
            log.error("Failed to get stats for topic: {}", topic, e);
            throw new PulsarAdminException("Failed to get topic stats: " + topic, e);
        }
    }

    /**
     * Get subscriptions for a topic
     */
    public List<SubscriptionInfo> getSubscriptions(String topic) {
        try {
            Map<String, SubscriptionStats> subs = pulsarAdmin.topics().getStats(topic).subscriptions;
            if (subs == null) {
                return Collections.emptyList();
            }

            return subs.entrySet().stream()
                    .map(entry -> SubscriptionInfo.builder()
                            .subscriptionName(entry.getKey())
                            .topic(topic)
                            .type(String.valueOf(entry.getValue().type))
                            .messageCount(entry.getValue().msgBacklog)
                            .consumerCount(entry.getValue().consumers != null ?
                                    entry.getValue().consumers.size() : 0)
                            .build())
                    .collect(Collectors.toList());

        } catch (PulsarAdminException e) {
            log.error("Failed to get subscriptions for topic: {}", topic, e);
            return Collections.emptyList();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Map Pulsar stats to our TopicStats model
     */
    private TopicInfo.TopicStats mapTopicStats(PersistentTopicStats stats) {
        return TopicInfo.TopicStats.builder()
                .messagesInRate(stats.msgInRate)
                .messagesOutRate(stats.msgOutRate)
                .throughputIn(stats.bytesInRate)
                .throughputOut(stats.bytesOutRate)
                .totalMessagesPublished(stats.totalMsgCtr)
                .build();
    }

    /**
     * Calculate total consumers from topic stats
     */
    private int calculateTotalConsumers(PersistentTopicStats stats) {
        if (stats.subscriptions == null) {
            return 0;
        }
        return stats.subscriptions.values().stream()
                .mapToInt(sub -> sub.consumers != null ? sub.consumers.size() : 0)
                .sum();
    }

    /**
     * Determine cluster health status
     */
    private HealthStatus determineClusterHealth(List<BrokerInfo> brokers, List<BookieInfo> bookies) {
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
    private PulsarCluster.ClusterStats collectClusterStats() {
        try {
            List<String> tenants = getTenants();
            List<String> namespaces = getNamespaces();

            return PulsarCluster.ClusterStats.builder()
                    .totalTenants(tenants.size())
                    .totalNamespaces(namespaces.size())
                    .activeBrokers(pulsarAdmin.brokers()
                            .getActiveBrokers(pulsarConfig.getClusterName()).size())
                    .build();

        } catch (Exception e) {
            log.warn("Failed to collect cluster stats", e);
            return PulsarCluster.ClusterStats.builder().build();
        }
    }
}