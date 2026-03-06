---
name: topic-consultation
description: Pulsar 主题配置和设计咨询。当用户需要咨询 Topic 配置、分区策略、保留策略时使用。
parent-skill: diagnosis
route-type: KNOWLEDGE_ONLY
allowed-tools: getTopicInfo, listTopics, getTopicConfig
---

# 主题咨询

## 概述

提供 Apache Pulsar Topic 配置和设计建议，包括分区策略、保留策略、消息策略等。

## 适用场景

在以下情况下使用此技能：
- Topic 设计咨询
- 分区策略选择
- 保留策略配置
- 消息策略设置
- Topic 配置优化

## Topic 设计要点

### 1. 分区策略

| 场景 | 推荐分区数 | 说明 |
|------|------------|------|
| 低吞吐 | 1 | 单分区足够 |
| 中等吞吐 | 3-5 | 平衡负载 |
| 高吞吐 | 10+ | 水平扩展 |
| 严格顺序 | Key_Shared | 按 Key 保序 |

### 2. 保留策略

| 策略 | 配置 | 适用场景 |
|------|------|----------|
| 时间保留 | retentionTime | 基于时间的数据生命周期 |
| 大小保留 | retentionSize | 基于存储限制 |
| 混合保留 | 两者结合 | 综合控制 |

### 3. 消息策略

| 策略 | 说明 | 默认值 |
|------|------|--------|
| TTL | 消息存活时间 | 无限 |
| 批量大小 | 批量消息数 | 1000 |
| 压缩 | 压缩算法 | 无 |
| 延迟投递 | 延迟消息 | 禁用 |

## 处理流程

### 1. 了解需求

```
吞吐量需求 → 消息大小 → 顺序要求 → 存储需求
```

### 2. 收集配置信息

```
getTopicInfo(topic) → Topic 信息
listTopics(namespace) → Topic 列表
getTopicConfig(topic) → Topic 配置
```

### 3. 提供建议

```
## Topic 配置建议

### 基本配置
- Topic 类型：[persistent/non-persistent]
- 分区数：[建议值]
- 命名规范：[建议命名]

### 存储配置
- 保留时间：[建议值]
- 保留大小：[建议值]
- TTL：[建议值]

### 性能配置
- 批量大小：[建议值]
- 压缩算法：[建议值]
- 预取值：[建议值]

### 最佳实践
1. [建议1]
2. [建议2]
3. [建议3]
```

## 常见配置示例

### 高吞吐 Topic
```bash
pulsar-admin topics create-partitioned-topic <topic> \
  --partitions 10

pulsar-admin topics set-retention <topic> \
  --time 1d --size 10G
```

### 顺序消息 Topic
```bash
# 使用 Key_Shared 订阅
# 生产者设置 partition key
```

### 延迟消息 Topic
```bash
pulsar-admin topics set-delayed-delivery <topic> \
  --enable --time 0
```

## 命名规范

```
persistent://<tenant>/<namespace>/<topic-name>

示例：
persistent://tenant1/order-service/order-created
persistent://shared/payment/payment-success
```

## 最佳实践

1. 合理规划分区数
2. 设置适当的保留策略
3. 使用压缩减少存储
4. 监控 Topic 性能指标