package com.pulsar.diagnostic.knowledge;

import com.pulsar.diagnostic.knowledge.document.DocumentLoader;
import com.pulsar.diagnostic.knowledge.loader.KnowledgeLoader;
import com.pulsar.diagnostic.knowledge.retrieval.BM25Retriever;
import com.pulsar.diagnostic.knowledge.store.KnowledgeVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main service for the Pulsar knowledge base
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeLoader knowledgeLoader;
    private final KnowledgeVectorStore vectorStore;
    private final BM25Retriever bm25Retriever;

    @Value("${pulsar-diagnostic.knowledge.embedding-file:#{null}}")
    private String embeddingFilePath;

    @Value("${pulsar-diagnostic.knowledge.auto-load:true}")
    private boolean autoLoad;

    private boolean initialized = false;

    public KnowledgeBaseService(KnowledgeLoader knowledgeLoader,
                                KnowledgeVectorStore vectorStore,
                                BM25Retriever bm25Retriever) {
        this.knowledgeLoader = knowledgeLoader;
        this.vectorStore = vectorStore;
        this.bm25Retriever = bm25Retriever;
    }

    /**
     * Initialize the knowledge base on startup
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing knowledge base...");

        // Try to load from saved embeddings first
        if (embeddingFilePath != null && vectorStore.loadFromFile(embeddingFilePath)) {
            initialized = true;
            log.info("Knowledge base loaded from file: {}", embeddingFilePath);
            return;
        }

        // If auto-load is enabled, load knowledge documents
        if (autoLoad) {
            loadKnowledge();
        }
    }

    /**
     * Load knowledge documents into the vector store
     */
    public void loadKnowledge() {
        log.info("Loading knowledge documents...");

        try {
            List<Document> documents = knowledgeLoader.loadAllKnowledge();

            if (!documents.isEmpty()) {
                // Add to vector store
                vectorStore.addDocuments(documents);

                // Add to BM25 index for hybrid retrieval
                bm25Retriever.addDocuments(documents);

                initialized = true;

                // Save to file if path is configured
                if (embeddingFilePath != null) {
                    vectorStore.saveToFile(embeddingFilePath);
                }

                log.info("Knowledge base initialized with {} documents (vector + BM25)", documents.size());
            }
        } catch (Exception e) {
            log.error("Failed to load knowledge base", e);
        }
    }

    /**
     * Search for relevant knowledge
     */
    public List<String> search(String query, int topK) {
        if (!initialized) {
            log.warn("Knowledge base not initialized");
            return List.of();
        }

        List<Document> results = vectorStore.search(query, topK);

        return results.stream()
                .map(doc -> doc.getText())
                .collect(Collectors.toList());
    }

    /**
     * Search with context for RAG
     */
    @Cacheable(value = "knowledge", key = "#query + '-' + #topK", unless = "#result == null || #result.items.isEmpty()")
    public KnowledgeContext searchWithContext(String query, int topK) {
        if (!initialized) {
            return new KnowledgeContext(query, List.of(), "");
        }

        List<Document> results = vectorStore.search(query, topK);

        List<KnowledgeItem> items = results.stream()
                .map(doc -> new KnowledgeItem(
                        doc.getId(),
                        doc.getText(),
                        doc.getMetadata()
                ))
                .collect(Collectors.toList());

        // Build context string
        String context = items.stream()
                .map(KnowledgeItem::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        return new KnowledgeContext(query, items, context);
    }

    /**
     * Search by category
     */
    public List<String> searchByCategory(String query, String category, int topK) {
        if (!initialized) {
            return List.of();
        }

        List<Document> results = vectorStore.searchWithFilter(
                query, topK, Map.of("category", category));

        return results.stream()
                .map(doc -> doc.getText())
                .collect(Collectors.toList());
    }

    /**
     * Add custom knowledge
     */
    public void addKnowledge(String id, String content, Map<String, Object> metadata) {
        Document document = new Document(id, content, metadata);
        vectorStore.addDocuments(List.of(document));
        bm25Retriever.addDocuments(List.of(document));
    }

    /**
     * Add multiple knowledge items
     */
    public void addKnowledgeBatch(List<Document> documents) {
        vectorStore.addDocuments(documents);
        bm25Retriever.addDocuments(documents);
    }

    /**
     * Check if knowledge base is ready
     */
    public boolean isReady() {
        return initialized && vectorStore.isInitialized();
    }

    /**
     * Get knowledge base statistics
     */
    public KnowledgeStats getStats() {
        return new KnowledgeStats(
                initialized,
                vectorStore.isInitialized(),
                embeddingFilePath
        );
    }

    /**
     * Save knowledge base to file
     */
    public void saveToFile(String filePath) {
        vectorStore.saveToFile(filePath);
    }

    // Inner classes

    /**
     * Knowledge context for RAG
     */
    public record KnowledgeContext(
            String query,
            List<KnowledgeItem> items,
            String context
    ) {}

    /**
     * Single knowledge item
     */
    public record KnowledgeItem(
            String id,
            String content,
            Map<String, Object> metadata
    ) {}

    /**
     * Knowledge base statistics
     */
    public record KnowledgeStats(
            boolean initialized,
            boolean vectorStoreReady,
            String embeddingFilePath
    ) {}
}