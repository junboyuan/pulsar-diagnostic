# Pulsar Troubleshooting Guide

## Common Issues and Solutions

### Connection Issues

#### Problem: Client Cannot Connect to Broker
**Symptoms:**
- Connection refused errors
- Timeout exceptions
- Client unable to establish connection

**Possible Causes:**
1. Broker is not running
2. Firewall blocking ports (6650 for broker, 8080 for admin)
3. Incorrect broker URL configuration
4. DNS resolution issues

**Solutions:**
1. Check broker status: `bin/pulsar-admin brokers healthcheck`
2. Verify broker is listening: `netstat -tlnp | grep 6650`
3. Check firewall rules: `iptables -L -n`
4. Verify broker URL in client configuration matches advertised address

#### Problem: Authentication Failed
**Symptoms:**
- Authentication errors in logs
- Client connection rejected

**Solutions:**
1. Verify authentication is enabled in broker.conf
2. Check client has correct authentication token
3. Verify token hasn't expired
4. Check authorization policies

---

### Performance Issues

#### Problem: High Message Latency
**Symptoms:**
- End-to-end latency increasing
- Messages taking longer to process
- Consumer lag growing

**Possible Causes:**
1. Consumer processing too slowly
2. Network latency issues
3. Broker overloaded
4. BookKeeper write latency

**Solutions:**
1. Check consumer processing time and optimize code
2. Monitor network latency between components
3. Scale brokers horizontally
4. Check BookKeeper disk I/O performance
5. Consider increasing consumer parallelism

#### Problem: Low Throughput
**Symptoms:**
- Message rate below expected
- Backlog growing despite consumers

**Solutions:**
1. Increase partition count for topics
2. Add more consumers to the subscription
3. Check for consumer processing bottlenecks
4. Verify network bandwidth
5. Review batch size configuration

---

### Backlog Issues

#### Problem: Growing Message Backlog
**Symptoms:**
- Backlog size increasing
- Consumer lag alerts
- Storage growing

**Possible Causes:**
1. Consumer not running or stuck
2. Consumer processing slower than production rate
3. Consumer errors causing redelivery loops
4. Too few consumers for the load

**Solutions:**
1. Check consumer application status and logs
2. Scale consumer instances
3. Review consumer error handling
4. Consider dead letter queue for poison pills
5. Optimize consumer processing logic

#### Problem: Backlog Not Decreasing
**Symptoms:**
- Messages accumulating
- Consumers seem to process but backlog remains

**Solutions:**
1. Verify subscription type is appropriate (Failover, Shared, Exclusive)
2. Check for stuck consumers
3. Review redelivery policies
4. Check for negative acknowledgments

---

### Broker Issues

#### Problem: Broker Crashes or Restarts Frequently
**Symptoms:**
- Broker process terminates unexpectedly
- Frequent leader elections
- Topic unloading events

**Possible Causes:**
1. Out of memory errors
2. GC overhead
3. File descriptor limits
4. Disk space exhaustion

**Solutions:**
1. Increase JVM heap size
2. Tune GC settings (recommend G1GC)
3. Increase file descriptor limits: `ulimit -n 65536`
4. Free up disk space or add storage
5. Review broker logs for root cause

#### Problem: Broker Not Loading Topics
**Symptoms:**
- Topics show as unloaded
- Ownership not acquired

**Solutions:**
1. Check broker bundles configuration
2. Review load manager settings
3. Check ZooKeeper connectivity
4. Verify namespace policies

---

### BookKeeper Issues

#### Problem: Bookie Not Available
**Symptoms:**
- Write errors
- Ledger ensemble issues
- Replication problems

**Solutions:**
1. Check bookie process status
2. Verify disk space availability
3. Check network connectivity to bookie
4. Review bookie logs for errors

#### Problem: Bookie in Read-Only Mode
**Symptoms:**
- Writes failing
- Bookie shows read-only status

**Causes:**
- Disk space threshold reached
- Disk I/O errors

**Solutions:**
1. Free up disk space
2. Check disk health
3. Review bookie configuration for disk thresholds

---

### Topic Issues

#### Problem: Topic Not Found
**Symptoms:**
- Topic lookup fails
- Producer/Consumer cannot connect

**Solutions:**
1. Verify topic name format: `persistent://tenant/namespace/topic`
2. Check namespace exists
3. Create topic if using non-partitioned topics
4. Verify tenant and namespace policies

#### Problem: Topic Partitions Unbalanced
**Symptoms:**
- Uneven message distribution
- Some partitions with higher load

**Solutions:**
1. Review key-based routing if using keys
2. Consider increasing partition count
3. Check for hot keys in message distribution
4. Review partition assignment strategy

---

### Memory and Resource Issues

#### Problem: OutOfMemoryError
**Symptoms:**
- Broker crashes
- GC overhead limit exceeded errors

**Solutions:**
1. Increase -Xmx setting
2. Tune GC algorithm (recommend G1GC)
3. Review direct memory limits (-XX:MaxDirectMemorySize)
4. Check for memory leaks
5. Monitor heap usage patterns

#### Problem: Too Many Open Files
**Symptoms:**
- "Too many open files" errors
- Connection failures

**Solutions:**
1. Increase system limits: `ulimit -n 65536`
2. Add to /etc/security/limits.conf
3. Review file handle usage pattern
4. Check for connection leaks