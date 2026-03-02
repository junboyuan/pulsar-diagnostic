---
name: capacity-planning
description: Use when analyzing cluster capacity and providing scaling recommendations
---

# Capacity Planning Skill

## Overview

Analyze current cluster capacity and provide recommendations for scaling and growth planning.

## When to Use

Use this skill when:
- Planning for growth
- Evaluating resource needs
- Before adding new workloads
- Regular capacity reviews

## Process

### 1. Collect Current State

Gather current capacity data:
```
getClusterInfo() → getClusterMetrics() → getBrokerMetrics() → getAllMetrics()
```

### 2. Analyze Utilization

Calculate current utilization:
- Broker count vs recommended
- Topics/partitions per broker
- Connections per broker
- Storage utilization
- Throughput per broker

### 3. Project Growth

Estimate future needs:
```
Current Growth Rate: X% per month
Projected in 6 months:
- Messages: Current * (1 + X% * 6)
- Storage: Current * (1 + X% * 6)
- Connections: Current * (1 + X% * 6)
```

### 4. Provide Recommendations

```
## Capacity Planning Report

### Current Utilization
- Brokers: X/Y (Z% utilized)
- Storage: X TB / Y TB (Z%)
- Connections: X/Y per broker
- Throughput: X msg/s capacity, Y msg/s used

### Growth Projection
- 3 months: [projected needs]
- 6 months: [projected needs]
- 12 months: [projected needs]

### Recommendations

#### Immediate
- [Action items needed now]

#### Short-term (1-3 months)
- [Actions for near term]

#### Long-term (6-12 months)
- [Strategic planning items]

### Scaling Options
1. Horizontal: Add X brokers
2. Vertical: Increase Y resources
```

## Available Tools

| Tool | Purpose |
|------|---------|
| `getClusterInfo` | Current cluster structure |
| `getClusterMetrics` | Throughput and connections |
| `getBrokerMetrics` | Per-broker utilization |
| `getAllMetrics` | All capacity metrics |

## Key Metrics

| Metric | Warning | Scale Threshold |
|--------|---------|-----------------|
| Broker CPU | > 70% | > 80% |
| Broker Memory | > 75% | > 85% |
| Connections/broker | > 3000 | > 5000 |
| Topics/broker | > 3000 | > 5000 |
| Storage used | > 70% | > 85% |

## Scaling Guidelines

- **Horizontal scaling**: Add brokers when load is evenly distributed
- **Vertical scaling**: Increase resources when few brokers are overloaded
- **Storage scaling**: Add bookies or increase disk capacity