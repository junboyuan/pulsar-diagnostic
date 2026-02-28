package com.pulsar.diagnostic.knowledge.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for generating embeddings for documents
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generate embedding for a single text
     */
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            if (response != null && !response.getResults().isEmpty()) {
                return response.getResults().get(0).getOutput();
            }
            return new float[0];
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            return new float[0];
        }
    }

    /**
     * Generate embeddings for multiple texts
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating embeddings for {} texts", texts.size());

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            if (response != null) {
                return response.getResults().stream()
                        .map(result -> result.getOutput())
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings", e);
            return List.of();
        }
    }

    /**
     * Calculate cosine similarity between two embeddings
     */
    public double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Get embedding dimension
     */
    public int getEmbeddingDimension() {
        // OpenAI text-embedding-ada-002 uses 1536 dimensions
        return 1536;
    }
}