# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run the application (from project root)
mvn spring-boot:run -pl pulsar-diagnostic-web

# Run tests
mvn test

# Run tests for a specific module
mvn test -pl pulsar-diagnostic-core
```

## Environment Configuration

The application requires these environment variables (see `application.yml`):
- `OPENAI_API_KEY` - OpenAI API key for Spring AI
- `PULSAR_ADMIN_URL` - Pulsar admin URL (default: http://localhost:8080)
- `PULSAR_BROKER_URL` - Pulsar broker URL (default: pulsar://localhost:6650)
- `PROMETHEUS_URL` - Prometheus URL for metrics (default: http://localhost:9090)
- `PULSAR_CLUSTER_NAME` - Cluster name (default: standalone)
- `MCP_SERVER_URL` - MCP server URL for tool integration (default: http://localhost:8000)

## Architecture

This is a multi-module Maven project for an AI-powered Apache Pulsar diagnostic system.

### Module Dependency Graph
```
pulsar-diagnostic-web (entry point)
    └── pulsar-diagnostic-agent (AI services)
        ├── pulsar-diagnostic-knowledge (RAG/embeddings)
        └── pulsar-diagnostic-core (Pulsar integration)
            └── pulsar-diagnostic-common (shared models)
```

### Module Responsibilities

**pulsar-diagnostic-common**: Shared domain models, enums, exceptions, and utilities. Contains `DiagnosticResult`, `PulsarCluster`, `BrokerInfo`, `TopicInfo`, health status enums, and JSON utilities.

**pulsar-diagnostic-core**: Integration with Pulsar and Prometheus.
- `PulsarAdminClient` - Wrapper for Pulsar Admin API (brokers, bookies, topics, namespaces)
- `PrometheusMetricsCollector` - Queries Prometheus for Pulsar metrics
- `HealthCheckService` - Scheduled health checks (every 30 seconds)
- `LogFileReader` - Reads Pulsar component logs

**pulsar-diagnostic-knowledge**: RAG-based knowledge base using Spring AI.
- `KnowledgeBaseService` - Main entry point, loads documents on startup
- `KnowledgeVectorStore` - In-memory vector store with similarity search
- `EmbeddingService` - OpenAI embeddings integration
- Knowledge documents in `src/main/resources/knowledge/`

**pulsar-diagnostic-agent**: AI agent services using Spring AI.
- `PulsarDiagnosticAgent` - Main agent facade
- `DiagnosticService` - Issue diagnosis with RAG context
- `InspectionService` - Cluster health inspections
- `ChatService` - Conversational interface with streaming support
- `PromptTemplates` - System prompts for different modes

**pulsar-diagnostic-web**: REST API and WebSocket endpoints.
- `DiagnosticController` - `/api/diagnose` endpoints
- `ChatController` - `/api/chat` endpoints
- `InspectionController` - `/api/inspection` endpoints
- `ChatWebSocketHandler` - WebSocket for streaming responses

## Key Patterns

### Adding New Diagnostic Capabilities
1. Add any new domain models to `pulsar-diagnostic-common`
2. Implement data collection in `pulsar-diagnostic-core`
3. Add knowledge documents to `pulsar-diagnostic-knowledge/src/main/resources/knowledge/`
4. Update `PromptTemplates` if new diagnostic modes are needed

### Pulsar Admin API
Uses Apache Pulsar 2.10.4 client. The `PulsarAdminClient` wraps admin operations with simplified model classes. TLS and token authentication are supported via configuration.

### Spring AI Integration
- Uses `ChatClient` for LLM interactions
- OpenAI models configurable in `application.yml` (default: gpt-4)
- Embeddings use `text-embedding-ada-002`
- MCP (Model Context Protocol) integration for external tool access

## API Quick Reference

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/diagnose` | POST | Diagnose issue from description or symptoms |
| `/api/diagnose/topic/{topicName}/backlog` | GET | Diagnose topic backlog issues |
| `/api/diagnose/connections` | GET | Diagnose connection issues |
| `/api/diagnose/performance` | GET | Diagnose performance issues |
| `/api/chat` | POST | Chat with the agent |
| `/api/inspection` | POST | Perform cluster inspection |
| `/api/status` | GET | Get system status |