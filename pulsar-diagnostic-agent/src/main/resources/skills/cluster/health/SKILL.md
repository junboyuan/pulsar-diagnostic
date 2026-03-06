---
name: cluster-health
description: 执行 Pulsar 集群健康检查。当用户需要检查集群状态、监控系统健康时使用。
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: getClusterInfo, performHealthCheck, getActiveBrokers, getBookies, getClusterMetrics, analyzeBrokerLogs, getBrokerMetrics
---

# 集群健康检查

## 概述

对 Apache Pulsar 集群进行全面健康检查，包括 Broker、Bookie、主题和整体系统健康状况。

## 适用场景

在以下情况下使用此技能：
- 用户询问集群健康状态
- 定期健康监控
- 维护或配置更改后
- 排查集群问题

## 检查项目

### 1. 核心组件

| 组件 | 检查内容 | 健康标准 |
|------|----------|----------|
| Broker | 数量、状态、负载 | 所有 Broker 活跃 |
| Bookie | 数量、可写状态 | 无只读 Bookie |
| ZooKeeper | 连接状态 | 连接正常 |
| Topics | 数量、积压状态 | 无异常积压 |

### 2. 性能指标

| 指标 | 健康范围 | 警告 | 严重 |
|------|----------|------|------|
| Broker CPU | < 70% | 70-90% | > 90% |
| Broker 内存 | < 80% | 80-95% | > 95% |
| 消息延迟 | < 100ms | 100-500ms | > 500ms |
| 积压量 | < 1万 | 1-10万 | > 10万 |

## 处理流程

### 1. 收集集群信息

```
getClusterInfo() → 基本集群信息
performHealthCheck() → 健康检查
getActiveBrokers() → Broker 状态
getBookies() → Bookie 状态
getClusterMetrics() → 集群指标
```

### 2. 评估健康状态

- **健康** - 所有检查通过
- **警告** - 检测到轻微问题
- **严重** - 主要问题或组件宕机

### 3. 生成健康报告

```
## 集群健康报告

### 整体状态：[健康/警告/严重]

### 组件详情

#### Brokers
- 状态：[健康/警告/严重]
- 活跃数量：X/Y
- 问题：[列出任何问题]

#### Bookies
- 状态：[健康/警告/严重]
- 总数：X，可写：Y，只读：Z
- 问题：[列出任何问题]

#### Topics
- 主题总数：X
- 有积压的主题：Y

### 建议措施
1. [优先建议]
2. [次要建议]
```

## 深度分析选项

- 调用 `analyzeBrokerLogs()` 进行日志分析
- 调用 `getBrokerMetrics()` 获取详细 Broker 指标
- 检查日志中的错误模式

## 警告信号

- 零活跃 Broker
- 所有 Bookie 只读
- 日志中错误率高
- 内存/CPU 达到严重级别