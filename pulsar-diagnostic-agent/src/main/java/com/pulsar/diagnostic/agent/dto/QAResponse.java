package com.pulsar.diagnostic.agent.dto;

/**
 * 知识问答响应
 *
 * 用于结构化返回知识库问答结果
 */
public record QAResponse(
        /**
         * 知识库内容是否有助于回答问题
         */
        boolean useful,

        /**
         * 中文回复内容
         */
        String content,

        /**
         * 英文翻译
         */
        String translation
) {
    /**
     * 创建有用的回答
     */
    public static QAResponse useful(String content, String translation) {
        return new QAResponse(true, content, translation);
    }

    /**
     * 创建无法回答的响应
     */
    public static QAResponse notUseful(String content, String translation) {
        return new QAResponse(false, content, translation);
    }

    /**
     * 创建默认的无法回答响应
     */
    public static QAResponse unknown() {
        return new QAResponse(
                false,
                "根据现有知识库无法回答此问题，建议查阅Pulsar官方文档或提供更多上下文。",
                "Unable to answer this question based on the current knowledge base. Please refer to the Pulsar official documentation or provide more context."
        );
    }
}