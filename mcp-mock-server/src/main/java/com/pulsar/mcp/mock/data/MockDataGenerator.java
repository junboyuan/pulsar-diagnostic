package com.pulsar.mcp.mock.data;

import com.pulsar.mcp.mock.config.MockDataConfig;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generates dynamic mock data for various Pulsar diagnostic scenarios.
 */
@Component
public class MockDataGenerator {

    private final MockDataConfig config;
    private final Random random = new Random();

    // Dynamic state
    private volatile long backlogMessages = 1000;
    private volatile double brokerCpuUsage = 45.0;
    private volatile double brokerMemoryUsage = 60.0;
    private volatile double messagesInRate = 1000.0;
    private volatile double messagesOutRate = 950.0;
    private volatile int activeConnections = 100;

    public MockDataGenerator(MockDataConfig config) {
        this.config = config;
    }

    /**
     * Update dynamic data periodically.
     */
    public void updateDynamicData() {
        if (!config.isDynamicData()) {
            return;
        }

        String scenario = config.getScenario();

        switch (scenario) {
            case "backlog":
                // Increase backlog
                backlogMessages += random.nextInt(100) + 10;
                break;
            case "produce-slow":
                // High CPU, low message rate
                brokerCpuUsage = 70 + random.nextDouble() * 20;
                messagesInRate = Math.max(100, messagesInRate - random.nextDouble() * 50);
                break;
            case "disk-full":
                // High disk usage
                brokerMemoryUsage = 85 + random.nextDouble() * 10;
                break;
            default:
                // Normal fluctuations
                backlogMessages = Math.max(0, backlogMessages + (random.nextInt(100) - 50));
                brokerCpuUsage = 40 + random.nextDouble() * 30;
                brokerMemoryUsage = 50 + random.nextDouble() * 25;
                messagesInRate = 800 + random.nextDouble() * 400;
                messagesOutRate = messagesInRate * (0.9 + random.nextDouble() * 0.15);
                activeConnections = 80 + random.nextInt(40);
        }
    }

    // ============== Cluster Info ==============

