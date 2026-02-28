package com.pulsar.diagnostic.common.exception;

/**
 * Base exception for Pulsar Diagnostic system
 */
public class PulsarDiagnosticException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public PulsarDiagnosticException(String message) {
        super(message);
        this.errorCode = "INTERNAL_ERROR";
        this.httpStatus = 500;
    }

    public PulsarDiagnosticException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_ERROR";
        this.httpStatus = 500;
    }

    public PulsarDiagnosticException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }

    public PulsarDiagnosticException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public PulsarDiagnosticException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }

    public PulsarDiagnosticException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}