package com.pulsar.diagnostic.common.enums;

/**
 * Enumeration of diagnostic severity levels
 */
public enum Severity {
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3),
    CRITICAL("critical", 4);

    private final String code;
    private final int level;

    Severity(String code, int level) {
        this.code = code;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public int getLevel() {
        return level;
    }

    public static Severity fromLevel(int level) {
        for (Severity severity : values()) {
            if (severity.level == level) {
                return severity;
            }
        }
        return LOW;
    }
}