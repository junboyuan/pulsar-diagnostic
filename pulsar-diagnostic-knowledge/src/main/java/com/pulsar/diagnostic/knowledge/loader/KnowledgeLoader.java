package com.pulsar.diagnostic.knowledge.loader;

import com.pulsar.diagnostic.knowledge.document.DocumentLoader;
import org.springframework.ai.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loader for Pulsar knowledge base documents
 */
@Component
public class KnowledgeLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLoader.class);

    private static final String KNOWLEDGE_PATH = "classpath:knowledge/";

    private final DocumentLoader documentLoader;

    public KnowledgeLoader(DocumentLoader documentLoader) {
        this.documentLoader = documentLoader;
    }

    /**
     * Load all knowledge documents
     */
    public List<Document> loadAllKnowledge() {
        List<Document> allDocuments = new ArrayList<>();

        // Load predefined knowledge documents
        allDocuments.addAll(loadTroubleshootingKnowledge());
        allDocuments.addAll(loadBestPracticesKnowledge());
        allDocuments.addAll(loadConfigurationKnowledge());

        // Load from knowledge directory if exists
        allDocuments.addAll(loadFromKnowledgeDirectory());

        log.info("Loaded {} knowledge documents", allDocuments.size());
        return allDocuments;
    }

    /**
     * Load troubleshooting knowledge
     */
    public List<Document> loadTroubleshootingKnowledge() {
        List<Document> documents = new ArrayList<>();

        // Common Pulsar troubleshooting issues
        String[] troubleshootingDocs = {
            """
            # Pulsar Troubleshooting: Message Backlog Issues

            ## Symptoms
            - Messages are not being consumed
            - Backlog size keeps growing
            - Consumer lag is increasing

            ## Common Causes
            1. Consumer is not running or is stuck
            2. Consumer processing is too slow
            3. Message processing errors causing redelivery loops
            4. Insufficient consumer instances

            ## Solutions
            1. Check consumer status and logs
            2. Increase consumer parallelism
            3. Enable batch processing for high throughput
            4. Check for message processing exceptions
            5. Monitor consumer metrics

            ## Related Metrics
            - pulsar_backlog_size
            - pulsar_consumer_msg_rate
            - pulsar_consumer_msg_ack_rate
            """,
            """
            # Pulsar Troubleshooting: Connection Issues

            ## Symptoms
            - Producers/consumers cannot connect to brokers
            - Connection timeouts
            - Frequent disconnections

            ## Common Causes
            1. Network connectivity issues
            2. Broker is down or overloaded
            3. Authentication/authorization failures
            4. Firewall blocking connections
            5. TLS certificate issues

            ## Solutions
            1. Check broker health status
            2. Verify network connectivity
            3. Check authentication credentials
            4. Review broker logs for errors
            5. Verify TLS configuration

            ## Related Metrics
            - pulsar_broker_connection_count
            - pulsar_broker_connection_closed
            """,
            """
            # Pulsar Troubleshooting: Broker Performance Issues

            ## Symptoms
            - High latency for message operations
            - Broker becomes unresponsive
            - Memory or CPU warnings

            ## Common Causes
            1. Insufficient broker resources
            2. Too many topics on single broker
            3. Inefficient consumer/producer configurations
            4. Large message sizes
            5. Journal disk I/O bottleneck

            ## Solutions
            1. Monitor broker JVM heap usage
            2. Enable topic auto-creation offloading
            3. Optimize batch size and max message size
            4. Consider broker scaling
            5. Use separate disks for journal and ledgers

            ## Related Metrics
            - pulsar_broker_memory_usage
            - pulsar_broker_cpu_usage
            - pulsar_journal_write_latency
            """,
            """
            # Pulsar Troubleshooting: Bookie Failures

            ## Symptoms
            - Write failures to BookKeeper
            - Ledger recovery issues
            - Missing entries in topics

            ## Common Causes
            1. Bookie disk full
            2. Bookie process crashed
            3. Network partition between broker and bookie
            4. ZooKeeper session expired
            5. Ledger ensemble issues

            ## Solutions
            1. Check bookie disk space
            2. Restart failed bookie
            3. Verify bookie is in writable state
            4. Check ensemble write quorum settings
            5. Monitor bookie metrics

            ## Related Metrics
            - pulsar_bookie_journal_write_latency
            - pulsar_bookie_ledger_read_latency
            - pulsar_bookie_disk_usage
            """
        };

        for (int i = 0; i < troubleshootingDocs.length; i++) {
            Document doc = documentLoader.createDocument(
                "troubleshooting-" + i,
                troubleshootingDocs[i],
                Map.of(
                    "category", "troubleshooting",
                    "index", String.valueOf(i)
                )
            );
            documents.add(doc);
        }

        return documents;
    }

    /**
     * Load best practices knowledge
     */
    public List<Document> loadBestPracticesKnowledge() {
        List<Document> documents = new ArrayList<>();

        String[] bestPracticesDocs = {
            """
            # Pulsar Best Practices: Topic Design

            ## Topic Naming Convention
            Use hierarchical naming: tenant/namespace/topic
            - tenant: Organization or application name
            - namespace: Logical grouping (environment, business domain)
            - topic: Specific data stream name

            ## Partitioning Strategy
            - Use partitioned topics for high throughput
            - Number of partitions = expected parallelism
            - Consider key-based ordering requirements
            - Balance between parallelism and overhead

            ## Topic Configuration
            - Set appropriate retention policies
            - Configure backlog quotas
            - Enable message TTL when appropriate
            - Use schema validation for data integrity
            """,
            """
            # Pulsar Best Practices: Producer Configuration

            ## Batching
            - Enable batching for high throughput
            - Set appropriate batch size (default 1000 messages)
            - Configure batch max bytes (default 1MB)
            - Balance latency vs throughput

            ## Compression
            - Use compression for large messages
            - ZSTD provides best compression ratio
            - LZ4 for lower CPU overhead

            ## Reliability
            - Use synchronous writes for critical data
            - Handle send failures with retry logic
            - Monitor producer metrics

            ## Memory Management
            - Configure appropriate max pending messages
            - Use memory limits to prevent OOM
            - Consider chunking for large messages
            """,
            """
            # Pulsar Best Practices: Consumer Configuration

            ## Subscription Types
            - Exclusive: Single consumer, ordered processing
            - Failover: HA with standby consumer
            - Shared: Multiple consumers, load balancing
            - Key_Shared: Parallel processing with key ordering

            ## Performance Tuning
            - Configure receiver queue size
            - Enable batch receive for efficiency
            - Set appropriate acknowledgment timeout
            - Use negative acknowledgments wisely

            ## Error Handling
            - Implement retry logic for failures
            - Use dead letter topics for poison pills
            - Handle redelivery count appropriately
            - Log and monitor consumer errors
            """,
            """
            # Pulsar Best Practices: Cluster Operations

            ## Resource Planning
            - Estimate required brokers based on throughput
            - Plan for 3+ bookies for redundancy
            - Size ZooKeeper ensemble appropriately
            - Consider geographical distribution

            ## Monitoring
            - Track key metrics (throughput, latency, backlog)
            - Set up alerts for critical thresholds
            - Monitor JVM health of all components
            - Track disk usage and I/O performance

            ## Maintenance
            - Regular health checks and inspections
            - Plan for rolling upgrades
            - Maintain backup and recovery procedures
            - Keep configurations in version control
            """
        };

        for (int i = 0; i < bestPracticesDocs.length; i++) {
            Document doc = documentLoader.createDocument(
                "best-practices-" + i,
                bestPracticesDocs[i],
                Map.of(
                    "category", "best-practices",
                    "index", String.valueOf(i)
                )
            );
            documents.add(doc);
        }

        return documents;
    }

    /**
     * Load configuration knowledge
     */
    public List<Document> loadConfigurationKnowledge() {
        List<Document> documents = new ArrayList<>();

        String[] configDocs = {
            """
            # Pulsar Configuration: Broker Settings

            ## Memory Configuration
            - managedLedgerDefaultEnsembleSize: Default ledger ensemble size (default: 3)
            - managedLedgerDefaultWriteQuorum: Write quorum size (default: 2)
            - managedLedgerDefaultAckQuorum: Ack quorum size (default: 2)

            ## Performance Tuning
            - maxConcurrentTopicLoadRequest: Concurrent topic loading (default: 5000)
            - numHttpServerThreads: HTTP handler threads (default: 2 * cores)
            - numIOThreads: Netty IO threads (default: 2 * cores)

            ## Message Settings
            - defaultRetentionTimeInMinutes: Default retention (default: 0)
            - defaultRetentionSizeInMB: Default size retention (default: 0)
            - maxMessageSize: Maximum message size (default: 5MB)
            """,
            """
            # Pulsar Configuration: BookKeeper Settings

            ## Ledger Configuration
            - journalDirectory: Journal storage location
            - ledgerDirectories: Ledger storage locations (comma-separated)
            - diskUsageThreshold: Disk usage limit (default: 0.95)
            - diskUsageWarnThreshold: Warning threshold (default: 0.90)

            ## Performance Settings
            - journalSyncData: Sync journal writes (default: true)
            - journalMaxGroupWaitMSec: Journal group wait (default: 1ms)
            - numAddWorkerThreads: Add worker threads (default: 4)
            - numReadWorkerThreads: Read worker threads (default: 4)
            """,
            """
            # Pulsar Configuration: Namespace Policies

            ## Retention Policies
            - retentionTime: How long to retain messages
            - retentionSize: Maximum retained data size
            - Example: Retain 7 days or 10GB

            ## Backlog Quotas
            - backlogQuotaDefaultLimitGB: Default backlog limit
            - backlogQuotaDefaultPolicy: Policy on limit exceeded
            - Options: producer_request_hold, producer_exception, consumer_backlog

            ## Message TTL
            - messageTTL: Time-to-live for messages in seconds
            - Applied when consumers are inactive

            ## Schema Settings
            - schemaValidationEnforced: Require schema validation
            - schemaCompatibilityStrategy: Schema evolution strategy
            """
        };

        for (int i = 0; i < configDocs.length; i++) {
            Document doc = documentLoader.createDocument(
                "configuration-" + i,
                configDocs[i],
                Map.of(
                    "category", "configuration",
                    "index", String.valueOf(i)
                )
            );
            documents.add(doc);
        }

        return documents;
    }

    /**
     * Load documents from knowledge directory
     */
    private List<Document> loadFromKnowledgeDirectory() {
        List<Document> documents = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_PATH + "*.md");

            for (Resource resource : resources) {
                try {
                    String content = new String(resource.getInputStream().readAllBytes());
                    List<Document> chunks = documentLoader.createChunks(
                        content,
                        Map.of("source", resource.getFilename())
                    );
                    documents.addAll(chunks);
                } catch (IOException e) {
                    log.warn("Failed to load knowledge file: {}", resource.getFilename());
                }
            }
        } catch (IOException e) {
            log.debug("No additional knowledge directory found");
        }

        return documents;
    }
}