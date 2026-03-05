# 故障诊断 Skill 架构重构设计

## 概述

**日期**: 2026-03-05
**任务**: 按问题现象重构故障诊断 Skill 架构
**状态**: 设计与实施

## 当前架构问题

现有意图分类是功能导向的：
- backlog-diagnosis
- cluster-health-check
- performance-analysis
- connectivity-troubleshoot
- capacity-planning
- topic-consultation
- disk-diagnosis

**问题**:
1. 用户通常从问题现象出发（如"生产慢"），而非功能分类
2. 同一问题现象可能对应多种根本原因
3. 缺乏清晰的故障定位路径

## 新架构设计

### 意图分类体系

按问题现象组织，形成两层结构：

```
问题现象 (Symptom)
├── 认证/鉴权问题 (auth-issue)
│   ├── 认证失败 (auth-failed)
│   ├── 权限不足 (permission-denied)
│   └── Token 过期 (token-expired)
│
├── 生产问题 (produce-issue)
│   ├── 生产慢 (produce-slow)
│   └── 生产失败 (produce-failed)
│
├── 消费问题 (consume-issue)
│   ├── 消费慢 (consume-slow)
│   ├── 消费失败 (consume-failed)
│   └── 消费重复 (consume-duplicate)
│
└── 其他问题 (other-issue)
    ├── 集群健康检查 (cluster-health)
    ├── 磁盘问题 (disk-issue)
    └── 容量规划 (capacity-planning)
```

### 各问题现象的故障原因映射

#### 1. 认证/鉴权问题 (auth-issue)

**症状关键词**: 认证失败、鉴权失败、权限不足、无法连接、401、403、token、 unauthorized

| 故障原因 | 症状 | 诊断方法 |
|---------|------|----------|
| 认证未启用 | 匿名连接被拒绝 | 检查 broker.conf 的 authenticationEnabled |
| Token 过期 | 之前正常，突然失败 | 检查 Token 有效期，刷新 Token |
| 权限配置错误 | 特定操作失败 | 检查命名空间权限配置 |
| TLS 配置问题 | SSL 握手失败 | 检查证书配置 |

**MCP 工具**: check_auth_config, get_permissions

#### 2. 生产慢 (produce-slow)

**症状关键词**: 生产慢、发送慢、写入慢、produce slow、延迟高、吞吐量低

| 故障原因 | 症状 | 诊断方法 |
|---------|------|----------|
| 流量突增 | 突然变慢，broker 负载高 | 检查 broker 指标、流量趋势 |
| Broker 负载高 | CPU/内存使用高 | 检查 broker_metrics |
| BookKeeper 写入慢 | 磁盘 IO 高 | 检查 bookie 指标、磁盘状态 |
| 网络延迟 | 跨机房问题 | 检查网络延迟 |
| 批量大小不合理 | 吞吐量低 | 检查生产者配置 |

**MCP 工具**: get_broker_metrics, get_topic_metrics, get_producer_stats

#### 3. 生产失败 (produce-failed)

**症状关键词**: 生产失败、发送失败、写入失败、produce failed、异常、错误

| 故障原因 | 症状 | 诊断方法 |
|---------|------|----------|
| Topic 不存在 | TopicNotFoundException | 检查 Topic 是否创建 |
| 权限不足 | 不允许生产 | 检查权限配置 |
| Broker 不可用 | 连接失败 | 检查 Broker 状态 |
| 磁盘满 | 写入被拒绝 | 检查磁盘空间 |
| 配额限制 | QuotaExceededException | 检查配额设置 |

**MCP 工具**: get_topic_info, check_permissions, inspect_cluster

#### 4. 消费慢 (consume-slow)

**症状关键词**: 消费慢、消费延迟、积压、backlog、消费处理慢、lag 高

| 故障原因 | 症状 | 诊断方法 |
|---------|------|----------|
| 消费者处理慢 | 单条消息处理时间长 | 检查消费者代码性能 |
| 消费者数量不足 | 积压持续增长 | 检查消费者数量 |
| 流量突增 | 突然积压 | 检查生产速率变化 |
| 消费者异常 | 重复重试 | 检查消费者日志 |
| 预取值不合理 | 内存压力大 | 检查消费者配置 |

**MCP 工具**: get_consumer_stats, get_topic_backlog, get_subscription_stats

#### 5. 消费失败 (consume-failed)

**症状关键词**: 消费失败、消费异常、无法消费、consume failed、消费错误

| 故障原因 | 症状 | 诊断方法 |
|---------|------|----------|
| 消息格式错误 | 反序列化失败 | 检查 Schema 配置 |
| 消费者代码异常 | 处理抛出异常 | 检查消费者日志 |
| DLQ 配置问题 | 消息无法转入 DLQ | 检查 DLQ 配置 |
| 订阅冲突 | 多消费者竞争 | 检查订阅类型和数量 |
| Broker 卸载 | 重新连接 | 检查 Broker 切换日志 |

