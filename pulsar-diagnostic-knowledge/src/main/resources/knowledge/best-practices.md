# Pulsar Best Practices

## Cluster Deployment

### Hardware Recommendations

**Broker Nodes:**
- CPU: 4-8 cores minimum, 16+ cores recommended for production
- Memory: 8GB minimum, 16-32GB recommended
- Disk: SSD recommended for journal, separate disks for ledger cache
- Network: 10Gbps+ recommended for production clusters

**BookKeeper Nodes:**
- CPU: 4-8 cores
- Memory: 8-16GB
- Disk: Multiple SSDs in RAID configuration for journal and ledger
- Journal disk: Dedicated high-IOPS SSD
- Ledger disk: Large capacity SSDs

**ZooKeeper Nodes:**
- CPU: 2-4 cores
- Memory: 4-8GB
- Disk: Dedicated SSD for transaction logs
- Deploy odd number of nodes (3, 5, or 7) for quorum

### Network Configuration

1. **Separate Networks:**
   - Use dedicated network for BookKeeper traffic
   - Separate client and internal traffic if possible

2. **Bandwidth:**
   - Ensure sufficient bandwidth for expected message throughput
   - Account for replication traffic

3. **Latency:**
   - Keep broker-bookie latency under 1ms for optimal performance
   - Co-locate in same data center/availability zone

---

## Topic Design

### Partitioning Strategy

**When to Partition:**
- Throughput requirements exceed single broker capacity
- Need parallel consumer processing
- Geographic distribution requirements

**Recommended Partition Count:**
```
partitions = max(expected_throughput_msgs_per_sec / 50000, num_brokers)
```

**Partition Guidelines:**
- Start with partition count = number of brokers
- Increase based on throughput requirements
- Consider consumer parallelism needs
- Avoid over-partitioning (management overhead)

### Topic Naming Conventions

**Format:** `persistent://tenant/namespace/topic`

**Naming Guidelines:**
- Use meaningful, descriptive names
- Follow a consistent pattern
- Include domain/context in name
- Consider using hierarchical naming

**Examples:**
- `persistent://ecommerce/orders/order-created`
- `persistent://analytics/events/page-views`
- `persistent://system/monitoring/metrics`

### Retention Policies

**Time-based Retention:**
```bash
# Set 7-day retention
pulsar-admin namespaces set-retention my-tenant/my-ns \
  --time 7d --size -1
```

**Size-based Retention:**
```bash
# Set 10GB size limit
pulsar-admin namespaces set-retention my-tenant/my-ns \
  --time -1 --size 10G
```

**Recommendations:**
- Use time-based for compliance requirements
- Use size-based for storage constraints
- Combine both for comprehensive control

---

## Subscription Management

### Subscription Types

**Exclusive:**
- Single consumer per subscription
- Use for ordered processing requirements
- Highest guarantee of message ordering

**Shared:**
- Multiple consumers share subscription
- Messages distributed among consumers
- Use for high-throughput, parallel processing
- No ordering guarantee across consumers

**Failover:**
- Primary consumer with standby consumers
- Failover on primary failure
- Use for high availability with ordering

**Key_Shared:**
- Multiple consumers with key-based ordering
- Messages with same key go to same consumer
- Best balance of parallelism and ordering

### Consumer Configuration

**Optimal Settings:**
```properties
# Receiver queue size (prefetch)
receiverQueueSize=1000

# Acknowledgment timeout
ackTimeoutMillis=30000

# Negative acknowledgment redelivery delay
negativeAckRedeliveryDelayMicros=60000000

# Max total receiver queue size across partitions
maxTotalReceiverQueueSizeAcrossPartitions=50000
```

---

## Performance Tuning

### Broker Tuning

**JVM Settings:**
```bash
# Recommended JVM options
-Xms8g -Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=10
-XX:+ParallelRefProcEnabled
-XX:+UnlockExperimentalVMOptions
-XX:+AggressiveOpts
-XX:+DoEscapeAnalysis
-XX:ParallelGCThreads=32
-XX:ConcGCThreads=32
-XX:G1NewSizePercent=50
-XX:+DisableExplicitGC
-XX:-ResizePLAB
-XX:+ExitOnOutOfMemoryError
```

