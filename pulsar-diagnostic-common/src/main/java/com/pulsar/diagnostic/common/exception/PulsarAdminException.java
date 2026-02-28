package com.pulsar.diagnostic.common.exception;

/**
 * Exception for Pulsar Admin API related errors
 */
public class PulsarAdminException extends PulsarDiagnosticException {

    public PulsarAdminException(String message) {
        super("PULSAR_ADMIN_ERROR", message, 503);
    }

    public PulsarAdminException(String message, Throwable cause) {
        super("PULSAR_ADMIN_ERROR", message, 503, cause);
    }

    public PulsarAdminException(String errorCode, String message) {
        super(errorCode, message, 503);
    }

    public PulsarAdminException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, 503, cause);
    }
}