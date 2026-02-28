package com.pulsar.diagnostic.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date and time utility class
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // Prevent instantiation
    }

    // Common date-time formats
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter READABLE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    /**
     * Get current LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Format LocalDateTime to ISO string
     */
    public static String formatIso(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Format LocalDateTime to readable string
     */
    public static String formatReadable(LocalDateTime dateTime) {
        return dateTime.format(READABLE_FORMATTER);
    }

    /**
     * Format LocalDateTime for log output
     */
    public static String formatForLog(LocalDateTime dateTime) {
        return dateTime.format(LOG_FORMATTER);
    }

    /**
     * Parse ISO string to LocalDateTime
     */
    public static LocalDateTime parseIso(String isoString) {
        try {
            return LocalDateTime.parse(isoString, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try with offset
            return OffsetDateTime.parse(isoString).toLocalDateTime();
        }
    }

    /**
     * Parse string with multiple format support
     */
    public static LocalDateTime parseFlexible(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }

        // Try ISO format first
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (DateTimeParseException ignored) {
        }

        // Try readable format
        try {
            return LocalDateTime.parse(dateTimeStr, READABLE_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        // Try log format
        try {
            return LocalDateTime.parse(dateTimeStr, LOG_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException("Unable to parse date time: " + dateTimeStr);
    }

    /**
     * Convert epoch millis to LocalDateTime
     */
    public static LocalDateTime fromEpochMillis(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    /**
     * Convert LocalDateTime to epoch millis
     */
    public static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Calculate duration between two times in human readable format
     */
    public static String durationBetween(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        return formatDuration(duration);
    }

    /**
     * Format duration to human readable string
     */
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        } else {
            return (seconds / 86400) + "d " + ((seconds % 86400) / 3600) + "h";
        }
    }

    /**
     * Get current timestamp as epoch millis
     */
    public static long currentEpochMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Check if a timestamp is recent (within specified minutes)
     */
    public static boolean isRecent(LocalDateTime timestamp, int minutesThreshold) {
        return timestamp != null &&
                Duration.between(timestamp, now()).toMinutes() <= minutesThreshold;
    }
}