package com.pulsar.diagnostic.core.logs;

import com.pulsar.diagnostic.common.model.LogEntry;
import com.pulsar.diagnostic.core.config.PulsarConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing Pulsar logs
 */
@Component
public class LogAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisService.class);

    private final LogFileReader logFileReader;
    private final PulsarConfig pulsarConfig;

    // Error patterns to detect
    private static final List<ErrorPattern> ERROR_PATTERNS = Arrays.asList(
            new ErrorPattern("Connection refused", "CONNECTIVITY", "Connection failure"),
            new ErrorPattern("Timeout", "PERFORMANCE", "Operation timeout"),
            new ErrorPattern("OutOfMemoryError", "RESOURCE", "Memory exhausted"),
            new ErrorPattern("No space left", "RESOURCE", "Disk space issue"),
            new ErrorPattern("BookieException", "STORAGE", "BookKeeper error"),
            new ErrorPattern("MetadataException", "STORAGE", "Metadata error"),
            new ErrorPattern("NotLeaderException", "CLUSTER", "Leader election issue"),
            new ErrorPattern("ProducerBusy", "TOPIC", "Producer conflict"),
            new ErrorPattern("ConsumerBusy", "TOPIC", "Consumer conflict"),
            new ErrorPattern("TopicNotFound", "TOPIC", "Topic not found"),
            new ErrorPattern("NamespaceNotFound", "CONFIG", "Namespace not found"),
            new ErrorPattern("AuthenticationException", "SECURITY", "Authentication failure"),
            new ErrorPattern("AuthorizationException", "SECURITY", "Authorization failure")
    );

    public LogAnalysisService(LogFileReader logFileReader, PulsarConfig pulsarConfig) {
        this.logFileReader = logFileReader;
        this.pulsarConfig = pulsarConfig;
    }

    /**
     * Analyze broker logs
     */
    public LogAnalysisResult analyzeBrokerLogs(int maxLines) {
        String logPath = pulsarConfig.getLogPaths().getBroker();
        return analyzeLogs(logPath, "broker", maxLines);
    }

    /**
     * Analyze bookie logs
     */
    public LogAnalysisResult analyzeBookieLogs(int maxLines) {
        String logPath = pulsarConfig.getLogPaths().getBookie();
        return analyzeLogs(logPath, "bookie", maxLines);
    }

    /**
     * Analyze zookeeper logs
     */
    public LogAnalysisResult analyzeZookeeperLogs(int maxLines) {
        String logPath = pulsarConfig.getLogPaths().getZookeeper();
        return analyzeLogs(logPath, "zookeeper", maxLines);
    }

    /**
     * Analyze logs from a specific path
     */
    public LogAnalysisResult analyzeLogs(String logPath, String component, int maxLines) {
        LogAnalysisResult result = new LogAnalysisResult();
        result.setComponent(component);
        result.setLogPath(logPath);

        if (!logFileReader.logFileExists(logPath)) {
            result.setAvailable(false);
            result.setErrorMessage("Log path not found: " + logPath);
            return result;
        }

        result.setAvailable(true);

        try {
            // Get error entries
            List<LogEntry> errorEntries = logFileReader.getErrorEntries(logPath, maxLines);
            result.setErrorEntries(errorEntries);
            result.setErrorCount(errorEntries.size());

            // Get warning entries
            List<LogEntry> warningEntries = logFileReader.getWarningEntries(logPath, maxLines);
            result.setWarningEntries(warningEntries.stream()
                    .filter(e -> !e.isError())
                    .collect(Collectors.toList()));
            result.setWarningCount(result.getWarningEntries().size());

            // Analyze patterns
            result.setDetectedPatterns(detectPatterns(errorEntries));

            // Get file info
            result.setFileSize(logFileReader.getLogFileSize(logPath));
            result.setLastModified(logFileReader.getLastModified(logPath));

        } catch (Exception e) {
            log.error("Failed to analyze logs for: {}", logPath, e);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Search for a pattern in all logs
     */
    public Map<String, List<LogEntry>> searchAllLogs(String pattern, int maxResultsPerLog) {
        Map<String, List<LogEntry>> results = new HashMap<>();

        for (Map.Entry<String, String> entry : pulsarConfig.getAllLogPaths().entrySet()) {
            String component = entry.getKey();
            String path = entry.getValue();

            if (logFileReader.logFileExists(path)) {
                try {
                    List<LogEntry> matches = logFileReader.searchInLog(path, pattern, maxResultsPerLog);
                    if (!matches.isEmpty()) {
                        results.put(component, matches);
                    }
                } catch (Exception e) {
                    log.warn("Failed to search logs for {}: {}", component, e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Get recent errors from all components
     */
    public Map<String, List<LogEntry>> getRecentErrors(int maxEntriesPerLog) {
        Map<String, List<LogEntry>> results = new HashMap<>();

        for (Map.Entry<String, String> entry : pulsarConfig.getAllLogPaths().entrySet()) {
            String component = entry.getKey();
            String path = entry.getValue();

            if (logFileReader.logFileExists(path)) {
                try {
                    List<LogEntry> errors = logFileReader.getErrorEntries(path, maxEntriesPerLog);
                    if (!errors.isEmpty()) {
                        results.put(component, errors);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get errors for {}: {}", component, e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Detect known error patterns in log entries
     */
    private List<DetectedPattern> detectPatterns(List<LogEntry> entries) {
        List<DetectedPattern> detected = new ArrayList<>();
        Map<String, Integer> patternCounts = new HashMap<>();

        for (LogEntry entry : entries) {
            String message = entry.getMessage();
            if (message == null) {
                message = entry.getRawLine();
            }

            for (ErrorPattern pattern : ERROR_PATTERNS) {
                if (message != null && message.contains(pattern.pattern)) {
                    String key = pattern.category + ":" + pattern.description;
                    patternCounts.merge(key, 1, Integer::sum);
                }
            }
        }

        // Convert to detected patterns
        for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
            String[] parts = entry.getKey().split(":");
            detected.add(new DetectedPattern(
                    parts[0],
                    parts[1],
                    entry.getValue()
            ));
        }

        // Sort by count
        detected.sort(Comparator.comparingInt(DetectedPattern::getCount).reversed());

        return detected;
    }

    // Inner classes

    @lombok.Data
    public static class LogAnalysisResult {
        private String component;
        private String logPath;
        private boolean available;
        private String errorMessage;
        private int errorCount;
        private int warningCount;
        private List<LogEntry> errorEntries = new ArrayList<>();
        private List<LogEntry> warningEntries = new ArrayList<>();
        private List<DetectedPattern> detectedPatterns = new ArrayList<>();
        private long fileSize;
        private java.time.LocalDateTime lastModified;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DetectedPattern {
        private String category;
        private String description;
        private int count;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorPattern {
        private String pattern;
        private String category;
        private String description;
    }
}