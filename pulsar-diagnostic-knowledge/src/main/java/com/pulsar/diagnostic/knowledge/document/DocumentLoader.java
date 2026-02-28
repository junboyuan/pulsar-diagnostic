package com.pulsar.diagnostic.knowledge.document;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loader for Pulsar documentation files
 */
@Component
public class DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;

    /**
     * Load document from classpath resource
     */
    public List<Document> loadFromClasspath(String resourcePath) {
        log.info("Loading document from classpath: {}", resourcePath);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Resource not found: {}", resourcePath);
                return List.of();
            }

            String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            return createChunks(content, Map.of("source", resourcePath));

        } catch (IOException e) {
            log.error("Failed to load document: {}", resourcePath, e);
            return List.of();
        }
    }

    /**
     * Load document from file system
     */
    public List<Document> loadFromFile(Path filePath) {
        log.info("Loading document from file: {}", filePath);

        if (!Files.exists(filePath)) {
            log.warn("File not found: {}", filePath);
            return List.of();
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return createChunks(content, Map.of("source", filePath.toString()));
        } catch (IOException e) {
            log.error("Failed to load file: {}", filePath, e);
            return List.of();
        }
    }

    /**
     * Load all documents from a directory
     */
    public List<Document> loadFromDirectory(String directoryPath, String fileExtension) {
        List<Document> allDocuments = new ArrayList<>();
        Path dirPath = Path.of(directoryPath);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            log.warn("Directory not found: {}", directoryPath);
            return allDocuments;
        }

        try {
            Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> fileExtension == null || path.toString().endsWith(fileExtension))
                    .forEach(path -> {
                        List<Document> docs = loadFromFile(path);
                        allDocuments.addAll(docs);
                    });
        } catch (IOException e) {
            log.error("Failed to walk directory: {}", directoryPath, e);
        }

        log.info("Loaded {} document chunks from {}", allDocuments.size(), directoryPath);
        return allDocuments;
    }

    /**
     * Create text chunks from content
     */
    public List<Document> createChunks(String content, Map<String, Object> metadata) {
        return createChunks(content, metadata, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * Create text chunks with custom size and overlap
     */
    public List<Document> createChunks(String content, Map<String, Object> metadata,
                                        int chunkSize, int overlap) {
        List<Document> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        // Split by paragraphs first
        String[] paragraphs = content.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                // Create chunk
                Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                chunkMetadata.put("chunk_index", chunkIndex++);

                chunks.add(new Document(currentChunk.toString().trim(), chunkMetadata));

                // Handle overlap
                if (overlap > 0 && currentChunk.length() > overlap) {
                    String overlapText = currentChunk.substring(currentChunk.length() - overlap);
                    currentChunk = new StringBuilder(overlapText);
                } else {
                    currentChunk = new StringBuilder();
                }
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        // Add final chunk
        if (currentChunk.length() > 0) {
            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("chunk_index", chunkIndex);
            chunks.add(new Document(currentChunk.toString().trim(), chunkMetadata));
        }

        return chunks;
    }

    /**
     * Create document from text
     */
    public Document createDocument(String content, Map<String, Object> metadata) {
        return new Document(content, metadata);
    }

    /**
     * Create document with generated ID
     */
    public Document createDocument(String id, String content, Map<String, Object> metadata) {
        return new Document(id, content, metadata);
    }
}