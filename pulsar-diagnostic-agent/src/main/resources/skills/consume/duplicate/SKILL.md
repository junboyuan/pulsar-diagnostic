---
name: consume-duplicate
description: 诊断 Pulsar 消息重复消费的问题。当用户报告重复消费、消息重复处理时使用。
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: diagnoseConsumeDuplicate, getConsumerStats, getSubscriptionStats, getTopicInfo
---

# 消费重复诊断

## 概述

诊断 Apache Pulsar 消息重复消费问题，识别导致消息被多次处理的根本原因。

## 适用场景

在以下情况下使用此技能：
- 消息被重复消费
- 业务逻辑重复执行
- 同一消息多次处理
- 消费者收到重复消息

## 可能原因分析

| 原因 | 说明 | 发生场景 |
|------|------|----------|
| 消费者重连 | 重连后未确认消息重新投递 | 网络波动、消费者重启 |
| Ack 超时 | 消息处理超时导致重新投递 | 处理时间超过 ackTimeoutMillis |
| 消费者崩溃 | 未 Ack 消息重新投递 | 消费者异常退出 |
| 网络抖动 | Ack 丢失 | 网络不稳定 |
| 批量 Ack 问题 | 部分 Ack 丢失 | 批量确认失败 |
| 负向 Ack 误用 | 过度使用 nack | 代码逻辑问题 |

## 处理流程

### 1. 收集重复数据

```
diagnoseConsumeDuplicate(resource?) → 消费重复诊断
getConsumerStats(topic) → 消费者统计
getSubscriptionStats(topic) → 订阅统计
```

### 2. 分析重复原因

```
检查顺序:
1. Ack 超时配置 → 2. 消费者重连频率 → 3. 网络稳定性 → 4. 处理时间 → 5. 消费者代码
```

### 3. 关键指标

| 指标 | 说明 | 正常值 |
|------|------|--------|
| Ack 超时时间 | 消息重新投递的等待时间 | 取决于处理时间 |
| 消费者重连次数 | 消费者断开重连频率 | 应该很低 |
| 消息重投递率 | 消息重新投递比例 | < 1% |
| 处理延迟 | 消息处理时间 | < ackTimeoutMillis |

### 4. 生成诊断报告

```
## 消费重复诊断报告

### 重复情况
- 重复消息比例：X%
- 平均重复次数：N 次
- 受影响订阅：[订阅名称]

### 配置分析
- Ack 超时：X ms
- 消费者数量：N 个
- 重连频率：X 次/小时

### 根本原因
[识别的根本原因]

### 解决方案
1. [立即修复]
2. [配置优化]
3. [代码改进]
```

## 常见解决方案

### 调整 Ack 超时
```properties
# 增加 Ack 超时时间
ackTimeoutMillis=60000

# 处理时间长的场景
ackTimeoutMillis=300000
```

### 实现幂等处理
```java
// 使用消息 ID 去重
if (processedIds.contains(message.getMessageId())) {
    message.ack();
    return;
}
// 处理消息
process(message);
processedIds.add(message.getMessageId());
message.ack();
```

### 优化消费者配置
```properties
# 使用 Key_Shared 保证顺序
subscriptionType=Key_Shared

# 减少 Ack 延迟
acknowledgmentGroupTimeMicros=100000
```

### 监控重连
```bash
# 监控消费者状态
pulsar-admin topics consumers <topic>

# 检查消费者连接
pulsar-admin topics stats <topic>
```

## 预防措施

1. **实现幂等处理**：所有消费逻辑都应该支持重复处理
2. **合理设置超时**：ackTimeoutMillis 应大于最大处理时间
3. **监控消费者稳定性**：减少不必要的重连
4. **使用事务消息**：需要精确一次语义时考虑事务