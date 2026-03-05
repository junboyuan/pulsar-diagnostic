# 知识问答与 MCP 服务集成设计

## 概述

**日期**: 2026-03-05
**任务**: 在知识问答和问题诊断中集成 MCP 服务，实现智能路由和综合回答
**状态**: 设计阶段

## 背景分析

### 当前架构问题

1. **知识问答（KnowledgeQAService）**
   - 仅使用 RAG 检索知识库
   - 无法获取实时集群数据
   - 回答可能过时或不完整

2. **诊断服务（DiagnosticService）**
   - 有工具支持但未与 MCP 集成
   - 知识上下文与实时数据分离

3. **意图识别（IntentRecognizer）**
   - 仅分类意图类型
   - 不判断是否需要 MCP 数据
   - 不提供路由信息

4. **MCP 集成（McpClient/McpToolRegistry）**
   - 已实现 HTTP 客户端
   - 未与核心问答/诊断流程集成

### 业务需求

用户问题可能需要：
- **纯知识回答**: "Pulsar 的消息保留策略是什么？"
- **实时数据查询**: "当前集群有多少个 broker？"
- **混合回答**: "为什么我的 topic 消息积压了？"（需要知识 + 实时数据）

## 设计方案

### 架构概览

```
用户问题
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│                    IntentRecognizer                      │
│  - 识别意图类型                                          │
│  - 判断是否需要 MCP 数据                                 │
│  - 提供路由建议                                          │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│                    ResponseOrchestrator                  │
│  - 根据路由信息协调各服务                                 │
│  - 并行获取知识和 MCP 数据                               │
│  - 构建统一上下文                                        │
└─────────────────────────────────────────────────────────┘
    │
    ├─── [knowledge] ──▶ RAGService ──▶ 知识上下文
    │
    ├─── [mcp] ───────▶ McpDataService ──▶ 实时数据
    │
    └─── [both] ──────▶ 并行获取 ──▶ 合并上下文
                              │
                              ▼
                    ┌─────────────────┐
                    │   LLM Synthesis │
                    │   综合回答生成   │
                    └─────────────────┘
```

### 核心组件设计

#### 1. IntentResult 增强

```java
public record IntentResult(
    String intent,
    double confidence,
    String reasoning,
    String suggestedAction,
    String resolvedQuestion,
    String translation,

    // 新增字段
    boolean needsMcpData,           // 是否需要 MCP 数据
    List<String> suggestedMcpTools, // 建议调用的 MCP 工具
    RouteType routeType             // 路由类型
) {
    public enum RouteType {
        KNOWLEDGE_ONLY,   // 仅知识库
        MCP_ONLY,         // 仅 MCP 实时数据
        HYBRID,           // 知识 + MCP
        GENERAL_CHAT      // 通用对话
    }
}
```

#### 2. McpDataService（新建）

专门处理 MCP 数据获取的服务：

```java
@Service
public class McpDataService {

    private final McpToolRegistry mcpToolRegistry;

    /**
     * 根据意图获取相关 MCP 数据
     */
    public McpDataResult fetchMcpData(IntentResult intent, String query);

    /**
     * 批量获取多个工具的数据
     */
    public Map<String, String> fetchMultipleTools(List<String> toolNames);

    /**
     * 智能选择工具并获取数据
     */
    public McpDataResult smartFetch(String query, IntentResult intent);
}

public record McpDataResult(
    boolean success,
    Map<String, String> toolResults,
    String aggregatedContext,
    long fetchTimeMs
) {}
```

#### 3. ResponseOrchestrator（新建）

统一响应编排服务：

```java
@Service
public class ResponseOrchestrator {

    private final IntentRecognizer intentRecognizer;
    private final RAGService ragService;
    private final McpDataService mcpDataService;
    private final ChatClient chatClient;

    /**
     * 处理用户问题并生成综合响应
     */
    public OrchestratorResponse process(String query, List<String> history);

    /**
     * 根据路由类型执行相应策略
     */
    private String executeRoute(IntentResult intent, String query);

    /**
     * 构建最终上下文
     */
    private String buildContext(String knowledgeContext, String mcpContext);

    /**
     * 使用 LLM 生成综合回答
     */
    private String synthesizeResponse(String query, String context);
}

public record OrchestratorResponse(
    String content,
    IntentResult intent,
    RouteType routeType,
    String knowledgeContext,
    String mcpContext,
    long totalTimeMs
) {}
```

