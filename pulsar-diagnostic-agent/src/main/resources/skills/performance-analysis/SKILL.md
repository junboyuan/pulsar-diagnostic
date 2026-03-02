---
name: performance-analysis
description: Use when analyzing cluster performance and identifying bottlenecks
---

# Performance Analysis Skill

## Overview

Analyze Apache Pulsar cluster performance, identify bottlenecks, and provide optimization recommendations.

## When to Use

Use this skill when:
- User reports slow performance
- Throughput issues
- High latency complaints
- Resource utilization concerns

## Process

### 1. Collect Performance Metrics

Gather data using:
```
getClusterMetrics() → getBrokerMetrics() → getAllMetrics()
```

Key metrics to analyze:
- Message throughput (in/out rates)
- End-to-end latency
- Broker CPU/memory usage
- Bookie disk I/O
- Network bandwidth

### 2. Identify Bottlenecks

Check for common bottlenecks:

| Metric | Warning | Critical |
|--------|---------|----------|
| Message backlog | > 10K messages | > 100K messages |
| Broker CPU | > 70% | > 90% |
| Broker memory | > 80% | > 95% |
| Disk I/O | > 80% | > 95% |
| Latency P99 | > 100ms | > 500ms |

### 3. Analyze Patterns

Look for:
- **Throughput imbalance**: Messages in ≠ messages out
- **Hot brokers**: Uneven load distribution
- **Slow consumers**: High backlog on specific subscriptions
- **Resource saturation**: Any metric at capacity

### 4. Generate Report

```
## Performance Analysis Report

### Summary
- Overall throughput: X msg/s in, Y msg/s out
- Average latency: Xms
- Peak latency (P99): Yms

### Findings
1. [Finding 1 with metrics]
2. [Finding 2 with metrics]

### Bottlenecks Identified
- [Component]: [Issue]
- [Component]: [Issue]

### Recommendations
1. [Priority 1 recommendation]
2. [Priority 2 recommendation]
```

## Available Tools

| Tool | Purpose |
|------|---------|
| `getClusterMetrics` | Cluster-wide throughput and latency |
| `getBrokerMetrics` | Per-broker performance |
| `getAllMetrics` | All available Prometheus metrics |
| `queryMetric` | Query specific Prometheus metric |

## Common Issues

### Slow Producers
- Check network latency
- Verify producer batch settings
- Review message size

### Slow Consumers
- Check subscription type
- Verify consumer batch settings
- Review processing logic

### Broker Overload
- Check topic distribution
- Review bundle split policies
- Consider scaling