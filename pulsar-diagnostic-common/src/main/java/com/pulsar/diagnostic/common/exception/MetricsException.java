package com.pulsar.diagnostic.common.exception;

/**
 * Exception for metrics collection related errors
 */
public class MetricsException extends PulsarDiagnosticException {

    public MetricsException(String message) {
        super("METRICS_ERROR", message, 503);
    }

    public MetricsException(String message, Throwable cause) {
        super("METRICS_ERROR", message, 503, cause);
    }

    public static MetricsException connectionFailed(String url, Throwable cause) {
        return new MetricsException("Failed to connect to metrics endpoint: " + url, cause);
    }

    public static MetricsException invalidMetric(String metricName) {
        return new MetricsException("Invalid or unavailable metric: " + metricName);
    }
}