#### 4. ChatService 重构

```java
@Service
public class ChatService {

    private final ResponseOrchestrator responseOrchestrator;

    public String chat(String userMessage, List<String> history) {
        // 使用编排器处理
        OrchestratorResponse response = responseOrchestrator.process(userMessage, history);
        return formatResponse(response);
    }
}
```

#### 5. IntentRecognizer 增强

更新提示词模板，让 LLM 判断是否需要 MCP：

```markdown
## 意图识别与路由判断

分析用户问题，识别意图并判断是否需要实时数据：

### 判断规则
1. **需要 MCP 数据**（needs_mcp_data: true）：
   - 询问当前状态、数量、列表（"有多少"、"当前"、"显示所有"）
   - 需要实时诊断（"为什么积压"、"性能问题"）
   - 需要检查配置或状态

2. **仅知识库**（needs_mcp_data: false）：
   - 概念性问题（"什么是"、"如何配置"）
   - 最佳实践问题
   - 历史或原理问题

### MCP 工具映射
- backlog-diagnosis → get_topic_backlog, get_consumer_stats
- cluster-health-check → inspect_cluster, get_broker_metrics
- performance-analysis → get_broker_metrics, get_topic_metrics
- connectivity-troubleshoot → check_connectivity, get_connection_stats
```

### 处理流程

#### 场景 1: 纯知识问题

```
用户: "Pulsar 的消息保留策略是什么？"
    │
    ▼
IntentRecognizer:
  - intent: "topic-consultation"
  - needsMcpData: false
  - routeType: KNOWLEDGE_ONLY
    │
    ▼
ResponseOrchestrator:
  - 调用 RAGService 获取知识
  - 不调用 MCP
  - 生成回答
```

#### 场景 2: 纯实时数据问题

```
用户: "当前集群有多少个 broker？"
    │
    ▼
IntentRecognizer:
  - intent: "cluster-health-check"
  - needsMcpData: true
  - suggestedMcpTools: ["inspect_cluster"]
  - routeType: MCP_ONLY
    │
    ▼
ResponseOrchestrator:
  - 不调用 RAG
  - 调用 MCP inspect_cluster
  - 生成回答
```

#### 场景 3: 混合问题

```
用户: "为什么我的 topic 消息积压了？"
    │
    ▼
IntentRecognizer:
  - intent: "backlog-diagnosis"
  - needsMcpData: true
  - suggestedMcpTools: ["get_topic_backlog", "get_consumer_stats"]
  - routeType: HYBRID
    │
    ▼
ResponseOrchestrator:
  - 并行调用:
    1. RAGService 获取积压诊断知识
    2. MCP 获取实时 backlog 数据
  - 合并上下文
  - 生成综合回答
```

### 数据模型

#### RoutingContext

```java
public record RoutingContext(
    RouteType routeType,
    List<String> knowledgeQueries,
    List<McpToolCall> mcpToolCalls,
    boolean parallel
) {}

public record McpToolCall(
    String toolName,
    Map<String, Object> arguments
) {}
```

#### CompositeContext

```java
public record CompositeContext(
    String knowledgeContext,
    String mcpContext,
    String mergedContext,
    Map<String, Object> metadata
) {}
```

### 配置项

```yaml
pulsar-diagnostic:
  orchestration:
    # 并行获取超时
    parallel-timeout-ms: 5000
    # MCP 数据获取超时
    mcp-timeout-ms: 3000
    # 知识检索超时
    knowledge-timeout-ms: 2000
    # 上下文合并策略
    context-merge-strategy: "weighted"  # weighted, sequential, prioritized
    # 权重配置
    knowledge-weight: 0.4
    mcp-weight: 0.6
```

### 实现计划

#### Phase 1: 基础组件
- [ ] 增强 IntentResult 添加路由信息
- [ ] 更新 IntentRecognizer 提示词
- [ ] 创建 McpDataResult 数据模型

#### Phase 2: MCP 数据服务
- [ ] 实现 McpDataService
- [ ] 添加智能工具选择逻辑
- [ ] 实现并行数据获取

