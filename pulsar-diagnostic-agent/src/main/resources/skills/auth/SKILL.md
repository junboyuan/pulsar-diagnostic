---
name: auth-issue
description: 诊断 Pulsar 认证和鉴权问题。当用户报告认证失败、权限不足、Token 问题、401/403 错误时使用。
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: diagnoseAuthIssues, checkAuthConfig, getPermissions, inspectCluster
---

# 认证/鉴权问题诊断

## 概述

诊断 Apache Pulsar 的认证和授权问题，包括 Token 验证失败、权限不足、认证配置错误等。

## 适用场景

在以下情况下使用此技能：
- 用户报告认证失败
- 权限不足错误
- Token 过期或无效
- 401/403 错误码
- 客户端无法连接（认证相关）

## 问题分类

### 1. 认证问题

| 问题 | 症状 | 诊断方法 |
|------|------|----------|
| 认证未启用 | 匿名连接被拒绝 | 检查 `authenticationEnabled` |
| Token 无效 | 认证失败 | 验证 Token 格式和签名 |
| Token 过期 | 之前正常，突然失败 | 检查 Token 有效期 |
| 认证插件错误 | 认证失败 | 检查插件配置 |

### 2. 鉴权问题

| 问题 | 症状 | 诊断方法 |
|------|------|----------|
| 权限不足 | 操作被拒绝 | 检查 namespace 权限 |
| 角色错误 | 特定操作失败 | 验证用户角色 |
| 超级用户配置 | 管理操作失败 | 检查超级用户配置 |

## 处理流程

### 1. 收集信息

```
diagnoseAuthIssues(resource?) → 认证状态检查
checkAuthConfig() → 认证配置检查
getPermissions(resource) → 权限检查
```

### 2. 分析问题

检查以下方面：
1. **Broker 认证配置**
   - `authenticationEnabled` 是否为 true
   - 认证插件是否正确配置
   - Token 密钥是否正确

2. **Token 状态**
   - Token 是否有效
   - Token 是否过期
   - Token 签名是否正确

3. **权限配置**
   - Namespace 权限设置
   - Topic 权限设置
   - 用户角色映射

### 3. 生成诊断报告

```
## 认证/鉴权诊断报告

### 问题类型
[认证失败 / 权限不足 / Token 问题]

### 详细分析
- 配置状态：[正确/错误]
- Token 状态：[有效/无效/过期]
- 权限状态：[充足/不足]

### 根本原因
[识别的根本原因]

### 解决方案
1. [立即修复步骤]
2. [配置调整建议]
3. [预防措施]
```

## 常见问题解决

### Token 过期
```bash
# 验证 Token
pulsar-admin tokens validate <token>

# 生成新 Token
pulsar-admin tokens create --secret-key <key> --subject <subject>
```

### 权限不足
```bash
# 检查权限
pulsar-admin namespaces permissions <namespace>

# 授予权限
pulsar-admin namespaces grant-permission <namespace> \
  --role <role> --actions <actions>
```

### TLS 问题
```bash
# 检查证书
openssl x509 -in <cert> -text -noout

# 验证连接
openssl s_client -connect <broker>:6651
```

## 警告信号

- 多个客户端同时报告认证失败
- 特定用户/角色的操作全部失败
- 日志中大量认证错误