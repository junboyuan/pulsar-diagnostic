package com.pulsar.diagnostic.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for log parsing
 */
public final class LogParserUtils {

    private LogParserUtils() {
        // Prevent instantiation
    }

    // Common log patterns
    private static final Pattern LOG4J_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+" +
                    "(\\w+)\\s+\\[([^\\]]+)\\]\\s+(\\S+)\\s+-\\s+(.*)$");

    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "^([\\w.$]+Exception|[\\w.$]+Error)(?::\\s*(.*))?$");

    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
            "^\\s+at\\s+([\\w.$]+\\.([\\w]+))\\.([\\w<>$]+)\\(([^)]+)\\)$");

    /**
     * Parse a log line into structured data
     */
    public static ParsedLogLine parseLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        var matcher = LOG4J_PATTERN.matcher(line);
        if (matcher.matches()) {
            return ParsedLogLine.builder()
                    .timestamp(matcher.group(1))
                    .level(matcher.group(2))
                    .thread(matcher.group(3))
                    .logger(matcher.group(4))
                    .message(matcher.group(5))
                    .rawLine(line)
                    .build();
        }

        // Return raw line if pattern doesn't match
        return ParsedLogLine.builder()
                .rawLine(line)
                .message(line)
                .build();
    }

    /**
     * Check if line is the start of an exception
     */
    public static boolean isExceptionStart(String line) {
        if (line == null) {
            return false;
        }
        var trimmed = line.trim();
        return EXCEPTION_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Check if line is part of stack trace
     */
    public static boolean isStackTraceLine(String line) {
        if (line == null) {
            return false;
        }
        return line.trim().startsWith("at ") ||
                line.trim().startsWith("... ") ||
                line.trim().startsWith("Caused by:");
    }

    /**
     * Parse exception from log lines
     */
    public static ParsedException parseException(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        String firstLine = lines.get(0).trim();
        var matcher = EXCEPTION_PATTERN.matcher(firstLine);

        if (!matcher.matches()) {
            return null;
        }

        String exceptionClass = matcher.group(1);
        String message = matcher.group(2);

        List<StackTraceElement> stackTrace = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (isStackTraceLine(line)) {
                var stackMatcher = STACK_TRACE_PATTERN.matcher(line);
                if (stackMatcher.find()) {
                    stackTrace.add(new StackTraceElement(
                            stackMatcher.group(1),
                            stackMatcher.group(3),
                            stackMatcher.group(4),
                            -1
                    ));
                }
            }
        }

        return ParsedException.builder()
                .exceptionClass(exceptionClass)
                .message(message)
                .stackTrace(stackTrace)
                .build();
    }

    /**
     * Determine log level from line
     */
    public static String determineLevel(String line) {
        if (line == null) {
            return "UNKNOWN";
        }

        String upperLine = line.toUpperCase();
        if (upperLine.contains("FATAL")) {
            return "FATAL";
        } else if (upperLine.contains("ERROR")) {
            return "ERROR";
        } else if (upperLine.contains("WARN")) {
            return "WARN";
        } else if (upperLine.contains("INFO")) {
            return "INFO";
        } else if (upperLine.contains("DEBUG")) {
            return "DEBUG";
        } else if (upperLine.contains("TRACE")) {
            return "TRACE";
        }

        return "UNKNOWN";
    }

    /**
     * Check if log entry contains error indicators
     */
    public static boolean containsError(String line) {
        if (line == null) {
            return false;
        }
        String upperLine = line.toUpperCase();
        return upperLine.contains("ERROR") ||
                upperLine.contains("FATAL") ||
                upperLine.contains("EXCEPTION") ||
                upperLine.contains("FAILED");
    }

    /**
     * Parsed log line
     */
    @lombok.Data
    @lombok.Builder
    public static class ParsedLogLine {
        private String timestamp;
        private String level;
        private String thread;
        private String logger;
        private String message;
        private String rawLine;
    }

    /**
     * Parsed exception
     */
    @lombok.Data
    @lombok.Builder
    public static class ParsedException {
        private String exceptionClass;
        private String message;
        private List<StackTraceElement> stackTrace;
    }
}