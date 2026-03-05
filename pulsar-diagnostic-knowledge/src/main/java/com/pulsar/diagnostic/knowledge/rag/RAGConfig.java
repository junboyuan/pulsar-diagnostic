package com.pulsar.diagnostic.knowledge.rag;

/**
 * RAG 配置
 *
 * @param retrievalTopK       检索阶段返回的文档数量
 * @param rerankTopK          重排后返回的文档数量
 * @param rerankEnabled       是否启用重排
 * @param queryEnhanceEnabled 是否启用查询增强
 * @param hybridEnabled       是否启用混合检索
 * @param vectorWeight        向量检索权重（混合检索时）
 * @param bm25Weight          BM25 检索权重（混合检索时）
 */
public record RAGConfig(
        int retrievalTopK,
        int rerankTopK,
        boolean rerankEnabled,
        boolean queryEnhanceEnabled,
        boolean hybridEnabled,
        double vectorWeight,
        double bm25Weight
) {
    /**
     * 默认配置
     */
    public static RAGConfig defaultConfig() {
        return new RAGConfig(
                20,     // retrievalTopK
                5,      // rerankTopK
                true,   // rerankEnabled
                true,   // queryEnhanceEnabled
                true,   // hybridEnabled
                0.6,    // vectorWeight
                0.4     // bm25Weight
        );
    }

    /**
     * 快速配置（禁用重排和查询增强，用于简单查询）
     */
    public static RAGConfig fast() {
        return new RAGConfig(
                10,     // retrievalTopK
                5,      // rerankTopK
                false,  // rerankEnabled
                false,  // queryEnhanceEnabled
                true,   // hybridEnabled
                0.6,    // vectorWeight
                0.4     // bm25Weight
        );
    }

    /**
     * 精确配置（启用所有功能，返回更少但更精确的结果）
     */
    public static RAGConfig precise() {
        return new RAGConfig(
                30,     // retrievalTopK
                3,      // rerankTopK
                true,   // rerankEnabled
                true,   // queryEnhanceEnabled
                true,   // hybridEnabled
                0.5,    // vectorWeight
                0.5     // bm25Weight
        );
    }

    /**
     * 仅向量检索配置
     */
    public static RAGConfig vectorOnly() {
        return new RAGConfig(
                10,     // retrievalTopK
                5,      // rerankTopK
                false,  // rerankEnabled
                false,  // queryEnhanceEnabled
                false,  // hybridEnabled
                1.0,    // vectorWeight
                0.0     // bm25Weight
        );
    }

    /**
     * 创建自定义配置
     */
    public static RAGConfig of(int retrievalTopK, int rerankTopK) {
        return new RAGConfig(
                retrievalTopK, rerankTopK,
                true, true, true,
                0.6, 0.4
        );
    }

    /**
     * 复制并修改检索数量
     */
    public RAGConfig withTopK(int retrievalTopK, int rerankTopK) {
        return new RAGConfig(
                retrievalTopK, rerankTopK,
                this.rerankEnabled, this.queryEnhanceEnabled, this.hybridEnabled,
                this.vectorWeight, this.bm25Weight
        );
    }

    /**
     * 复制并启用/禁用重排
     */
    public RAGConfig withRerank(boolean enabled) {
        return new RAGConfig(
                this.retrievalTopK, this.rerankTopK,
                enabled, this.queryEnhanceEnabled, this.hybridEnabled,
                this.vectorWeight, this.bm25Weight
        );
    }
}