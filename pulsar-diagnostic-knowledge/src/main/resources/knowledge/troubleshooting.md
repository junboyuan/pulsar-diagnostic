# Pulsar 故障诊断指南

## 问题现象分类

---

## 1. 认证/鉴权问题 (auth-issue)

### 问题: 认证失败
**症状:**
- 连接被拒绝，提示认证错误
- 401/403 错误码
- "Authentication failed" 日志

**可能原因:**
1. 认证未启用但客户端尝试认证连接
2. Token 无效或过期
3. 认证插件配置错误
4. TLS 证书问题

**解决方案:**
1. 检查 broker.conf 中的 `authenticationEnabled`
2. 验证 Token: `pulsar-admin tokens validate <token>`
3. 检查认证插件配置
4. 查看 Broker 日志中的认证错误详情

### 问题: 权限不足
**症状:**
- 操作被拒绝
- "Permission denied" 错误
- 特定 Topic 无法访问

**解决方案:**
1. 检查命名空间权限: `pulsar-admin namespaces grant-permission`
2. 验证角色配置
3. 检查 Topic 级别权限

---

## 2. 生产问题

### 问题: 生产慢 (produce-slow)
**症状:**
- 消息发送延迟高
- 吞吐量低于预期
- 生产者响应慢

**可能原因:**

| 原因 | 诊断方法 |
|------|----------|
| 流量突增 | 检查 Broker 指标，查看消息入速率趋势 |
| Broker 负载高 | 检查 CPU/内存使用率 |
| BookKeeper 写入慢 | 检查 Bookie 磁盘 I/O |
| 网络延迟 | ping/telnet 测试网络 |
| 批量大小不合理 | 检查生产者 batchSizeMode 配置 |

**解决方案:**
1. 增加 Broker 数量
2. 优化 BookKeeper 配置
3. 调整生产者批量大小
4. 检查网络带宽

### 问题: 生产失败 (produce-failed)
**症状:**
- 发送异常
- TopicNotFoundException
- 权限错误

**可能原因:**

| 原因 | 症状 | 解决方案 |
|------|------|----------|
| Topic 不存在 | TopicNotFoundException | 创建 Topic 或开启自动创建 |
| 权限不足 | PermissionDeniedException | 授予生产权限 |
| Broker 不可用 | 连接失败 | 检查 Broker 状态 |
| 磁盘满 | 写入被拒绝 | 清理磁盘空间 |
| 配额限制 | QuotaExceededException | 调整配额设置 |

---

## 3. 消费问题

### 问题: 消费慢 (consume-slow)
**症状:**
- 消息积压增长
- Consumer lag 增加
- 处理延迟高

**可能原因:**

| 原因 | 诊断方法 |
|------|----------|
| 消费者处理慢 | 检查消费者代码性能 |
| 消费者数量不足 | 检查订阅消费者数量 |
| 流量突增 | 检查生产速率变化 |
| 消费者异常重试 | 检查消费者日志 |
| 预取值不合理 | 检查 receiverQueueSize |

**解决方案:**
1. 增加消费者数量
2. 优化消费者处理逻辑
3. 调整预取值 (receiverQueueSize)
4. 检查消费者异常处理

### 问题: 消费失败 (consume-failed)
**症状:**
- 消费异常
- 消息无法处理
- 订阅状态异常

**可能原因:**

| 原因 | 症状 | 解决方案 |
|------|------|----------|
| Schema 不匹配 | 反序列化失败 | 检查 Schema 配置 |
| 消息格式错误 | 解析异常 | 检查消息格式 |
| 消费者代码异常 | 处理抛出异常 | 修复消费者代码 |
| DLQ 配置问题 | 死信无法转发 | 检查 DLQ 配置 |
| 订阅冲突 | 多消费者竞争 | 调整订阅类型 |

### 问题: 消费重复 (consume-duplicate)
**症状:**
- 同一消息多次处理
- 业务逻辑重复执行

**可能原因:**

| 原因 | 说明 |
|------|------|
| 消费者重连 | 重连后未确认消息重新投递 |
| Ack 超时 | 消息处理超时导致重新投递 |
| 消费者崩溃 | 未 Ack 消息重新投递 |
| 网络抖动 | Ack 丢失 |
| 批量 Ack 问题 | 部分 Ack 丢失 |

**解决方案:**
1. 实现幂等处理
2. 调整 ackTimeoutMillis 配置
3. 使用 Key_Shared 订阅保证顺序
4. 监控消费者重连频率

---

## 4. 其他问题

### 集群健康检查 (cluster-health)
- Broker 状态检查
- Bookie 可用性检查
- ZooKeeper 连接检查

### 磁盘问题 (disk-issue)
- 磁盘空间不足
- Bookie 只读模式
- 磁盘 I/O 性能

### 容量规划 (capacity-planning)
- 资源使用评估
- 扩容建议
- 性能基准测试

---

## 诊断命令速查

```bash
# 集群状态
pulsar-admin brokers list
pulsar-admin brokers healthcheck

# Topic 诊断
pulsar-admin topics list <namespace>
pulsar-admin topics stats <topic>
pulsar-admin topics peek-messages <topic> -s <subscription>

# 消费者诊断
pulsar-admin topics subscriptions <topic>
pulsar-admin topics consumers <topic>

# 权限检查
pulsar-admin namespaces permissions <namespace>
pulsar-admin topics permissions <topic>

# 指标查看
curl http://<broker>:8080/metrics
```