# RAG 能力重构实施记录

## 概述

**日期**: 2026-03-05
**任务**: 重构知识管理模块，实现高级 RAG 能力
**提交**: a83bf03

## 背景

原有知识库仅支持单路向量检索，用户要求实现：
1. 混合检索（向量 + BM25 关键词）
2. LLM Reranking 重排序
3. 查询增强（重写、扩展、实体提取）

## 架构设计

### 模块结构

```
pulsar-diagnostic-knowledge/
├── retrieval/           # 检索层
│   ├── DocumentRetriever.java    # 检索器接口
│   ├── VectorRetriever.java      # 向量检索器
│   ├── BM25Retriever.java        # BM25 检索器
│   ├── HybridRetriever.java      # 混合检索器（RRF融合）
│   └── RetrievalResult.java      # 检索结果模型
├── rerank/             # 重排层
│   ├── Reranker.java             # 重排器接口
│   └── RerankResult.java         # 重排结果模型
├── query/              # 查询增强层
│   ├── QueryEnhancer.java        # 查询增强接口
│   └── EnhancedQuery.java        # 增强查询模型
└── rag/                # RAG 服务层
    ├── RAGService.java           # RAG 服务入口
    ├── RAGConfig.java            # RAG 配置
    ├── RAGRequest.java           # RAG 请求
    └── RAGResponse.java          # RAG 响应

pulsar-diagnostic-agent/
└── rag/                # LLM 实现层（依赖 ChatClient）
    ├── LLMQueryEnhancer.java     # LLM 查询增强
    └── LLMReranker.java          # LLM 重排器
```

### RAG 处理流程

```
用户查询
    ↓
[查询增强] → 重写 + 扩展 + 实体提取
    ↓
[混合检索] → 向量检索 + BM25检索 → RRF融合
    ↓
[Reranking] → LLM打分重排
    ↓
[上下文构建] → 生成最终上下文
    ↓
返回结果
```

## 关键实现

### 1. BM25 检索器

```java
@Component
public class BM25Retriever implements DocumentRetriever {
    // BM25 参数
    private static final double K1 = 1.2;   // 词频饱和参数
    private static final double B = 0.75;   // 文档长度归一化参数

    // 支持中文简单分词
    private static final Pattern CHINESE_TOKENIZER = Pattern.compile(
        "[\\s\\p{Punct}\\p{Punctuation}]+|(?<=[\\u4e00-\\u9fa5])(?=[A-Za-z0-9])|(?<=[A-Za-z0-9])(?=[\\u4e00-\\u9fa5])"
    );

    // 倒排索引：term -> 文档ID列表
    private final Map<String, List<IndexEntry>> invertedIndex = new HashMap<>();
}
```

### 2. 混合检索融合

```java
// Reciprocal Rank Fusion (RRF) 算法
// score = 1 / (k + rank), k = 60
private List<RetrievalResult> reciprocalRankFusion(
        List<RetrievalResult> vectorResults,
        List<RetrievalResult> bm25Results,
        int topK) {

    // 融合分数 = 向量权重 * RRF(向量排名) + BM25权重 * RRF(BM25排名)
    Map<String, Double> fusionScores = new HashMap<>();

    for (int rank = 0; rank < vectorResults.size(); rank++) {
        double rrfScore = vectorWeight / (rrfK + rank + 1);
        fusionScores.merge(docId, rrfScore, Double::sum);
    }

    for (int rank = 0; rank < bm25Results.size(); rank++) {
        double rrfScore = bm25Weight / (rrfK + rank + 1);
        fusionScores.merge(docId, rrfScore, Double::sum);
    }

    return fusionScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topK)
            .collect(Collectors.toList());
}
```

### 3. LLM Reranking

```java
private static final String RERANK_PROMPT = """
    你是一个相关性评分专家。请评估以下文档与用户查询的相关性。

    用户查询: %s
    文档内容: %s

    评分标准:
    - 1分: 高度相关，直接回答用户问题
    - 2分: 部分相关，包含有用信息但不完整
    - 3分: 低相关性，仅有边缘联系
    - 4分: 不相关

    请只输出JSON格式: {"score": 1, "reason": "简短理由"}
    """;
```

### 4. 查询增强

```java
private static final String ENHANCE_PROMPT = """
    你是一个查询增强专家。请分析用户查询并进行增强处理。

    用户查询: %s

    请执行以下增强操作:
    1. 查询重写: 将模糊查询转换为更精确的查询
    2. 查询扩展: 生成3个相关的扩展查询
    3. 实体提取: 提取查询中的关键实体
    4. 意图识别: 判断用户意图

    JSON格式输出。
    """;
```

