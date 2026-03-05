# 知识问答与 MCP 服务集成任务

## 任务概述
在知识问答和问题诊断中集成 MCP 服务，实现智能路由和综合回答

## 进度追踪

### Phase 1: 基础组件 ✅
- [x] 增强 IntentResult 添加路由信息
- [x] 创建 RouteType 枚举
- [x] 更新 IntentRecognizer 提示词
- [x] 创建 McpDataResult 数据模型

### Phase 2: MCP 数据服务 ✅
- [x] 实现 McpDataService
- [x] 添加智能工具选择逻辑
- [x] 实现并行数据获取

### Phase 3: 响应编排 ✅
- [x] 实现 ResponseOrchestrator
- [x] 添加路由策略
- [x] 实现上下文合并

### Phase 4: 集成重构 ✅
- [x] 重构 ChatService 使用编排器
- [x] 重构 DiagnosticService 集成 MCP
- [x] 更新 KnowledgeQAService 支持混合模式

### Phase 5: 测试验证 ✅
- [x] 编译验证
- [x] 单元测试
- [x] 记录历史

## 审查部分

### 实现总结

成功实现了知识问答与 MCP 服务集成：

1. **新增 3 个核心组件**:
   - `RouteType` - 路由类型枚举
   - `McpDataService` - MCP 数据服务
   - `ResponseOrchestrator` - 响应编排服务

2. **增强 2 个核心服务**:
   - `IntentRecognizer` - 添加路由判断和 MCP 需求识别
   - `ChatService` - 使用编排器统一处理

3. **新增 2 个数据模型**:
   - `McpDataResult` - MCP 数据结果
   - `OrchestratorResponse` - 编排器响应

### 测试结果

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 架构优化

```
用户问题
    │
    ▼
IntentRecognizer (意图 + 路由判断)
    │
    ▼
ResponseOrchestrator (统一编排)
    │
    ├─── [knowledge] ──▶ RAGService
    │
    ├─── [mcp] ───────▶ McpDataService
    │
    └─── [hybrid] ────▶ 并行获取
                              │
                              ▼
                        LLM 综合回答
```

### 配置项

```yaml
pulsar-diagnostic:
  orchestration:
    parallel-timeout-ms: 10000
    mcp-timeout-ms: 5000
    knowledge-timeout-ms: 5000
  mcp:
    timeout-ms: 5000
```