package com.pulsar.diagnostic.agent.intent;

/**
 * 意图识别结果
 */
public record IntentResult(
        /**
         * 识别的意图
         */
        String intent,

        /**
         * 置信度 (0.0 - 1.0)
         */
        double confidence,

        /**
         * 分类理由
         */
        String reasoning,

        /**
         * 建议采取的操作
         */
        String suggestedAction,

        /**
         * 指代消解后的明确问题
         */
        String resolvedQuestion,

        /**
         * 用户输入的英文翻译
         */
        String translation
) {
    /**
     * 是否为通用对话（无特定技能匹配）
     */
    public boolean isGeneral() {
        return "general".equals(intent);
    }

    /**
     * 置信度是否足够高
     */
    public boolean isConfident() {
        return confidence >= 0.6;
    }

    /**
     * 是否进行了指代消解
     */
    public boolean hasResolvedQuestion() {
        return resolvedQuestion != null && !resolvedQuestion.isEmpty();
    }

    /**
     * 创建通用意图
     */
    public static IntentResult general() {
        return new IntentResult(
                "general",
                1.0,
                "无特定诊断意图",
                "友好回复并询问需要什么帮助",
                "",
                ""
        );
    }

    /**
     * 创建指定意图（兼容旧版本）
     */
    public static IntentResult of(String intent, double confidence, String reasoning, String suggestedAction) {
        return new IntentResult(intent, confidence, reasoning, suggestedAction, "", "");
    }

    /**
     * 创建指定意图（完整版本）
     */
    public static IntentResult of(String intent, double confidence, String reasoning,
                                  String suggestedAction, String resolvedQuestion, String translation) {
        return new IntentResult(intent, confidence, reasoning, suggestedAction, resolvedQuestion, translation);
    }

    /**
     * 获取最终问题（优先使用指代消解后的问题）
     */
    public String getFinalQuestion() {
        if (hasResolvedQuestion()) {
            return resolvedQuestion;
        }
        return "";
    }
}