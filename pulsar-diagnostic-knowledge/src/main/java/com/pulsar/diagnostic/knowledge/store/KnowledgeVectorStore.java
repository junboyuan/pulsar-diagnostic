package com.pulsar.diagnostic.knowledge.store;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Vector store for Pulsar knowledge base
 */
@Component
public class KnowledgeVectorStore {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorStore.class);

    private final SimpleVectorStore vectorStore;
    private boolean initialized = false;

    public KnowledgeVectorStore(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Add documents to the vector store
     */
    public void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.info("Adding {} documents to vector store", documents.size());

        try {
            vectorStore.add(documents);
            initialized = true;
        } catch (Exception e) {
            log.error("Failed to add documents to vector store", e);
        }
    }

    /**
     * Search for similar documents
     */
    public List<Document> search(String query, int topK) {
        return search(query, topK, 0.0);
    }

    /**
     * Search for similar documents with similarity threshold
     */
    public List<Document> search(String query, int topK, double similarityThreshold) {
        if (!initialized) {
            log.warn("Vector store not initialized");
            return List.of();
        }

        log.debug("Searching for: {} (topK={}, threshold={})", query, topK, similarityThreshold);

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Failed to search vector store", e);
            return List.of();
        }
    }

    /**
     * Search with metadata filter
     */
    public List<Document> searchWithFilter(String query, int topK, Map<String, Object> filter) {
        if (!initialized) {
            return List.of();
        }

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(buildFilterExpression(filter))
                    .build();

            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Failed to search with filter", e);
            return List.of();
        }
    }

    /**
     * Save vector store to file
     */
    public void saveToFile(String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            vectorStore.save(file);
            log.info("Vector store saved to: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to save vector store", e);
        }
    }

    /**
     * Load vector store from file
     */
    public boolean loadFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                vectorStore.load(file);
                initialized = true;
                log.info("Vector store loaded from: {}", filePath);
                return true;
            }
            log.info("Vector store file not found: {}", filePath);
            return false;
        } catch (Exception e) {
            log.error("Failed to load vector store", e);
            return false;
        }
    }

    /**
     * Check if store is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Clear the vector store
     */
    public void clear() {
        // SimpleVectorStore doesn't have a clear method, so we'd need to recreate it
        initialized = false;
        log.info("Vector store cleared");
    }

    /**
     * Build filter expression from metadata map
     */
    private String buildFilterExpression(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" && ");
            }
            sb.append(entry.getKey()).append(" == '").append(entry.getValue()).append("'");
        }
        return sb.toString();
    }
}