**Broker Configuration:**
```properties
# Message dispatching
dispatchThrottlingRatePerTopicInMsg=10000
dispatchThrottlingRatePerTopicInByte=10485760

# Managed ledger cache
managedLedgerCacheSizeMB=1024
managedLedgerCacheEvictionWatermark=0.9

# BookKeeper client
bookkeeperClientTimeoutInSeconds=30
bookkeeperClientSpeculativeReadTimeoutMillis=1000
```

### Producer Optimization

```java
ProducerBuilder<byte[]> builder = client.newProducer()
    .topic("my-topic")
    .enableBatching(true)
    .batchingMaxMessages(1000)
    .batchingMaxPublishDelay(1, TimeUnit.MILLISECONDS)
    .maxPendingMessages(10000)
    .blockIfQueueFull(true)
    .sendTimeout(30, TimeUnit.SECONDS);
```

### Consumer Optimization

```java
ConsumerBuilder<byte[]> builder = client.newConsumer()
    .topic("my-topic")
    .subscriptionName("my-subscription")
    .subscriptionType(SubscriptionType.Shared)
    .receiverQueueSize(1000)
    .acknowledgmentGroupTime(100, TimeUnit.MILLISECONDS)
    .negativeAckRedeliveryDelay(60, TimeUnit.SECONDS);
```

---

## Monitoring and Observability

### Key Metrics to Monitor

**Broker Metrics:**
- `pulsar_broker_messages_in_rate` - Message ingestion rate
- `pulsar_broker_messages_out_rate` - Message dispatch rate
- `pulsar_broker_throughput_in` - Bytes received per second
- `pulsar_broker_throughput_out` - Bytes sent per second
- `pulsar_broker_connections` - Active connections
- `pulsar_broker_lookup_rate` - Topic lookup rate

**Topic Metrics:**
- `pulsar_topic_msg_backlog` - Message backlog
- `pulsar_topic_msg_rate_in` - Message production rate
- `pulsar_topic_msg_rate_out` - Message consumption rate
- `pulsar_topic_storage_size` - Topic storage size

**BookKeeper Metrics:**
- `bookkeeper_journal_latency` - Journal write latency
- `bookkeeper_ledger_write_latency` - Ledger write latency
- `bookkeeper_ledger_read_latency` - Ledger read latency

### Prometheus Integration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'pulsar'
    static_configs:
      - targets: ['broker:8080', 'bookie:8000']
```

### Alerting Rules

```yaml
# alerts.yml
groups:
  - name: pulsar-alerts
    rules:
      - alert: HighBacklog
        expr: pulsar_topic_msg_backlog > 100000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High message backlog detected"

      - alert: BrokerDown
        expr: up{job="pulsar"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Pulsar broker is down"
```

---

## Security Best Practices

### Authentication

1. Enable authentication on all components
2. Use TLS for all connections
3. Rotate authentication tokens regularly
4. Implement proper secret management

### Authorization

1. Define clear namespace-level policies
2. Use role-based access control
3. Implement tenant isolation
4. Regular access audits

### Encryption

1. Enable TLS for all network traffic
2. Use strong cipher suites
3. Manage certificates properly
4. Consider encryption at rest

---

## Capacity Planning

### Throughput Estimation

```
Messages/sec per broker = (CPU cores * 10000) / avg_msg_size_kb
Storage per day = msg_rate * avg_msg_size * retention_days
```

### Scaling Guidelines

**Scale Out (Add Brokers):**
- When CPU utilization exceeds 70%
- When throughput requirements exceed current capacity
- When latency increases under load

**Scale Up (Increase Resources):**
- When memory usage is high
- When disk I/O is bottleneck
- When network bandwidth is saturated

### Resource Sizing

| Cluster Size | Brokers | Bookies | Expected Throughput |
|-------------|---------|---------|---------------------|
| Small       | 3       | 3       | 100K msg/sec        |
| Medium      | 5-7     | 5-7     | 500K msg/sec        |
| Large       | 10+     | 10+     | 1M+ msg/sec         |