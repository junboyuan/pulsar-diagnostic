---
name: connectivity-troubleshoot
description: 用于排查集群中的连接和网络问题。当客户端无法连接、连接超时、认证失败时使用。
allowed-tools: getClusterInfo, performHealthCheck, getActiveBrokers, diagnoseConnectionIssues
---

# 连接故障排查技能

## 概述

诊断和解决Apache Pulsar集群中的连接问题。

## 适用场景

在以下情况下使用此技能：
- 客户端无法连接到Broker
- 连接超时错误
- 认证/授权失败
- 网络分区问题

## 处理流程

### 1. 验证集群状态

首先检查基本集群健康状况：
```
getClusterInfo() → performHealthCheck() → getActiveBrokers()
```

### 2. 诊断连接问题

使用 `diagnoseConnectionIssues()` 获取具体诊断。

需要调查的常见问题：
- **Broker不可达**：检查Broker是否运行
- **DNS解析**：验证Broker URL是否正确
- **端口可访问性**：检查防火墙规则
- **TLS/SSL**：验证证书

### 3. 检查认证

如果出现认证错误：
- 验证令牌有效性
- 检查角色权限
- 检查认证插件配置

### 4. 提供故障排查步骤

```
## 连接故障排查报告

### 问题类型：[连接/认证/网络]

### 诊断结果
[识别的根本原因]

### 解决步骤
1. [步骤1]
2. [步骤2]
3. [步骤3]

### 验证方法
[如何验证修复成功]
```

## 可用工具

| 工具 | 用途 |
|------|------|
| `getClusterInfo` | 验证集群可访问性 |
| `performHealthCheck` | 检查组件健康 |
| `getActiveBrokers` | 列出可达的Broker |
| `diagnoseConnectionIssues` | 专门的连接诊断 |

## 常见错误码

| 错误 | 可能原因 | 解决方案 |
|-------|--------------|----------|
| 连接被拒绝 | Broker宕机 | 重启Broker |
| 超时 | 网络问题 | 检查防火墙/网络 |
| 认证失败 | 凭据无效 | 验证令牌/权限 |
| SSL错误 | 证书问题 | 更新/修复证书 |

## 警告信号

- 零个活跃Broker可达
- 所有连接超时
- 配置更改后认证失败