package com.pulsar.diagnostic.common.exception;

/**
 * Exception for AI Agent related errors
 */
public class AgentException extends PulsarDiagnosticException {

    public AgentException(String message) {
        super("AGENT_ERROR", message, 500);
    }

    public AgentException(String message, Throwable cause) {
        super("AGENT_ERROR", message, 500, cause);
    }

    public AgentException(String errorCode, String message) {
        super(errorCode, message, 500);
    }

    public AgentException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public AgentException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, 500, cause);
    }

    public static AgentException toolExecutionFailed(String toolName, Throwable cause) {
        return new AgentException("TOOL_EXECUTION_FAILED",
                "Failed to execute tool: " + toolName, cause);
    }

    public static AgentException knowledgeBaseError(String message) {
        return new AgentException("KNOWLEDGE_BASE_ERROR", message);
    }

    public static AgentException invalidInput(String message) {
        return new AgentException("INVALID_INPUT", message, 400);
    }
}