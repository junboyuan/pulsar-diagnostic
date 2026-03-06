---
name: disk-issue
description: 诊断 Pulsar 磁盘空间和 I/O 问题。当用户报告磁盘满、存储不足、I/O 错误时使用。
parent-skill: diagnosis
route-type: HYBRID
allowed-tools: diagnoseDiskIssues, checkDiskSpace, inspectCluster, getResourceUsage
---

# 磁盘问题诊断

## 概述

诊断 Apache Pulsar 组件的磁盘空间和 I/O 问题，包括磁盘满、I/O 性能下降等。

## 适用场景

在以下情况下使用此技能：
- 磁盘空间不足
- 磁盘 I/O 性能问题
- Bookie 进入只读模式
- 写入被拒绝

## 问题分类

### 1. 磁盘空间问题

| 组件 | 问题 | 影响 |
|------|------|------|
| Broker | 日志文件过大 | 日志写入失败 |
| Bookie | Ledger 目录满 | 进入只读模式 |
| ZooKeeper | 数据目录满 | 无法写入快照 |

### 2. 磁盘 I/O 问题

| 问题 | 症状 | 诊断方法 |
|------|------|----------|
| I/O 延迟高 | 写入延迟增加 | `iostat -x 1` |
| 吞吐量不足 | 性能下降 | 检查磁盘带宽 |
| 磁盘错误 | I/O 错误日志 | `dmesg` 检查 |

## 处理流程

### 1. 检查磁盘状态

```
diagnoseDiskIssues(component?) → 磁盘诊断
checkDiskSpace() → 磁盘空间检查
getResourceUsage() → 资源使用情况
```

### 2. 磁盘阈值

| 状态 | 使用率 | 说明 |
|------|--------|------|
| 正常 | < 70% | 绿色 |
| 警告 | 70-85% | 需要关注 |
| 高危 | 85-95% | 需要立即处理 |
| 严重 | > 95% | Bookie 进入只读 |

### 3. 生成诊断报告

```
## 磁盘问题诊断报告

### 磁盘使用情况
- Broker 磁盘：X%
- Bookie 磁盘：Y%
- ZooKeeper 磁盘：Z%

### 识别的问题
- [问题1]：[详细说明]
- [问题2]：[详细说明]

### 建议措施
1. [立即清理]
2. [配置调整]
3. [扩容建议]
```

## 常见解决方案

### 清理磁盘空间
```bash
# 清理旧日志
find /var/log/pulsar -name "*.log.*" -mtime +7 -delete

# 清理 Bookie 日志
bookkeeper shell gc
```

### 调整 Bookie 阈值
```properties
# bookkeeper.conf
diskUsageThreshold=0.90
diskUsageWarnThreshold=0.95
diskCheckInterval=10000
```

### 配置日志轮转
```yaml
# log4j2.yaml
RollingFile:
  - filePattern: "pulsar.%d{yyyy-MM-dd}-%i.log"
    policies:
      time: daily
      size: 100MB
```

## 预防措施

1. 设置磁盘使用告警（80% 阈值）
2. 配置自动日志清理
3. 监控磁盘 I/O 性能
4. 定期检查磁盘健康