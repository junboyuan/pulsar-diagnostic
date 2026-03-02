---
name: topic-consultation
description: Use when providing consultation on topic design, configuration, and optimization
---

# Topic Consultation Skill

## Overview

Provide expert consultation on Apache Pulsar topic design, configuration, and optimization.

## When to Use

Use this skill when:
- Designing new topics
- Optimizing existing topics
- Choosing partition strategies
- Setting retention policies

## Process

### 1. Understand Requirements

Ask clarifying questions:
- Expected throughput?
- Number of producers/consumers?
- Message size?
- Retention requirements?
- Ordering guarantees needed?

### 2. Analyze Existing Topic (if applicable)

For existing topics:
```
getTopicInfo(topic) → getTopicStats(topic) → getTopicSubscriptions(topic)
```

### 3. Provide Recommendations

```
## Topic Consultation Report

### Current State (if existing)
- Topic: [name]
- Partitions: X
- Throughput: X msg/s
- Subscriptions: Y

### Recommendations

#### Partitioning
- Current: X partitions
- Recommended: Y partitions
- Reasoning: [explanation]

#### Retention Policy
- Recommended: [size/time based]
- Reasoning: [explanation]

#### Configuration
- maxMessageSize: X KB
- ttlDuration: Y hours
- retentionSize: Z GB

#### Best Practices
1. [Practice 1]
2. [Practice 2]

### Naming Convention
Suggested: persistent://[tenant]/[namespace]/[domain]-[entity]-[event-type]
```

## Available Tools

| Tool | Purpose |
|------|---------|
| `getTopicInfo` | Get topic configuration |
| `getTopicStats` | Get topic statistics |
| `getTopicSubscriptions` | List subscriptions |
| `listTopicsInNamespace` | List all topics |

## Decision Matrix

### Partitioning Strategy

| Scenario | Recommendation |
|----------|----------------|
| High throughput | Multiple partitions |
| Strict ordering | Single partition |
| Many consumers | Partition by key |
| Global consumers | Round-robin |

### Retention Policy

| Use Case | Recommendation |
|----------|----------------|
| Event streaming | Time-based (7-30 days) |
| Log aggregation | Size-based (100GB+) |
| Work queues | Ack-based deletion |
| Audit trail | Indefinite retention |

## Common Mistakes to Avoid

- Too few partitions (bottleneck)
- Too many partitions (overhead)
- No retention policy (disk full)
- Wrong subscription type
- Missing schema validation