package com.pulsar.diagnostic.common.enums;

/**
 * Enumeration of topic types in Pulsar
 */
public enum TopicType {
    PERSISTENT("persistent", "Persistent topic with durable storage"),
    NON_PERSISTENT("non-persistent", "Non-persistent topic without durable storage"),
    SYSTEM("system", "System internal topic");

    private final String code;
    private final String description;

    TopicType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TopicType fromCode(String code) {
        for (TopicType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return PERSISTENT;
    }
}