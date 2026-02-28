package com.pulsar.diagnostic.common.enums;

/**
 * Enumeration of component types in Pulsar ecosystem
 */
public enum ComponentType {
    BROKER("broker", "Pulsar Broker"),
    BOOKIE("bookie", "BookKeeper Bookie"),
    ZOOKEEPER("zookeeper", "ZooKeeper Server"),
    PROXY("proxy", "Pulsar Proxy"),
    FUNCTION_WORKER("function-worker", "Pulsar Function Worker"),
    SCHEMA_REGISTRY("schema-registry", "Schema Registry Service");

    private final String code;
    private final String displayName;

    ComponentType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}