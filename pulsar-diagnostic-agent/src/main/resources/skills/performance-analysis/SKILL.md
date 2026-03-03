---
name: performance-analysis
description: 用于分析集群性能并识别瓶颈。当用户报告性能缓慢、吞吐量问题、高延迟时使用。
allowed-tools: getClusterMetrics, getBrokerMetrics, getAllMetrics, queryMetric, getTopicInfo
---

# 性能分析技能

## 概述

分析Apache Pulsar集群性能，识别瓶颈并提供优化建议。

## 适用场景

在以下情况下使用此技能：
- 用户报告性能缓慢
- 吞吐量问题
- 高延迟投诉
- 资源利用率关注

## 处理流程

### 1. 收集性能指标

使用以下工具收集数据：
```
getClusterMetrics() → getBrokerMetrics() → getAllMetrics()
```

需要分析的关键指标：
- 消息吞吐量（入/出速率）
- 端到端延迟
- Broker CPU/内存使用
- Bookie磁盘I/O
- 网络带宽

### 2. 识别瓶颈

检查常见瓶颈：

| 指标 | 警告 | 严重 |
|------|------|------|
| 消息积压 | > 1万条 | > 10万条 |
| Broker CPU | > 70% | > 90% |
| Broker内存 | > 80% | > 95% |
| 磁盘I/O | > 80% | > 95% |
| 延迟P99 | > 100ms | > 500ms |

### 3. 分析模式

查找以下问题：
- **吞吐量不平衡**：消息入 ≠ 消息出
- **热点Broker**：负载分布不均
- **慢消费者**：特定订阅高积压
- **资源饱和**：任何指标达到容量

### 4. 生成报告

```
## 性能分析报告

### 摘要
- 整体吞吐量：入 X 条/秒，出 Y 条/秒
- 平均延迟：Xms
- 峰值延迟(P99)：Yms

### 发现
1. [发现1及指标]
2. [发现2及指标]

### 识别的瓶颈
- [组件]：[问题]
- [组件]：[问题]

### 建议措施
1. [优先级1建议]
2. [优先级2建议]
```

## 可用工具

| 工具 | 用途 |
|------|------|
| `getClusterMetrics` | 集群范围吞吐量和延迟 |
| `getBrokerMetrics` | 每个Broker的性能 |
| `getAllMetrics` | 所有可用的Prometheus指标 |
| `queryMetric` | 查询特定Prometheus指标 |

## 常见问题

### 生产者慢
- 检查网络延迟
- 验证生产者批处理设置
- 检查消息大小

### 消费者慢
- 检查订阅类型
- 验证消费者批处理设置
- 检查处理逻辑

### Broker过载
- 检查主题分布
- 检查Bundle分割策略
- 考虑扩容