#### Phase 3: 响应编排
- [ ] 实现 ResponseOrchestrator
- [ ] 添加路由策略
- [ ] 实现上下文合并

#### Phase 4: 集成重构
- [ ] 重构 ChatService 使用编排器
- [ ] 重构 DiagnosticService 集成 MCP
- [ ] 更新 KnowledgeQAService 支持混合模式

#### Phase 5: 测试验证
- [ ] 单元测试
- [ ] 集成测试
- [ ] 端到端测试

### 预期效果

1. **智能路由**: 根据问题自动选择数据源
2. **高效并行**: 知识和实时数据并行获取
3. **综合回答**: 结合知识和实时数据生成回答
4. **可扩展**: 易于添加新的数据源和工具

---

## 详细实现记录

### 2026-03-05 实施过程

#### Phase 1: 基础组件

**新增文件:**
1. `RouteType.java` - 路由类型枚举，定义四种路由策略
   - `KNOWLEDGE_ONLY` - 仅知识库
   - `MCP_ONLY` - 仅 MCP 实时数据
   - `HYBRID` - 知识库 + MCP
   - `GENERAL_CHAT` - 通用对话

2. `McpDataResult.java` - MCP 数据获取结果模型
   - 封装工具调用结果
   - 聚合上下文文本
   - 错误处理

3. `OrchestratorResponse.java` - 编排器响应模型
   - 最终回复内容
   - 路由类型信息
   - 数据来源追溯

**修改文件:**
1. `IntentResult.java` - 增强意图识别结果
   - 添加 `needsMcpData` 字段
   - 添加 `suggestedMcpTools` 字段
   - 添加 `routeType` 字段

#### Phase 2: MCP 数据服务

**新增文件:**
1. `McpDataService.java` - MCP 数据服务
   - 意图到工具的映射关系
   - 并行工具调用
   - 结果聚合

关键实现：
```java
// 意图到 MCP 工具的映射
private static final Map<String, String[]> INTENT_TOOL_MAPPING = Map.of(
    "backlog-diagnosis", new String[]{"get_topic_backlog", "get_consumer_stats"},
    "cluster-health-check", new String[]{"inspect_cluster", "get_broker_metrics"},
    ...
);
```

#### Phase 3: 响应编排

**新增文件:**
1. `ResponseOrchestrator.java` - 响应编排服务
   - 根据路由类型执行策略
   - 并行获取知识和 MCP 数据
   - 构建统一上下文
   - LLM 综合回答生成

处理流程：
```
用户问题 → 意图识别 → 路由判断 → 数据获取 → LLM 综合 → 返回结果
```

#### Phase 4: 集成重构

**修改文件:**
1. `IntentRecognizer.java` - 增强意图识别
   - 添加 MCP 需求判断逻辑
   - 添加工具建议
   - 添加路由类型推断

2. `ChatService.java` - 使用编排器
   - 注入 `ResponseOrchestrator`
   - 使用编排器处理请求
   - 格式化响应输出

3. `application.yml` - 添加编排配置
   - 并行获取超时
   - MCP 超时
   - 知识检索超时

#### 测试验证

```bash
# 编译验证
mvn clean compile -DskipTests
# 结果: BUILD SUCCESS

# 单元测试
mvn test
# 结果: Tests run: 11, Failures: 0, Errors: 0
```

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `intent/RouteType.java` | 新增 | 路由类型枚举 |
| `dto/McpDataResult.java` | 新增 | MCP 数据结果模型 |
| `dto/OrchestratorResponse.java` | 新增 | 编排器响应模型 |
| `intent/IntentResult.java` | 修改 | 添加路由相关字段 |
| `intent/IntentRecognizer.java` | 修改 | 增强 MCP 需求判断 |
| `service/McpDataService.java` | 新增 | MCP 数据服务 |
| `service/ResponseOrchestrator.java` | 新增 | 响应编排服务 |
| `service/ChatService.java` | 修改 | 使用编排器 |
| `application.yml` | 修改 | 添加编排配置 |

## 架构优化效果

1. **智能路由**: 根据问题自动判断数据来源
2. **并行获取**: 知识和实时数据并行获取，提升响应速度
3. **综合回答**: 结合知识库和实时集群数据生成回答
4. **可扩展性**: 易于添加新的数据源和工具