    public Map<String, Object> generateClusterInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("clusterName", "pulsar-cluster-standalone");
        info.put("serviceUrl", "pulsar://localhost:6650");
        info.put("adminUrl", "http://localhost:8080");
        info.put("brokerServiceUrl", "pulsar://localhost:6650");
        info.put("status", "healthy");
        info.put("brokersCount", 1);
        info.put("bookiesCount", 1);
        info.put("namespacesCount", 5);
        info.put("topicsCount", 25);
        return info;
    }

    // ============== Broker Metrics ==============

    public Map<String, Object> generateBrokerMetrics() {
        updateDynamicData();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("brokerId", "broker-1");
        metrics.put("brokerUrl", "pulsar://localhost:6650");
        metrics.put("status", "active");

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("cpuUsage", String.format("%.2f%%", brokerCpuUsage));
        system.put("memoryUsage", String.format("%.2f%%", brokerMemoryUsage));
        system.put("directMemoryUsage", String.format("%.2f%%", 30 + random.nextDouble() * 20));
        system.put("openFileDescriptors", 1000 + random.nextInt(500));
        system.put("maxFileDescriptors", 100000);
        metrics.put("systemMetrics", system);

        Map<String, Object> msgMetrics = new LinkedHashMap<>();
        msgMetrics.put("messagesInPerSecond", String.format("%.2f", messagesInRate));
        msgMetrics.put("messagesOutPerSecond", String.format("%.2f", messagesOutRate));
        msgMetrics.put("bytesInPerSecond", String.format("%.2f KB", messagesInRate * 0.5));
        msgMetrics.put("bytesOutPerSecond", String.format("%.2f KB", messagesOutRate * 0.5));
        metrics.put("messageMetrics", msgMetrics);

        metrics.put("activeConnections", activeConnections);
        metrics.put("totalProducers", 15 + random.nextInt(10));
        metrics.put("totalConsumers", 20 + random.nextInt(15));
        metrics.put("totalTopics", 25 + random.nextInt(10));

        return metrics;
    }

    // ============== Topic Metrics ==============

    public Map<String, Object> generateTopicMetrics(String topic) {
        updateDynamicData();

        String topicName = topic != null ? topic : "persistent://public/default/test-topic";

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("topic", topicName);
        metrics.put("partitions", 0);
        metrics.put("status", "active");

        Map<String, Object> producers = new LinkedHashMap<>();
        producers.put("count", 3 + random.nextInt(5));
        producers.put("messagesRateIn", String.format("%.2f", messagesInRate / 3));
        producers.put("averageLatency", String.format("%.2f ms", 5 + random.nextDouble() * 10));
        metrics.put("producers", producers);

        Map<String, Object> consumers = new LinkedHashMap<>();
        consumers.put("count", 5 + random.nextInt(5));
        consumers.put("messagesRateOut", String.format("%.2f", messagesOutRate / 5));
        consumers.put("averageLatency", String.format("%.2f ms", 10 + random.nextDouble() * 15));
        metrics.put("consumers", consumers);

        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("size", String.format("%.2f MB", 50 + random.nextDouble() * 100));
        storage.put("backlogSize", String.format("%.2f MB", backlogMessages * 0.001));
        storage.put("messageTTL", "3600s");
        storage.put("retentionSize", "1GB");
        storage.put("retentionTime", "7d");
        metrics.put("storage", storage);

        return metrics;
    }

    // ============== Topic Backlog ==============

    public Map<String, Object> generateTopicBacklog(String topic) {
        updateDynamicData();

        String topicName = topic != null ? topic : "persistent://public/default/test-topic";
        String scenario = config.getScenario();

        // Adjust backlog based on scenario
        long currentBacklog = backlogMessages;
        if ("backlog".equals(scenario)) {
            currentBacklog = backlogMessages + 5000;
        }

        Map<String, Object> backlog = new LinkedHashMap<>();
        backlog.put("topic", topicName);
        backlog.put("totalBacklog", currentBacklog);
        backlog.put("backlogSize", String.format("%.2f MB", currentBacklog * 0.001));

        List<Map<String, Object>> subscriptions = new ArrayList<>();
        String[] subNames = {"sub-1", "sub-2", "sub-3"};

        for (String subName : subNames) {
            Map<String, Object> sub = new LinkedHashMap<>();
            sub.put("subscription", subName);
            sub.put("type", random.nextBoolean() ? "Shared" : "Failover");
            sub.put("backlog", currentBacklog / 3 + random.nextInt(500));
            sub.put("msgOutRate", String.format("%.2f", messagesOutRate / 3));
            sub.put("msgAckRate", String.format("%.2f", messagesOutRate / 3 * 0.95));
            sub.put("unackedMessages", random.nextInt(100));
            subscriptions.add(sub);
        }
        backlog.put("subscriptions", subscriptions);

        return backlog;
    }

    // ============== Consumer Stats ==============

    public Map<String, Object> generateConsumerStats(String topic) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("topic", topicName);
        stats.put("totalConsumers", 5 + random.nextInt(10));

        List<Map<String, Object>> consumers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> consumer = new LinkedHashMap<>();
            consumer.put("consumerId", "consumer-" + i);
            consumer.put("consumerName", "app-consumer-" + i);
            consumer.put("subscription", "sub-" + i);
            consumer.put("address", "192.168.1." + (100 + i) + ":5" + (i * 1000));
            consumer.put("connectedSince", "2024-01-01T00:00:00Z");
            consumer.put("messagesRateOut", String.format("%.2f", 50 + random.nextDouble() * 100));
            consumer.put("messagesAcked", 10000 + random.nextInt(5000));
            consumer.put("availablePermits", 1000);
            consumer.put("unackedMessages", random.nextInt(50));
            consumer.put("avgMessagesPerBatch", 5 + random.nextInt(10));
            consumers.add(consumer);
        }
        stats.put("consumers", consumers);

        return stats;
    }

    // ============== Producer Stats ==============

    public Map<String, Object> generateProducerStats(String topic) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";
        String scenario = config.getScenario();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("topic", topicName);
        stats.put("totalProducers", 3 + random.nextInt(5));

        List<Map<String, Object>> producers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> producer = new LinkedHashMap<>();
            producer.put("producerId", "producer-" + i);
            producer.put("producerName", "app-producer-" + i);
            producer.put("address", "192.168.1." + (50 + i) + ":4" + (i * 1000));
            producer.put("connectedSince", "2024-01-01T00:00:00Z");

            double msgRate = 30 + random.nextDouble() * 70;
            if ("produce-slow".equals(scenario)) {
                msgRate = msgRate * 0.3; // Reduced rate
            }
            producer.put("messagesRateIn", String.format("%.2f", msgRate));
            producer.put("messagesPublished", 50000 + random.nextInt(10000));
            producer.put("averageLatency", "produce-slow".equals(scenario)
                    ? String.format("%.2f ms", 100 + random.nextDouble() * 200)
                    : String.format("%.2f ms", 5 + random.nextDouble() * 15));
            producer.put("averageBatchSize", 5 + random.nextInt(10));
            producers.add(producer);
        }
        stats.put("producers", producers);

        return stats;
    }

    // ============== Disk Space ==============

    public Map<String, Object> generateDiskSpace() {
        String scenario = config.getScenario();
        double usedPercentage = 60 + random.nextDouble() * 15;
        if ("disk-full".equals(scenario)) {
            usedPercentage = 90 + random.nextDouble() * 8;
        }

        Map<String, Object> disk = new LinkedHashMap<>();
        disk.put("totalSpace", "500 GB");
        disk.put("usedSpace", String.format("%.2f GB", 500 * usedPercentage / 100));
        disk.put("freeSpace", String.format("%.2f GB", 500 * (100 - usedPercentage) / 100));
        disk.put("usagePercentage", String.format("%.2f%%", usedPercentage));
        disk.put("status", usedPercentage > 85 ? "warning" : "normal");

        // Bookie disk info
        List<Map<String, Object>> bookies = new ArrayList<>();
        Map<String, Object> bookie = new LinkedHashMap<>();
        bookie.put("bookieId", "bookie-1");
        bookie.put("address", "192.168.1.10:3181");
        bookie.put("status", "healthy");
        bookie.put("diskUsage", String.format("%.2f%%", usedPercentage));
        bookies.add(bookie);
        disk.put("bookies", bookies);

        return disk;
    }

    // ============== Auth Config ==============

    public Map<String, Object> generateAuthConfig() {
        String scenario = config.getScenario();

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("authEnabled", true);
        auth.put("authenticationProvider", "org.apache.pulsar.broker.authentication.AuthenticationProviderToken");
        auth.put("authorizationProvider", "org.apache.pulsar.broker.authorization.PulsarAuthorizationProvider");

        Map<String, Object> tokenConfig = new LinkedHashMap<>();
        tokenConfig.put("tokenAuthEnabled", true);
        tokenConfig.put("tokenPublicKey", "file:///etc/pulsar/token/public.key");
        tokenConfig.put("tokenSecretKey", "file:///etc/pulsar/token/secret.key");
        auth.put("tokenConfig", tokenConfig);

        if ("auth-failure".equals(scenario)) {
            auth.put("status", "error");
            auth.put("errorMessage", "Token validation failed: expired token");
            auth.put("lastError", "2024-01-01T12:00:00Z");
        } else {
            auth.put("status", "configured");
        }

        return auth;
    }

    // ============== Permissions ==============

    public Map<String, Object> generatePermissions(String namespace) {
        String ns = namespace != null ? namespace : "public/default";

        Map<String, Object> perms = new LinkedHashMap<>();
        perms.put("namespace", ns);

        Map<String, Object> namespacePerms = new LinkedHashMap<>();
        namespacePerms.put("admin", List.of("consume", "produce", "functions"));
        namespacePerms.put("producer", List.of("produce"));
        namespacePerms.put("consumer", List.of("consume"));
        perms.put("namespacePermissions", namespacePerms);

        Map<String, Object> topicPerms = new LinkedHashMap<>();
        topicPerms.put("admin", List.of("consume", "produce"));
        topicPerms.put("app-producer", List.of("produce"));
        topicPerms.put("app-consumer", List.of("consume"));
        perms.put("topicPermissions", topicPerms);

        return perms;
    }

    // ============== Subscription Stats ==============

    public Map<String, Object> generateSubscriptionStats(String topic) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("topic", topicName);
        stats.put("totalSubscriptions", 3);

        List<Map<String, Object>> subscriptions = new ArrayList<>();
        String[] types = {"Shared", "Failover", "Exclusive"};

        for (int i = 0; i < 3; i++) {
            Map<String, Object> sub = new LinkedHashMap<>();
            sub.put("subscription", "subscription-" + (i + 1));
            sub.put("type", types[i]);
            sub.put("activeConsumers", i == 2 ? 1 : 2 + random.nextInt(3));
            sub.put("msgBacklog", 100 + random.nextInt(500));
            sub.put("msgOutRate", String.format("%.2f", 30 + random.nextDouble() * 50));
            sub.put("msgAckRate", String.format("%.2f", 28 + random.nextDouble() * 48));
            sub.put("unackedMessages", random.nextInt(100));
            sub.put("replicated", false);
            subscriptions.add(sub);
        }
        stats.put("subscriptions", subscriptions);

        return stats;
    }

    // ============== DLQ Stats ==============

    public Map<String, Object> generateDlqStats(String topic) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("topic", topicName);
        stats.put("dlqEnabled", true);
        stats.put("dlqTopic", topicName + "-dlq");

        Map<String, Object> dlqInfo = new LinkedHashMap<>();
        dlqInfo.put("topic", topicName + "-dlq");
        dlqInfo.put("subscriptions", 1);
        dlqInfo.put("backlog", 50 + random.nextInt(200));
        dlqInfo.put("producers", 1);
        dlqInfo.put("consumers", 1);
        dlqInfo.put("messageRateIn", String.format("%.2f", 0.1 + random.nextDouble() * 2));
        stats.put("dlqInfo", dlqInfo);

        // Recent DLQ messages
        List<Map<String, Object>> recentMessages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("messageId", String.valueOf(i + 1));
            msg.put("originalTopic", topicName);
            msg.put("originalSubscription", "sub-1");
            msg.put("reason", "Max redelivery count exceeded");
            msg.put("timestamp", "2024-01-01T12:00:0" + i + "Z");
            recentMessages.add(msg);
        }
        stats.put("recentMessages", recentMessages);

        return stats;
    }

    // ============== Cluster Metrics ==============

    public Map<String, Object> generateClusterMetrics() {
        updateDynamicData();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("clusterName", "pulsar-cluster-standalone");
        metrics.put("timestamp", System.currentTimeMillis());

        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("totalBrokers", 1);
        resources.put("activeBrokers", 1);
        resources.put("totalBookies", 1);
        resources.put("activeBookies", 1);
        resources.put("totalTopics", 25);
        resources.put("totalNamespaces", 5);
        metrics.put("resources", resources);

        Map<String, Object> throughput = new LinkedHashMap<>();
        throughput.put("messagesInRate", String.format("%.2f msg/s", messagesInRate));
        throughput.put("messagesOutRate", String.format("%.2f msg/s", messagesOutRate));
        throughput.put("bytesInRate", String.format("%.2f MB/s", messagesInRate * 0.5 / 1024));
        throughput.put("bytesOutRate", String.format("%.2f MB/s", messagesOutRate * 0.5 / 1024));
        metrics.put("throughput", throughput);

        Map<String, Object> latency = new LinkedHashMap<>();
        latency.put("avgPublishLatency", String.format("%.2f ms", 5 + random.nextDouble() * 10));
        latency.put("avgEndToEndLatency", String.format("%.2f ms", 15 + random.nextDouble() * 20));
        latency.put("p99PublishLatency", String.format("%.2f ms", 20 + random.nextDouble() * 30));
        latency.put("p99EndToEndLatency", String.format("%.2f ms", 50 + random.nextDouble() * 50));
        metrics.put("latency", latency);

        return metrics;
    }

    // ============== Resource Usage ==============

    public Map<String, Object> generateResourceUsage() {
        updateDynamicData();

        String scenario = config.getScenario();
        double cpuUsage = brokerCpuUsage;
        double memUsage = brokerMemoryUsage;

        if ("produce-slow".equals(scenario)) {
            cpuUsage = 85 + random.nextDouble() * 10;
        } else if ("disk-full".equals(scenario)) {
            memUsage = 88 + random.nextDouble() * 10;
        }

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("timestamp", System.currentTimeMillis());

        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("usage", String.format("%.2f%%", cpuUsage));
        cpu.put("status", cpuUsage > 80 ? "warning" : "normal");
        cpu.put("cores", 8);
        usage.put("cpu", cpu);

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usage", String.format("%.2f%%", memUsage));
        memory.put("status", memUsage > 85 ? "warning" : "normal");
        memory.put("totalMemory", "16 GB");
        memory.put("usedMemory", String.format("%.2f GB", 16 * memUsage / 100));
        memory.put("directMemory", String.format("%.2f GB", 2 + random.nextDouble() * 2));
        usage.put("memory", memory);

        Map<String, Object> network = new LinkedHashMap<>();
        network.put("bandwidthIn", String.format("%.2f Mbps", 100 + random.nextDouble() * 50));
        network.put("bandwidthOut", String.format("%.2f Mbps", 80 + random.nextDouble() * 40));
        network.put("connections", activeConnections);
        usage.put("network", network);

        return usage;
    }

    // ============== Topic Info ==============

    public Map<String, Object> generateTopicInfo(String topic) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("topic", topicName);
        info.put("namespace", extractNamespace(topicName));
        info.put("localName", extractLocalName(topicName));
        info.put("persistent", topicName.startsWith("persistent://"));
        info.put("partitioned", false);
        info.put("partitions", 0);
        info.put("status", "active");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("messageTTL", 3600);
        config.put("retentionSize", 1073741824L); // 1GB
        config.put("retentionTime", 604800); // 7 days
        config.put("maxMessageSize", 5242880); // 5MB
        config.put("compaction", true);
        config.put("deduplication", false);
        info.put("config", config);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("producers", 3);
        stats.put("consumers", 5);
        stats.put("subscriptions", 3);
        stats.put("storageSize", String.format("%.2f MB", 50 + random.nextDouble() * 50));
        info.put("stats", stats);

        return info;
    }

    // ============== Topic Config ==============

    public Map<String, Object> generateTopicConfig(String topic) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("topic", topicName);
        config.put("messageTTL", 3600);
        config.put("retentionPolicies", Map.of(
                "retentionSizeInMB", 1024,
                "retentionTimeInMinutes", 10080
        ));
        config.put("backlogQuota", Map.of(
                "limit", 10737418240L, // 10GB
                "policy", "producer_request_hold"
        ));
        config.put("compactionThreshold", 1048576);
        config.put("delayedDeliveryEnabled", false);
        config.put("delayedDeliveryTickTime", 1000);
        config.put("deduplicationEnabled", false);
        config.put("maxMessageSize", 5242880);
        config.put("maxUnackedMessagesPerConsumer", 50000);
        config.put("maxUnackedMessagesPerSubscription", 200000);

        return config;
    }

    // ============== Lists ==============

    public List<String> generateBrokerList() {
        return List.of(
                "broker-1: pulsar://localhost:6650 (active)",
                "broker-2: pulsar://localhost:6651 (standby)"
        );
    }

    public List<String> generateTopicList(String namespace) {
        String ns = namespace != null ? namespace : "public/default";
        return List.of(
                "persistent://" + ns + "/test-topic",
                "persistent://" + ns + "/orders-topic",
                "persistent://" + ns + "/notifications-topic",
                "persistent://" + ns + "/events-topic",
                "non-persistent://" + ns + "/metrics-topic"
        );
    }

    // ============== Inspection ==============

    public Map<String, Object> generateClusterInspection(List<String> components) {
        Map<String, Object> inspection = new LinkedHashMap<>();
        inspection.put("timestamp", System.currentTimeMillis());
        inspection.put("cluster", "pulsar-cluster-standalone");

        if (components == null || components.isEmpty() || components.contains("brokers")) {
            Map<String, Object> brokers = new LinkedHashMap<>();
            brokers.put("status", "healthy");
            brokers.put("totalCount", 1);
            brokers.put("activeCount", 1);
            brokers.put("details", generateBrokerList());
            inspection.put("brokers", brokers);
        }

        if (components == null || components.isEmpty() || components.contains("bookies")) {
            Map<String, Object> bookies = new LinkedHashMap<>();
            bookies.put("status", "healthy");
            bookies.put("totalCount", 1);
            bookies.put("writableCount", 1);
            inspection.put("bookies", bookies);
        }

        if (components == null || components.isEmpty() || components.contains("topics")) {
            Map<String, Object> topics = new LinkedHashMap<>();
            topics.put("status", "healthy");
            topics.put("totalCount", 25);
            topics.put("partitionedCount", 5);
            inspection.put("topics", topics);
        }

        if (components == null || components.isEmpty() || components.contains("namespaces")) {
            Map<String, Object> namespaces = new LinkedHashMap<>();
            namespaces.put("status", "healthy");
            namespaces.put("totalCount", 5);
            inspection.put("namespaces", namespaces);
        }

        return inspection;
    }

    public Map<String, Object> generateSubscriptionCheck(String topic, String subscription) {
        String topicName = topic != null ? topic : "persistent://public/default/test-topic";
        String subName = subscription != null ? subscription : "subscription-1";

        Map<String, Object> check = new LinkedHashMap<>();
        check.put("topic", topicName);
        check.put("subscription", subName);
        check.put("exists", true);
        check.put("type", "Shared");
        check.put("activeConsumers", 2);
        check.put("backlog", 100 + random.nextInt(500));
        check.put("unackedMessages", random.nextInt(50));
        check.put("msgOutRate", String.format("%.2f", 30 + random.nextDouble() * 50));
        check.put("msgAckRate", String.format("%.2f", 28 + random.nextDouble() * 48));
        check.put("status", "healthy");

        return check;
    }

    // ============== Helper Methods ==============

    private String extractNamespace(String topic) {
        if (topic == null || !topic.contains("://")) {
            return "public/default";
        }
        String[] parts = topic.split("://")[1].split("/");
        if (parts.length >= 2) {
            return parts[0] + "/" + parts[1];
        }
        return "public/default";
    }

    private String extractLocalName(String topic) {
        if (topic == null || !topic.contains("://")) {
            return topic;
        }
        String[] parts = topic.split("://")[1].split("/");
        if (parts.length >= 3) {
            return String.join("/", java.util.Arrays.copyOfRange(parts, 2, parts.length));
        }
        return topic;
    }
}