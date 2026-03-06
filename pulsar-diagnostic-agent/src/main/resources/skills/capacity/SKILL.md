---
name: capacity-planning
description: Pulsar 容量规划和资源评估。当用户需要评估资源需求、规划扩容、优化配置时使用。
parent-skill: diagnosis
route-type: KNOWLEDGE_ONLY
allowed-tools: getClusterMetrics, getResourceUsage, listBrokers, getTopicMetrics
---

# 容量规划

## 概述

提供 Apache Pulsar 集群的容量规划和资源评估建议。

## 适用场景

在以下情况下使用此技能：
- 评估资源需求
- 规划集群扩容
- 优化资源配置
- 预测容量瓶颈

## 容量评估维度

### 1. 计算资源

| 资源 | 评估因素 | 建议 |
|------|----------|------|
| Broker CPU | 消息吞吐量 | 1000 msg/s ≈ 1 core |
| Broker 内存 | 连接数、Topic 数 | 最少 4GB |
| Bookie 磁盘 | 消息存储量 | 根据保留策略 |

### 2. 存储资源

| 因素 | 计算方式 |
|------|----------|
| 消息存储 | 消息大小 × 吞吐量 × 保留时间 |
| Ledger 索引 | Ledger 数量 × 索引大小 |
| 日志空间 | 日志速率 × 轮转周期 |

### 3. 网络带宽

| 场景 | 带宽需求 |
|------|----------|
| 生产流量 | 消息大小 × 生产速率 |
| 消费流量 | 消息大小 × 消费速率 |
| 复制流量 | 跨机房复制带宽 |

## 处理流程

### 1. 收集容量数据

```
getClusterMetrics() → 集群指标
getResourceUsage() → 资源使用
listBrokers() → Broker 列表
getTopicMetrics() → Topic 指标
```

### 2. 容量分析

```
当前使用 → 增长趋势 → 预测需求 → 扩容建议
```

### 3. 生成规划报告

```
## 容量规划报告

### 当前资源使用
- Broker 数量：X
- CPU 使用率：Y%
- 内存使用率：Z%
- 磁盘使用率：W%

### 吞吐量分析
- 消息入速率：X 条/秒
- 消息出速率：Y 条/秒
- 数据存储：Z GB/天

### 增长预测
- 3 个月需求：[预测]
- 6 个月需求：[预测]
- 1 年需求：[预测]

### 扩容建议
1. [短期建议]
2. [中期建议]
3. [长期建议]
```

## 扩容建议参考

### Broker 扩容
- CPU > 80%：增加 Broker 或优化配置
- 内存 > 85%：增加内存或优化堆外内存

### Bookie 扩容
- 磁盘 > 80%：增加 Bookie 或清理数据
- I/O 瓶颈：增加 Bookie 分散 I/O

### Topic 分区
- 单分区吞吐 > 5万 msg/s：增加分区数
- 消费者积压：增加分区和消费者

## 最佳实践

1. 预留 30% 资源余量
2. 监控资源使用趋势
3. 定期评估容量需求
4. 制定扩容预案