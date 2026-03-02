---
name: backlog-diagnosis
description: Use when diagnosing message backlog issues in Pulsar topics and subscriptions
---

# Backlog Diagnosis Skill

## Overview

Diagnose and analyze message backlog issues in Apache Pulsar topics and subscriptions.

## When to Use

Use this skill when:
- User reports message backlog growing
- Consumer lag issues
- Messages not being consumed
- Subscription backlog warnings

## Process

Follow these steps in order:

### 1. Gather Information

Use the available tools to collect data:
- `getClusterMetrics()` - Check overall cluster backlog
- `getTopicInfo(topicName)` - Get topic statistics
- `getTopicSubscriptions(topicName)` - Check subscription status
- `checkTopicBacklog(topicName)` - Detailed backlog analysis

### 2. Analyze Root Cause

Identify the root cause from:
- **No consumers** - Subscription exists but no active consumers
- **Slow consumers** - Consumers active but processing too slowly
- **Consumer errors** - Processing failures causing redelivery
- **Producer surge** - Sudden increase in message rate
- **Resource constraints** - Broker/bookie resource issues

### 3. Provide Diagnosis

Structure your diagnosis as:
```
## Backlog Diagnosis Report

### Issue Summary
[Brief description of the backlog situation]

### Affected Resources
- Topic: [topic name]
- Subscription: [subscription name]
- Current backlog: [X messages / Y bytes]

### Root Cause
[Identified root cause with evidence]

### Recommendations
1. [Immediate action]
2. [Short-term fix]
3. [Long-term improvement]
```

## Available Tools

| Tool | Purpose |
|------|---------|
| `getClusterMetrics` | Get overall cluster metrics including backlog |
| `getTopicInfo` | Get detailed topic information |
| `getTopicSubscriptions` | List all subscriptions for a topic |
| `checkTopicBacklog` | Detailed backlog check for a topic |
| `diagnoseBacklogIssue` | Specialized backlog diagnosis |

## Example Usage

**User query:** "My topic has a growing backlog, can you help?"

**Response pattern:**
1. First call `getClusterMetrics()` to understand overall situation
2. Then call `getTopicInfo(topic)` for the specific topic
3. Call `getTopicSubscriptions(topic)` to check consumers
4. Analyze and provide diagnosis with recommendations

## Red Flags

Watch for these warning signs:
- Backlog growing faster than consumption rate
- No active consumers on subscription
- Consumer with 0 message rate
- Backlog exceeding 100MB per topic