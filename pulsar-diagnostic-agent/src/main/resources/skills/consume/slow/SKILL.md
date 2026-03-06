---
name: consume-slow
description: 诊断 Pulsar 消息消费慢的问题。当用户报告消费延迟、消息积压、消费处理慢时使用。
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: diagnoseConsumeSlow, getConsumerStats, getTopicBacklog, getSubscriptionStats, getClusterMetrics
---

# 消费慢诊断

## 概述

诊断 Apache Pulsar 消息消费性能问题，识别导致消费延迟、消息积压的根本原因。

## 适用场景

在以下情况下使用此技能：
- 消息消费慢
- 消费延迟高
- 消息积压增长
- Consumer lag 增加
- 处理速度跟不上

## 可能原因分析

| 原因 | 症状 | 诊断方法 |
|------|------|----------|
| 消费者处理慢 | 单条消息处理时间长 | 检查消费者代码性能 |
| 消费者数量不足 | 积压持续增长 | 检查消费者数量 |
| 流量突增 | 突然积压 | 检查生产速率变化 |
| 消费者异常重试 | 日志有错误 | 检查消费者日志 |
| 预取值不合理 | 内存压力大 | 检查 receiverQueueSize |
| 订阅类型不当 | 消费不均衡 | 检查订阅类型 |

## 处理流程

### 1. 收集消费数据

```
diagnoseConsumeSlow(resource?) → 消费慢诊断
getConsumerStats(topic) → 消费者统计
getTopicBacklog(topic) → 积压详情
getSubscriptionStats(topic) → 订阅状态
getClusterMetrics() → 集群指标
```

### 2. 性能基准

| 指标 | 正常 | 警告 | 严重 |
|------|------|------|------|
| 积压大小 | < 1万条 | 1-10万条 | > 10万条 |
| 消费延迟 | < 100ms | 100ms-1s | > 1s |
| 消费者利用率 | 60-80% | 80-95% | > 95% 或 < 50% |
| 消息出速率 | ≈ 入速率 | < 入速率 | << 入速率 |

### 3. 分析模式

```
检查顺序:
1. 积压趋势 → 2. 消费者数量 → 3. 消费速率 → 4. 消费者健康 → 5. 配置优化
```

### 4. 生成诊断报告

```
## 消费慢诊断报告

### 消费概览
- 消息入速率：X 条/秒
- 消息出速率：Y 条/秒
- 当前积压：Z 条
- 消费者数量：N 个

### 识别的问题
- [问题1]：[详细说明]
- [问题2]：[详细说明]

### 根本原因
[分析得出的根本原因]

### 优化建议
1. [立即优化措施]
2. [配置调整]
3. [扩容建议]
```

## 常见优化措施

### 消费者处理慢
- 优化消费者代码性能
- 减少消息处理中的 I/O 操作
- 使用异步处理

### 消费者数量不足
```bash
# 增加消费者实例
# 对于 Shared 订阅，可以增加消费者数量
# 对于 Key_Shared 订阅，确保 key 分布合理
```

### 流量突增
- 快速扩容消费者
- 临时跳过积压（谨慎使用）
- 使用批量消费

### 配置优化
```properties
# 增加预取值
receiverQueueSize=1000

# 启用批量接收
batchReceivePolicy.maxNumMessages=100
batchReceivePolicy.timeoutMs=100

# 调整 Ack 超时
ackTimeoutMillis=60000
```

## 关键指标监控

- **积压趋势**：积压是否持续增长
- **消费速率比**：出速率/入速率
- **消费者活跃数**：实际消费的消费者数量
- **重试率**：消息重试比例