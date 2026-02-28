package com.pulsar.diagnostic.common.constant;

/**
 * Constants for Pulsar Diagnostic system
 */
public final class DiagnosticConstants {

    private DiagnosticConstants() {
        // Prevent instantiation
    }

    // System info
    public static final String SYSTEM_NAME = "Pulsar Diagnostic AI Agent";
    public static final String SYSTEM_VERSION = "1.0.0";

    // Default values
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 1000;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_METRICS_PORT = 9090;
    public static final int DEFAULT_ADMIN_PORT = 8080;
    public static final int DEFAULT_BROKER_PORT = 6650;

    // Message related
    public static final long BACKLOG_WARNING_THRESHOLD = 10000L;
    public static final long BACKLOG_CRITICAL_THRESHOLD = 100000L;
    public static final int MAX_TOPIC_PARTITIONS = 100;

    // Time constants
    public static final long METRICS_COLLECTION_INTERVAL_MS = 60000L;
    public static final long HEALTH_CHECK_INTERVAL_MS = 30000L;

    // Log analysis
    public static final int LOG_ANALYSIS_MAX_LINES = 10000;
    public static final int LOG_TAIL_LINES = 100;

    // Knowledge base
    public static final int EMBEDDING_DIMENSION = 1536;
    public static final int MAX_DOCUMENT_CHUNK_SIZE = 1000;
    public static final int KNOWLEDGE_TOP_K = 5;

    // HTTP headers
    public static final String HEADER_CLUSTER_NAME = "X-Cluster-Name";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    // Agent roles
    public static final String AGENT_ROLE_CHAT = "chat";
    public static final String AGENT_ROLE_DIAGNOSTIC = "diagnostic";
    public static final String AGENT_ROLE_INSPECTION = "inspection";
}