package com.pulsar.diagnostic.core.logs;

import com.pulsar.diagnostic.common.exception.LogException;
import com.pulsar.diagnostic.common.model.LogEntry;
import com.pulsar.diagnostic.common.util.DateTimeUtils;
import com.pulsar.diagnostic.common.util.LogParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reader for Pulsar log files
 */
@Component
public class LogFileReader {

    private static final Logger log = LoggerFactory.getLogger(LogFileReader.class);

    /**
     * Read last N lines from a log file
     */
    public List<String> tail(String filePath, int lines) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw LogException.fileNotFound(filePath);
        }

        List<String> result = new ArrayList<>();

        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            result = stream.skip(Math.max(0, countLines(path) - lines))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw LogException.readError(filePath, e);
        }

        return result;
    }

    /**
     * Read entire log file
     */
    public List<String> readAll(String filePath) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw LogException.fileNotFound(filePath);
        }

        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw LogException.readError(filePath, e);
        }
    }

    /**
     * Read and parse log entries from a file
     */
    public List<LogEntry> readParsedLogEntries(String filePath, int maxLines) {
        List<String> lines = tail(filePath, maxLines);
        return parseLogEntries(filePath, lines);
    }

    /**
     * Read log entries with filter
     */
    public List<LogEntry> readLogEntriesWithFilter(String filePath, Predicate<LogEntry> filter, int maxLines) {
        List<LogEntry> entries = readParsedLogEntries(filePath, maxLines);
        return entries.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Search for pattern in log file
     */
    public List<LogEntry> searchInLog(String filePath, String pattern, int maxResults) {
        return readLogEntriesWithFilter(filePath,
                entry -> entry.getMessage() != null && entry.getMessage().contains(pattern),
                maxResults);
    }

    /**
     * Get error entries from log
     */
    public List<LogEntry> getErrorEntries(String filePath, int maxEntries) {
        return readLogEntriesWithFilter(filePath, LogEntry::isError, maxEntries);
    }

    /**
     * Get warning entries from log
     */
    public List<LogEntry> getWarningEntries(String filePath, int maxEntries) {
        return readLogEntriesWithFilter(filePath,
                entry -> entry.isError() || entry.isWarning(),
                maxEntries);
    }

    /**
     * Parse raw log lines into LogEntry objects
     */
    public List<LogEntry> parseLogEntries(String source, List<String> lines) {
        List<LogEntry> entries = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            LogParserUtils.ParsedLogLine parsed = LogParserUtils.parseLine(line);

            if (parsed != null) {
                LocalDateTime timestamp = null;
                if (parsed.getTimestamp() != null) {
                    try {
                        timestamp = DateTimeUtils.parseFlexible(parsed.getTimestamp());
                    } catch (Exception e) {
                        // Keep null timestamp
                    }
                }

                LogEntry entry = LogEntry.builder()
                        .source(source)
                        .filePath(source)
                        .lineNumber(i + 1)
                        .timestamp(timestamp)
                        .level(parsed.getLevel() != null ? parsed.getLevel() :
                                LogParserUtils.determineLevel(line))
                        .logger(parsed.getLogger())
                        .thread(parsed.getThread())
                        .message(parsed.getMessage())
                        .rawLine(line)
                        .build();

                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Count lines in a file
     */
    public long countLines(String filePath) {
        try {
            return countLines(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("Failed to count lines in: {}", filePath, e);
            return 0;
        }
    }

    private long countLines(Path path) throws IOException {
        try (Stream<String> stream = Files.lines(path)) {
            return stream.count();
        }
    }

    /**
     * Check if log file exists
     */
    public boolean logFileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Get log file size in bytes
     */
    public long getLogFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Get log file last modified time
     */
    public LocalDateTime getLastModified(String filePath) {
        try {
            return DateTimeUtils.fromEpochMillis(
                    Files.getLastModifiedTime(Paths.get(filePath)).toMillis());
        } catch (IOException e) {
            return null;
        }
    }
}