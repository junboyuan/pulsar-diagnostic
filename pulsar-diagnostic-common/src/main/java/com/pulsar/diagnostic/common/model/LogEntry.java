package com.pulsar.diagnostic.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a parsed log entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    private String source;

    private String filePath;

    private int lineNumber;

    private LocalDateTime timestamp;

    private String level;

    private String logger;

    private String thread;

    private String message;

    private String exceptionClass;

    private String stackTrace;

    private String rawLine;

    /**
     * Check if this log entry represents an error
     */
    public boolean isError() {
        return "ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level);
    }

    /**
     * Check if this log entry represents a warning
     */
    public boolean isWarning() {
        return "WARN".equalsIgnoreCase(level);
    }
}