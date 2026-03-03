package com.pulsar.diagnostic.web.dto;

import com.pulsar.diagnostic.agent.dto.QAResponse;

/**
 * Response DTO for chat endpoint
 */
public record ChatResponse(
        /**
         * 响应内容（兼容旧格式）
         */
        String response,

        /**
         * 处理时间（毫秒）
         */
        long processingTimeMs,

        /**
         * 知识库是否有用（可选，仅知识问答模式）
         */
        Boolean useful,

        /**
         * 英文翻译（可选，仅知识问答模式）
         */
        String translation
) {
    /**
     * 创建标准聊天响应
     */
    public static ChatResponse of(String response, long processingTimeMs) {
        return new ChatResponse(response, processingTimeMs, null, null);
    }

    /**
     * 创建知识问答响应
     */
    public static ChatResponse fromQA(QAResponse qaResponse, long processingTimeMs) {
        return new ChatResponse(
                qaResponse.content(),
                processingTimeMs,
                qaResponse.useful(),
                qaResponse.translation()
        );
    }

    /**
     * 创建带意图信息的响应
     */
    public static ChatResponse withIntent(String response, long processingTimeMs,
                                          String intent, double confidence) {
        return new ChatResponse(
                formatWithIntent(response, intent, confidence),
                processingTimeMs,
                null,
                null
        );
    }

    private static String formatWithIntent(String response, String intent, double confidence) {
        return String.format("## 诊断结果\n\n**识别意图**: %s\n**置信度**: %.0f%%\n\n---\n\n%s",
                getIntentDisplayName(intent), confidence * 100, response);
    }

    private static String getIntentDisplayName(String intent) {
        return switch (intent) {
            case "backlog-diagnosis" -> "消息积压诊断";
            case "cluster-health-check" -> "集群健康检查";
            case "performance-analysis" -> "性能分析";
            case "connectivity-troubleshoot" -> "连接故障排查";
            case "capacity-planning" -> "容量规划";
            case "topic-consultation" -> "主题咨询";
            default -> intent;
        };
    }
}