**MCP 工具**: get_consumer_stats, get_dlq_stats, check_subscription

#### 6. 消费重复 (consume-duplicate)

**症状关键词**: 消费重复、重复消费、重复消息、duplicate、重复处理

| 故障原因 | 症状 | 诊断方法 |
|---------|------|----------|
| 消费者重连 | 重连后重新投递 | 检查 Ack 机制 |
| Ack 超时 | 消息重新投递 | 检查 ackTimeoutMillis 配置 |
| 消费者崩溃 | 未 Ack 消息重新投递 | 检查消费者稳定性 |
| 网络抖动 | Ack 丢失 | 检查网络稳定性 |
| 批量 Ack 问题 | 部分 Ack 丢失 | 检查批量 Ack 配置 |

**MCP 工具**: get_consumer_stats, get_subscription_stats

## 实现计划

### Phase 1: 意图分类重构
- [ ] 更新 IntentRecognizer 中的意图分类
- [ ] 更新关键词映射
- [ ] 更新 MCP 工具映射

### Phase 2: 诊断工具重构
- [ ] 创建问题现象导向的诊断方法
- [ ] 添加故障原因分析逻辑

### Phase 3: 知识库更新
- [ ] 按新分类重组织知识库
- [ ] 添加故障原因到解决方案映射

### Phase 4: 测试验证
- [ ] 编译测试
- [ ] 功能验证

---

## 实施记录

### Phase 1: 意图分类重构 ✅

**更新文件:**
1. `IntentRecognizer.java`
   - 重构 `SKILL_KEYWORDS` 按问题现象组织
   - 重构 `INTENT_MCP_TOOLS` 映射
   - 更新 `VALID_INTENTS` 列表
   - 更新 `detectMcpNeed` 方法
   - 更新 `determineRouteType` 方法

**新意图分类:**
| 意图 | 问题现象 | 关键词示例 |
|------|----------|-----------|
| auth-issue | 认证/鉴权问题 | 认证、权限、token、401 |
| produce-slow | 生产慢 | 生产慢、发送延迟、写入慢 |
| produce-failed | 生产失败 | 生产失败、发送异常 |
| consume-slow | 消费慢 | 消费慢、积压、backlog |
| consume-failed | 消费失败 | 消费失败、消费异常 |
| consume-duplicate | 消费重复 | 重复消费、重复消息 |

### Phase 2: 诊断工具重构 ✅

**更新文件:**
1. `DiagnosticTool.java` - 新增诊断方法:
   - `diagnoseAuthIssues()` - 认证/鉴权问题诊断
   - `diagnoseProduceSlow()` - 生产慢诊断
   - `diagnoseProduceFailed()` - 生产失败诊断
   - `diagnoseConsumeSlow()` - 消费慢诊断
   - `diagnoseConsumeFailed()` - 消费失败诊断
   - `diagnoseConsumeDuplicate()` - 消费重复诊断

2. `McpDataService.java` - 更新工具映射
3. `ResponseOrchestrator.java` - 更新意图显示名称

### Phase 3: 知识库更新 ✅

**更新文件:**
- `troubleshooting.md` - 按问题现象重新组织诊断知识

### Phase 4: 测试验证 ✅

```bash
# 编译验证
mvn clean compile -DskipTests
# 结果: BUILD SUCCESS

# 单元测试
mvn test
# 结果: BUILD SUCCESS
```

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `intent/IntentRecognizer.java` | 修改 | 按问题现象重构意图分类 |
| `service/McpDataService.java` | 修改 | 更新 MCP 工具映射 |
| `service/ResponseOrchestrator.java` | 修改 | 更新意图显示名称 |
| `tool/DiagnosticTool.java` | 修改 | 新增6个问题诊断方法 |
| `troubleshooting.md` | 重写 | 按问题现象组织知识库 |

## 架构对比

### 重构前
```
功能导向意图:
- backlog-diagnosis
- cluster-health-check
- performance-analysis
- connectivity-troubleshoot
- capacity-planning
- topic-consultation
- disk-diagnosis
```

### 重构后
```
问题现象导向意图:
├── auth-issue (认证/鉴权问题)
├── produce-slow (生产慢)
├── produce-failed (生产失败)
├── consume-slow (消费慢)
├── consume-failed (消费失败)
├── consume-duplicate (消费重复)
├── cluster-health (集群健康)
├── disk-issue (磁盘问题)
├── capacity-planning (容量规划)
└── topic-consultation (主题咨询)
```

## 优势

1. **用户友好**: 用户从问题现象出发，更符合自然思维方式
2. **诊断精准**: 每个问题现象有明确的故障原因映射
3. **易于扩展**: 可方便添加新的问题现象和故障原因
4. **知识组织**: 知识库按问题现象组织，查找更高效