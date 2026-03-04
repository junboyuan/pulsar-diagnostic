package com.pulsar.diagnostic.agent.tool;

import com.pulsar.diagnostic.agent.mcp.McpClient;
import com.pulsar.diagnostic.common.model.LogEntry;
import com.pulsar.diagnostic.core.logs.LogAnalysisService;
import com.pulsar.diagnostic.core.logs.LogFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for analyzing Pulsar logs.
 * Uses MCP server for log analysis.
 */
@Component
public class LogAnalysisTool {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisTool.class);

    private final McpClient mcpClient;
    private final LogAnalysisService logAnalysisService;
    private final LogFileReader logFileReader;

    public LogAnalysisTool(McpClient mcpClient,
                           LogAnalysisService logAnalysisService,
                           LogFileReader logFileReader) {
        this.mcpClient = mcpClient;
        this.logAnalysisService = logAnalysisService;
        this.logFileReader = logFileReader;
    }

    /**
     * Analyze broker logs for errors and warnings
     * @param maxLines Maximum number of lines to analyze
     */
    @Tool(description = "Analyze Pulsar broker logs for errors and patterns. Input: max number of lines (optional, default 500)")
    public String analyzeBrokerLogs(@ToolParam(description = "Maximum number of lines to analyze, default 500", required = false) Integer maxLines) {
        log.info("Tool: Analyzing broker logs via MCP");
        int lines = maxLines != null ? maxLines : 1000;

        try {
            return mcpClient.callToolSync("analyze_logs",
                    Map.of("component", "broker",
                           "max_lines", lines));
        } catch (Exception e) {
            log.error("Failed to analyze broker logs via MCP, falling back", e);
            try {
                LogAnalysisService.LogAnalysisResult result =
                        logAnalysisService.analyzeBrokerLogs(lines);

                return formatAnalysisResult(result, "Broker");
            } catch (Exception ex) {
                return "Error analyzing broker logs: " + ex.getMessage();
            }
        }
    }

    /**
     * Analyze bookie (BookKeeper) logs for errors and warnings
     * @param maxLines Maximum number of lines to analyze
     */
    @Tool(description = "Analyze BookKeeper bookie logs for errors and patterns. Input: max number of lines (optional, default 500)")
    public String analyzeBookieLogs(@ToolParam(description = "Maximum number of lines to analyze, default 500", required = false) Integer maxLines) {
        log.info("Tool: Analyzing bookie logs via MCP");
        int lines = maxLines != null ? maxLines : 1000;

        try {
            return mcpClient.callToolSync("analyze_logs",
                    Map.of("component", "bookie",
                           "max_lines", lines));
        } catch (Exception e) {
            log.error("Failed to analyze bookie logs via MCP, falling back", e);
            try {
                LogAnalysisService.LogAnalysisResult result =
                        logAnalysisService.analyzeBookieLogs(lines);

                return formatAnalysisResult(result, "Bookie");
            } catch (Exception ex) {
                return "Error analyzing bookie logs: " + ex.getMessage();
            }
        }
    }

    /**
     * Search for a specific pattern in all Pulsar logs
     * @param pattern Search pattern or keyword
     * @param maxResults Maximum results per log file
     */
    @Tool(description = "Search for a specific pattern in all Pulsar logs. Input: search pattern")
    public String searchAllLogs(@ToolParam(description = "Search pattern or keyword") String pattern,
                                 @ToolParam(description = "Maximum results per log file, default 100", required = false) Integer maxResults) {
        log.info("Tool: Searching logs for: {}", pattern);
        int max = maxResults != null ? maxResults : 100;

        try {
            Map<String, List<LogEntry>> results =
                    logAnalysisService.searchAllLogs(pattern, max);

            if (results.isEmpty()) {
                return "No matches found for pattern: " + pattern;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== Search Results for '%s' ===\n\n", pattern));

            for (Map.Entry<String, List<LogEntry>> entry : results.entrySet()) {
                sb.append(String.format("Component: %s (%d matches)\n",
                        entry.getKey(), entry.getValue().size()));

                for (LogEntry logEntry : entry.getValue()) {
                    sb.append(String.format("  [%s] %s: %s\n",
                            logEntry.getLevel(),
                            logEntry.getTimestamp() != null ? logEntry.getTimestamp() : "N/A",
                            truncateMessage(logEntry.getMessage(), 100)));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error searching logs: " + e.getMessage();
        }
    }

    /**
     * Get recent errors from all Pulsar components
     * @param maxErrors Maximum errors to retrieve per component
     */
    @Tool(description = "Get recent error messages from all logs. Input: max number of errors to return (optional, default 50)")
    public String getRecentErrors(@ToolParam(description = "Maximum errors to retrieve per component, default 50", required = false) Integer maxErrors) {
        log.info("Tool: Getting recent errors");
        int max = maxErrors != null ? maxErrors : 50;

        try {
            Map<String, List<LogEntry>> errors =
                    logAnalysisService.getRecentErrors(max);

            if (errors.isEmpty()) {
                return "No recent errors found in any component";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== Recent Errors Across All Components ===\n\n");

            int totalErrors = errors.values().stream()
                    .mapToInt(List::size)
                    .sum();

            sb.append(String.format("Total errors found: %d\n\n", totalErrors));

            for (Map.Entry<String, List<LogEntry>> entry : errors.entrySet()) {
                sb.append(String.format("Component: %s (%d errors)\n",
                        entry.getKey(), entry.getValue().size()));

                for (LogEntry error : entry.getValue()) {
                    sb.append(String.format("  [%s] ", error.getTimestamp() != null ?
                            error.getTimestamp().toString() : "N/A"));
                    sb.append(truncateMessage(error.getMessage(), 150));
                    sb.append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error getting recent errors: " + e.getMessage();
        }
    }

    /**
     * Read the last N lines from a specific log file
     * @param filePath Path to the log file
     * @param lines Number of lines to read
     */
    @Tool(description = "Read the last N lines from a specific log file. Input: file path and number of lines")
    public String tailLogFile(@ToolParam(description = "Path to the log file") String filePath,
                               @ToolParam(description = "Number of lines to read, default 100", required = false) Integer lines) {
        log.info("Tool: Tailing log file: {}", filePath);
        int lineCount = lines != null ? lines : 100;

        try {
            List<String> logLines = logFileReader.tail(filePath, lineCount);

            if (logLines.isEmpty()) {
                return "Log file is empty or not found: " + filePath;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== Last %d lines from %s ===\n\n",
                    logLines.size(), filePath));

            for (String line : logLines) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error reading log file: " + e.getMessage();
        }
    }

    private String formatAnalysisResult(LogAnalysisService.LogAnalysisResult result, String component) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("=== %s Log Analysis ===\n\n", component));

        if (!result.isAvailable()) {
            sb.append("Log not available: ").append(result.getErrorMessage());
            return sb.toString();
        }

        sb.append(String.format("Log Path: %s\n", result.getLogPath()));
        sb.append(String.format("Errors Found: %d\n", result.getErrorCount()));
        sb.append(String.format("Warnings Found: %d\n", result.getWarningCount()));

        // Show detected patterns
        if (!result.getDetectedPatterns().isEmpty()) {
            sb.append("\n=== Detected Patterns ===\n");
            for (LogAnalysisService.DetectedPattern pattern : result.getDetectedPatterns()) {
                sb.append(String.format("- [%s] %s: %d occurrences\n",
                        pattern.getCategory(),
                        pattern.getDescription(),
                        pattern.getCount()));
            }
        }

        // Show sample errors
        if (!result.getErrorEntries().isEmpty()) {
            sb.append("\n=== Sample Errors ===\n");
            int count = 0;
            for (LogEntry error : result.getErrorEntries()) {
                if (count++ >= 5) break; // Show max 5 errors
                sb.append(String.format("- [%s] %s\n",
                        error.getLevel(),
                        truncateMessage(error.getMessage(), 200)));
            }
        }

        return sb.toString();
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}