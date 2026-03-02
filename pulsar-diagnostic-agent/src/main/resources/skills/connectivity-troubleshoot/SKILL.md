---
name: connectivity-troubleshoot
description: Use when troubleshooting connection and network issues in the cluster
---

# Connectivity Troubleshoot Skill

## Overview

Diagnose and resolve connection issues in Apache Pulsar clusters.

## When to Use

Use this skill when:
- Clients cannot connect to brokers
- Connection timeout errors
- Authentication/authorization failures
- Network partition issues

## Process

### 1. Verify Cluster Status

First check basic cluster health:
```
getClusterInfo() → performHealthCheck() → getActiveBrokers()
```

### 2. Diagnose Connection Issues

Use `diagnoseConnectionIssues()` to get specific diagnosis.

Common issues to investigate:
- **Broker unreachable**: Check if brokers are running
- **DNS resolution**: Verify broker URLs are correct
- **Port accessibility**: Check firewall rules
- **TLS/SSL**: Verify certificates

### 3. Check Authentication

If auth errors:
- Verify token validity
- Check role permissions
- Review authentication plugin configuration

### 4. Provide Troubleshooting Steps

```
## Connectivity Troubleshooting Report

### Issue Type: [Connection/Auth/Network]

### Diagnosis
[Root cause identified]

### Steps to Resolve
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Verification
[How to verify the fix worked]
```

## Available Tools

| Tool | Purpose |
|------|---------|
| `getClusterInfo` | Verify cluster is accessible |
| `performHealthCheck` | Check component health |
| `getActiveBrokers` | List reachable brokers |
| `diagnoseConnectionIssues` | Specialized connection diagnosis |

## Common Error Codes

| Error | Likely Cause | Solution |
|-------|--------------|----------|
| Connection refused | Broker down | Restart broker |
| Timeout | Network issue | Check firewall/network |
| Auth failed | Invalid credentials | Verify token/permissions |
| SSL error | Certificate issue | Renew/fix certificates |

## Red Flags

- Zero active brokers reachable
- All connections timing out
- Authentication failures after config change