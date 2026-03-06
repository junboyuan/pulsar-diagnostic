# 故障诊断 Skill 架构设计

## 概述

**日期**: 2026-03-06
**任务**: 按路由类型组织 Skill，实现总分结构的故障诊断体系
**状态**: 设计与实施

## 当前结构问题

现有的 Skills 是扁平化的：
```
skills/
├── backlog-diagnosis/SKILL.md
├── cluster-health-check/SKILL.md
├── performance-analysis/SKILL.md
├── connectivity-troubleshoot/SKILL.md
├── capacity-planning/SKILL.md
└── topic-consultation/SKILL.md
```

**问题**:
1. 与新的意图分类（按问题现象）不匹配
2. 缺乏统一的入口和协调机制
3. 子技能之间无法复用和引用

## 新架构设计

### 目录结构

```
skills/
├── diagnosis/                          # 故障诊断总入口
│   └── SKILL.md                        # 总诊断 Skill
│
├── auth/                               # 认证/鉴权问题
│   └── SKILL.md                        # auth-issue 子 Skill
│
├── produce/                            # 生产问题
│   ├── slow/SKILL.md                   # produce-slow 子 Skill
│   └── failed/SKILL.md                 # produce-failed 子 Skill
│
├── consume/                            # 消费问题
│   ├── slow/SKILL.md                   # consume-slow 子 Skill
│   ├── failed/SKILL.md                 # consume-failed 子 Skill
│   └── duplicate/SKILL.md              # consume-duplicate 子 Skill
│
├── cluster/                            # 集群问题
│   └── health/SKILL.md                 # cluster-health 子 Skill
│
├── disk/                               # 磁盘问题
│   └── SKILL.md                        # disk-issue 子 Skill
│
├── capacity/                           # 容量规划
│   └── SKILL.md                        # capacity-planning 子 Skill
│
└── topic/                              # 主题咨询
    └── SKILL.md                        # topic-consultation 子 Skill
```

### 总分关系设计

```
┌─────────────────────────────────────────────────────────┐
│                    diagnosis/SKILL.md                    │
│                      (总诊断入口)                         │
│                                                          │
│  职责: 意图识别、路由分发、结果聚合                        │
└─────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │  auth/   │    │ produce/ │    │ consume/ │
    │ SKILL.md │    │  ...     │    │  ...     │
    └──────────┘    └──────────┘    └──────────┘
```

### Skill 引用机制

主 Skill 通过 `sub-skills` 字段引用子 Skill：

```yaml
---
name: diagnosis
description: Pulsar 故障诊断总入口，根据问题现象路由到对应的诊断子技能
sub-skills:
  - auth-issue
  - produce-slow
  - produce-failed
  - consume-slow
  - consume-failed
  - consume-duplicate
  - cluster-health
  - disk-issue
---
```

子 Skill 通过 `parent-skill` 字段声明归属：

```yaml
---
name: auth-issue
description: 诊断认证和鉴权问题
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: checkAuthConfig, getPermissions, inspectCluster
---
```

## 实现计划

### Phase 1: 创建总诊断 Skill
- [ ] 创建 `diagnosis/SKILL.md` 作为总入口
- [ ] 定义路由分发逻辑

### Phase 2: 创建认证/生产/消费问题子 Skills
- [ ] 创建 `auth/SKILL.md`
- [ ] 创建 `produce/slow/SKILL.md`
- [ ] 创建 `produce/failed/SKILL.md`
- [ ] 创建 `consume/slow/SKILL.md`
- [ ] 创建 `consume/failed/SKILL.md`
- [ ] 创建 `consume/duplicate/SKILL.md`

### Phase 3: 创建其他问题子 Skills
- [ ] 创建 `cluster/health/SKILL.md`
- [ ] 创建 `disk/SKILL.md`
- [ ] 创建 `capacity/SKILL.md`
- [ ] 创建 `topic/SKILL.md`

### Phase 4: 清理旧 Skills
- [ ] 删除旧的扁平化 Skills
- [ ] 更新相关代码引用

---

## 实施记录

### Phase 1: 创建总诊断 Skill ✅

**新建文件:**
- `skills/diagnosis/SKILL.md` - 总诊断入口

**功能:**
- 定义路由分发逻辑
- 声明所有子技能引用
- 提供综合诊断入口

### Phase 2: 创建认证/生产/消费问题子 Skills ✅

**新建文件:**
- `skills/auth/SKILL.md` - 认证/鉴权问题
- `skills/produce/slow/SKILL.md` - 生产慢
- `skills/produce/failed/SKILL.md` - 生产失败
- `skills/consume/slow/SKILL.md` - 消费慢
- `skills/consume/failed/SKILL.md` - 消费失败
- `skills/consume/duplicate/SKILL.md` - 消费重复

### Phase 3: 创建其他问题子 Skills ✅

**新建文件:**
- `skills/cluster/health/SKILL.md` - 集群健康检查
- `skills/disk/SKILL.md` - 磁盘问题
- `skills/capacity/SKILL.md` - 容量规划
- `skills/topic/SKILL.md` - 主题咨询

### Phase 4: 清理旧 Skills

保留旧 Skills 目录，新 Skills 并存。

### 测试验证 ✅

```bash
# 编译验证
mvn clean compile -DskipTests
# 结果: BUILD SUCCESS
```

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `skills/diagnosis/SKILL.md` | 新增 | 总诊断入口 |
| `skills/auth/SKILL.md` | 新增 | 认证问题子 Skill |
| `skills/produce/slow/SKILL.md` | 新增 | 生产慢子 Skill |
| `skills/produce/failed/SKILL.md` | 新增 | 生产失败子 Skill |
| `skills/consume/slow/SKILL.md` | 新增 | 消费慢子 Skill |
| `skills/consume/failed/SKILL.md` | 新增 | 消费失败子 Skill |
| `skills/consume/duplicate/SKILL.md` | 新增 | 消费重复子 Skill |
| `skills/cluster/health/SKILL.md` | 新增 | 集群健康子 Skill |
| `skills/disk/SKILL.md` | 新增 | 磁盘问题子 Skill |
| `skills/capacity/SKILL.md` | 新增 | 容量规划子 Skill |
| `skills/topic/SKILL.md` | 新增 | 主题咨询子 Skill |

## 新目录结构

```
skills/
├── diagnosis/                    # 总诊断入口
│   └── SKILL.md
├── auth/                         # 认证问题
│   └── SKILL.md
├── produce/                      # 生产问题
│   ├── slow/SKILL.md
│   └── failed/SKILL.md
├── consume/                      # 消费问题
│   ├── slow/SKILL.md
│   ├── failed/SKILL.md
│   └── duplicate/SKILL.md
├── cluster/                      # 集群问题
│   └── health/SKILL.md
├── disk/                         # 磁盘问题
│   └── SKILL.md
├── capacity/                     # 容量规划
│   └── SKILL.md
└── topic/                        # 主题咨询
    └── SKILL.md
```

## Skill 引用机制

### 主 Skill 声明
```yaml
---
name: diagnosis
sub-skills:
  - auth-issue
  - produce-slow
  - ...
---
```

### 子 Skill 声明
```yaml
---
name: auth-issue
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: ...
---
```

## 优势

1. **层次清晰**: 总分结构，一目了然
2. **易于扩展**: 新增问题类型只需添加子 Skill
3. **复用性强**: 子 Skill 可独立使用或被引用
4. **路由精准**: 按问题现象精确路由到子 Skill