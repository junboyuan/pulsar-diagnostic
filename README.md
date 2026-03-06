# Pulsar Diagnostic AI Agent

AI-powered Apache Pulsar diagnostic and troubleshooting system.

## Overview

This project provides an intelligent diagnostic agent for Apache Pulsar clusters. It combines AI capabilities with real-time cluster data to help identify and resolve issues quickly.

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- OpenAI API Key (for AI features)
- Apache Pulsar cluster (optional, for real diagnostics)
- MCP Mock Server (for testing without real Pulsar)

### Build the Project

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests
```

### Configuration

Set the following environment variables:

```bash
# Required for AI features
export OPENAI_API_KEY=your-api-key

# Pulsar cluster configuration (optional)
export PULSAR_ADMIN_URL=http://localhost:8080
export PULSAR_BROKER_URL=pulsar://localhost:6650
export PROMETHEUS_URL=http://localhost:9090
export PULSAR_CLUSTER_NAME=standalone

# MCP server configuration
export MCP_SERVER_URL=http://localhost:8000
export MCP_ENABLED=true
```

### Running the Application

#### Option 1: Run with Mock Server (Recommended for Testing)

```bash
# Terminal 1: Start the MCP Mock Server
mvn spring-boot:run -pl mcp-mock-server

# Terminal 2: Start the main application
export MCP_SERVER_URL=http://localhost:8000
mvn spring-boot:run -pl pulsar-diagnostic-web
```

#### Option 2: Run with Real Pulsar Cluster

```bash
# Ensure Pulsar is running and environment variables are set
mvn spring-boot:run -pl pulsar-diagnostic-web
```

### Verify the Application

```bash
# Check health status
curl http://localhost:8080/actuator/health

# Check API documentation
open http://localhost:8080/swagger-ui.html
```

## API Reference

### Diagnostic Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/diagnose` | POST | Diagnose an issue from description |
| `/api/diagnose/topic/{topicName}/backlog` | GET | Diagnose topic backlog issues |
| `/api/diagnose/connections` | GET | Diagnose connection issues |
| `/api/diagnose/performance` | GET | Diagnose performance issues |

### Chat Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/chat` | POST | Chat with the diagnostic agent |
| `/api/chat/qa` | POST | Knowledge-based Q&A |
| `/api/chat/stream` | POST | Streaming chat response (SSE) |

### Inspection Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/inspection` | POST | Perform cluster inspection |

### Example Requests

#### Diagnose an Issue

```bash
curl -X POST http://localhost:8080/api/diagnose \
  -H "Content-Type: application/json" \
  -d '{
    "issue": "My topic has a growing backlog and consumers are slow"
  }'
```

#### Chat with the Agent

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How can I optimize my Pulsar cluster performance?"
  }'
```

#### Diagnose Topic Backlog

```bash
curl http://localhost:8080/api/diagnose/topic/persistent://public/default/my-topic/backlog
```

## Architecture

### Module Structure

```
pulsar-diagnostic-web (entry point, port 8080)
    └── pulsar-diagnostic-agent (AI services)
        ├── pulsar-diagnostic-knowledge (RAG/embeddings)
        └── pulsar-diagnostic-core (Pulsar integration)
            └── pulsar-diagnostic-common (shared models)

mcp-mock-server (mock MCP server, port 8000)
```

### Module Responsibilities

| Module | Description |
|--------|-------------|
| `pulsar-diagnostic-common` | Shared domain models, enums, exceptions |
| `pulsar-diagnostic-core` | Pulsar Admin API, Prometheus integration |
| `pulsar-diagnostic-knowledge` | RAG-based knowledge base with embeddings |
| `pulsar-diagnostic-agent` | AI agent services, diagnostic logic |
| `pulsar-diagnostic-web` | REST API, WebSocket endpoints |
| `mcp-mock-server` | Mock MCP server for testing |

## MCP Mock Server

The MCP Mock Server provides a self-contained testing environment without requiring a real Pulsar cluster.

### Start Mock Server

```bash
mvn spring-boot:run -pl mcp-mock-server
```

### Mock Server Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/tools` | GET | List all available tools |
| `/api/tools/{toolName}` | POST | Execute a tool |
| `/api/tools/health` | GET | Health check |