## 配置项

```yaml
pulsar-diagnostic:
  rag:
    enabled: ${RAG_ENABLED:true}
    retrieval:
      top-k: ${RAG_RETRIEVAL_TOP_K:20}
      vector-weight: ${RAG_VECTOR_WEIGHT:0.6}
      bm25-weight: ${RAG_BM25_WEIGHT:0.4}
    rerank:
      enabled: ${RAG_RERANK_ENABLED:true}
      top-k: ${RAG_RERANK_TOP_K:5}
      batch-size: ${RAG_RERANK_BATCH_SIZE:5}
      timeout-seconds: ${RAG_RERANK_TIMEOUT:30}
    query-enhancement:
      enabled: ${RAG_QUERY_ENHANCE_ENABLED:true}
      expansion-count: ${RAG_QUERY_EXPANSION_COUNT:3}
```

## 遇到的问题及解决

### 问题1: ChatClient 依赖问题

**现象**: `LLMReranker` 和 `LLMQueryEnhancer` 在 knowledge 模块中无法编译
**原因**: `ChatClient` 来自 `spring-ai-client-chat`，只在 agent 模块中有依赖
**解决**: 将 LLM 实现类移动到 agent 模块，接口保留在 knowledge 模块

### 问题2: Document 构造函数

**现象**: 测试中 `new Document("id", "content")` 报错
**原因**: Spring AI 2.0.0-M2 中 Document 构造函数签名变化
**解决**: 使用 `new Document("id", "content", Map.of())` 或 `new Document("content", Map.of())`

### 问题3: metadata 不能为 null

**现象**: `IllegalArgumentException: metadata cannot be null`
**解决**: 传入空 Map 而非 null: `Map.of()`

## 测试验证

```bash
# 编译验证
mvn clean compile -DskipTests

# 单元测试
mvn test

# 结果: 所有 11 个测试通过
```

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `retrieval/DocumentRetriever.java` | 新增 | 检索器接口 |
| `retrieval/VectorRetriever.java` | 新增 | 向量检索器 |
| `retrieval/BM25Retriever.java` | 新增 | BM25 检索器 |
| `retrieval/HybridRetriever.java` | 新增 | 混合检索器 |
| `retrieval/RetrievalResult.java` | 新增 | 检索结果模型 |
| `rerank/Reranker.java` | 新增 | Reranker 接口 |
| `rerank/RerankResult.java` | 新增 | 重排结果模型 |
| `query/QueryEnhancer.java` | 新增 | 查询增强接口 |
| `query/EnhancedQuery.java` | 新增 | 增强查询模型 |
| `rag/RAGService.java` | 新增 | RAG 服务入口 |
| `rag/RAGConfig.java` | 新增 | RAG 配置 |
| `rag/RAGRequest.java` | 新增 | RAG 请求 |
| `rag/RAGResponse.java` | 新增 | RAG 响应 |
| `agent/rag/LLMQueryEnhancer.java` | 新增 | LLM 查询增强实现 |
| `agent/rag/LLMReranker.java` | 新增 | LLM 重排器实现 |
| `KnowledgeBaseService.java` | 修改 | 添加 BM25 索引支持 |
| `KnowledgeQAService.java` | 修改 | 集成 RAGService |
| `KnowledgeQAServiceTest.java` | 修改 | 更新测试 |
| `application.yml` | 修改 | 添加 RAG 配置 |

## 提交信息

```
commit a83bf03
Author: ...
Date:   2026-03-05

Add advanced RAG capabilities with hybrid retrieval and LLM reranking

- Implement hybrid retrieval combining vector similarity and BM25 keyword search
- Add LLM-based reranking for improved relevance scoring
- Implement query enhancement with rewriting and expansion
- Add BM25Retriever with Chinese tokenization support
- Create RAGService for unified retrieval pipeline
- Add configurable RAG settings in application.yml
```

## 后续优化建议

1. **性能优化**: BM25 索引可持久化到文件，避免每次启动重建
2. **分词优化**: 可集成 HanLP 等专业中文分词库
3. **缓存层**: 对 RAG 结果添加缓存，减少重复计算
4. **监控指标**: 添加检索延迟、召回率等监控指标
5. **A/B 测试**: 支持对比不同检索策略的效果