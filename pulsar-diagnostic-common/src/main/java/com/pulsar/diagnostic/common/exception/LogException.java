package com.pulsar.diagnostic.common.exception;

/**
 * Exception for log analysis related errors
 */
public class LogException extends PulsarDiagnosticException {

    public LogException(String message) {
        super("LOG_ERROR", message, 500);
    }

    public LogException(String message, Throwable cause) {
        super("LOG_ERROR", message, 500, cause);
    }

    public static LogException fileNotFound(String path) {
        return new LogException("Log file not found: " + path);
    }

    public static LogException readError(String path, Throwable cause) {
        return new LogException("Failed to read log file: " + path, cause);
    }

    public static LogException parseError(String message) {
        return new LogException("Failed to parse log: " + message);
    }
}