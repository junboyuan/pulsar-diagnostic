package com.pulsar.diagnostic.agent.intent;

/**
 * 意图识别结果
 */
public record IntentResult(
        String intent,
        double confidence,
        String reasoning,
        String suggestedAction
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
     * 创建通用意图
     */
    public static IntentResult general() {
        return new IntentResult("general", 1.0, "无特定诊断意图", "友好回复并询问需要什么帮助");
    }

    /**
     * 创建指定意图
     */
    public static IntentResult of(String intent, double confidence, String reasoning, String suggestedAction) {
        return new IntentResult(intent, confidence, reasoning, suggestedAction);
    }
}