---
name: produce-slow
description: 诊断 Pulsar 消息生产慢的问题。当用户报告发送延迟高、写入慢、生产吞吐量低时使用。
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: diagnoseProduceSlow, getBrokerMetrics, getTopicMetrics, getProducerStats, getClusterMetrics
---

# 生产慢诊断

## 概述

诊断 Apache Pulsar 消息生产性能问题，识别导致发送延迟高、吞吐量低的根本原因。

## 适用场景

在以下情况下使用此技能：
- 消息发送延迟高
- 生产吞吐量低于预期
- 写入速度慢
- 生产者响应慢

## 可能原因分析

| 原因 | 症状 | 检查方法 |
|------|------|----------|
| 流量突增 | 突然变慢，Broker 负载高 | 检查消息入速率趋势 |
| Broker 负载高 | CPU/内存使用率高 | `getBrokerMetrics` |
| BookKeeper 写入慢 | 磁盘 IO 高 | 检查 Bookie 指标 |
| 网络延迟 | 跨机房问题 | 网络诊断 |
| 批量大小不合理 | 吞吐量低 | 检查生产者配置 |
| 压缩开销大 | CPU 高 | 检查压缩配置 |

## 处理流程

### 1. 收集性能数据

```
diagnoseProduceSlow(topic?) → 生产性能诊断
getBrokerMetrics() → Broker 状态
getTopicMetrics(topic) → Topic 性能
getProducerStats(topic) → 生产者统计
```

### 2. 性能基准

| 指标 | 正常 | 警告 | 严重 |
|------|------|------|------|
| 发送延迟 P99 | < 10ms | 10-100ms | > 100ms |
| Broker CPU | < 60% | 60-80% | > 80% |
| Broker 内存 | < 70% | 70-90% | > 90% |
| Bookie 写延迟 | < 5ms | 5-20ms | > 20ms |

### 3. 分析瓶颈

```
检查顺序:
1. Broker 负载 → 2. BookKeeper 性能 → 3. 网络状态 → 4. 生产者配置
```

### 4. 生成诊断报告

```
## 生产慢诊断报告

### 性能概览
- 消息入速率：X 条/秒
- 发送延迟 P99：X ms
- 吞吐量：X MB/秒

### 识别的瓶颈
- [瓶颈1]：[详细说明]
- [瓶颈2]：[详细说明]

### 根本原因
[分析得出的根本原因]

### 优化建议
1. [立即优化措施]
2. [配置调整]
3. [扩容建议]
```

## 常见优化措施

### 流量突增
- 增加 Broker 数量
- 调整 Topic 分区数
- 启用批量发送

### Broker 负载高
- 水平扩展 Broker
- 优化 JVM 配置
- 调整 Bundle 分配

### BookKeeper 写入慢
- 检查磁盘性能
- 增加 Bookie 数量
- 调整 Journal 配置

### 生产者配置优化
```properties
# 增加批量大小
batchingMaxMessages=1000
batchingMaxPublishDelayMs=10

# 启用压缩
compressionType=LZ4

# 增加并发
maxPendingMessages=1000
```