### Available Mock Tools (18 tools)

| Tool | Description |
|------|-------------|
| `inspect_cluster` | Inspect cluster status |
| `get_broker_metrics` | Get broker performance metrics |
| `get_topic_metrics` | Get topic performance metrics |
| `get_topic_backlog` | Get topic backlog statistics |
| `get_consumer_stats` | Get consumer statistics |
| `get_producer_stats` | Get producer statistics |
| `get_subscription_stats` | Get subscription statistics |
| `get_dlq_stats` | Get dead letter queue stats |
| `check_disk_space` | Check disk space usage |
| `check_auth_config` | Check authentication config |
| `get_permissions` | Get permissions info |
| `get_cluster_metrics` | Get cluster-wide metrics |
| `get_resource_usage` | Get resource usage |
| `get_topic_info` | Get topic information |
| `get_topic_config` | Get topic configuration |
| `list_brokers` | List all brokers |
| `list_topics` | List all topics |
| `check_subscription` | Check subscription status |

### Mock Server Scenarios

Configure different scenarios in `application.yml`:

```yaml
mock:
  scenario: normal  # normal, backlog, produce-slow, disk-full, auth-failure
  dynamic-data: true
  update-interval-seconds: 5
```

## Debugging

### Enable Debug Logging

```bash
# Set log level
export LOGGING_LEVEL_COM_PULSAR_DIAGNOSTIC=DEBUG

# Or in application.yml
logging:
  level:
    com.pulsar.diagnostic: DEBUG
```

### Common Issues

#### 1. OpenAI API Key Not Set

```
Error: Parameter 0 of method chatClient required a bean of type 'org.springframework.ai.chat.model.ChatModel'
```

**Solution:** Set the `OPENAI_API_KEY` environment variable.

#### 2. MCP Server Not Available

```
Error: MCP server not available
```

**Solution:** Start the MCP Mock Server or configure `MCP_SERVER_URL` to point to a real MCP server.

#### 3. Pulsar Connection Failed

```
Error: Failed to connect to Pulsar admin
```

**Solution:** Ensure Pulsar is running and `PULSAR_ADMIN_URL` is correctly configured.

### Health Checks

```bash
# Main application health
curl http://localhost:8080/actuator/health

# MCP Mock Server health
curl http://localhost:8000/api/tools/health

# Check available MCP tools
curl http://localhost:8000/api/tools
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Tests for Specific Module

```bash
# Test MCP Mock Server
mvn test -pl mcp-mock-server

# Test Core Module
mvn test -pl pulsar-diagnostic-core
```

### Run Integration Tests

```bash
mvn verify
```

## Development

### Project Structure

```
├── pulsar-diagnostic-common/     # Shared models
├── pulsar-diagnostic-core/       # Pulsar integration
├── pulsar-diagnostic-knowledge/  # RAG knowledge base
├── pulsar-diagnostic-agent/      # AI agent logic
├── pulsar-diagnostic-web/        # REST API
├── mcp-mock-server/              # Mock MCP server
└── pom.xml                       # Parent POM
```

### Adding New Diagnostic Capabilities

1. Add domain models to `pulsar-diagnostic-common`
2. Implement data collection in `pulsar-diagnostic-core`
3. Add knowledge documents to `pulsar-diagnostic-knowledge/src/main/resources/knowledge/`
4. Add MCP mock service in `mcp-mock-server`

### Code Style

- Java 17+ features
- Spring Boot 3.x conventions
- Lombok for boilerplate reduction

## License

This project is licensed under the Apache License 2.0.