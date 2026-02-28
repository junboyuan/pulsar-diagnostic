package com.pulsar.diagnostic.common.model;

import com.pulsar.diagnostic.common.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a BookKeeper Bookie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookieInfo {

    private String bookieId;

    private String address;

    private int port;

    private HealthStatus healthStatus;

    private boolean isReadOnly;

    private long diskUsage;

    private long diskCapacity;

    private double diskUsagePercent;

    private long ledgerCount;

    private long entryCount;

    private long totalSize;

    private LocalDateTime lastUpdated;

    public double getDiskUsagePercent() {
        if (diskCapacity > 0) {
            return (double) diskUsage / diskCapacity * 100;
        }
        return 0;
    }
}