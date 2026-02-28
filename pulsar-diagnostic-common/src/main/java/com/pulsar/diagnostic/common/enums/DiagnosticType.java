package com.pulsar.diagnostic.common.enums;

/**
 * Enumeration of diagnostic issue types
 */
public enum DiagnosticType {
    PERFORMANCE("performance", "Performance Issue"),
    CONNECTIVITY("connectivity", "Connectivity Issue"),
    STORAGE("storage", "Storage Issue"),
    CONFIGURATION("configuration", "Configuration Issue"),
    RESOURCE("resource", "Resource Issue"),
    MESSAGE("message", "Message Processing Issue"),
    SECURITY("security", "Security Issue"),
    OTHER("other", "Other Issue");

    private final String code;
    private final String description;

    DiagnosticType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}