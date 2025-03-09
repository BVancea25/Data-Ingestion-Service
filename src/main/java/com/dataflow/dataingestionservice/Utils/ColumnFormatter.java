package com.dataflow.dataingestionservice.Utils;

import com.dataflow.dataingestionservice.Config.TransactionBatchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for converting and formatting column values.
 * <p>
 * This class provides methods to convert date-time strings into {@link LocalDateTime} objects using various formats,
 * as well as converting strings to {@link UUID} objects.
 * It supports a set of predefined date-time formats and allows a user-provided format to be used for parsing.
 * If parsing with the user-provided format fails, it attempts to parse using fallback formatters.
 * </p>
 */
public class ColumnFormatter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);

    /**
     * A map of supported date-time formats keyed by a format identifier.
     * <p>
     * Supported formats include:
     * <ul>
     *   <li>"ISO" : {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} (e.g. 2025-02-11T03:36:00)</li>
     *   <li>"YYYY-MM-DD HH:mm:ss" : Pattern "yyyy-MM-dd HH:mm:ss" (e.g. 2025-02-11 03:36:00)</li>
     *   <li>"MM/DD/YYYY HH:mm:ss" : Pattern "MM/dd/yyyy HH:mm:ss" (e.g. 02/11/2025 03:36:00)</li>
     *   <li>"YYYY/MM/DD HH:mm:ss" : Pattern "yyyy/MM/dd HH:mm:ss" (e.g. 2025/02/11 03:36:00)</li>
     *   <li>"M/d/yyyy HH:mm:ss" : Pattern "M/d/yyyy HH:mm:ss" (e.g. 2/11/2025 03:36:00)</li>
     * </ul>
     * </p>
     */
    private static final Map<String, DateTimeFormatter> FORMATTER_MAP = Map.of(
            "ISO", DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            "YYYY-MM-DD HH:mm:ss", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            "MM/DD/YYYY HH:mm:ss", DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            "YYYY/MM/DD HH:mm:ss", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            "M/d/yyyy HH:mm:ss", DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss")
    );

    /**
     * Array of fallback formatters derived from the values of {@link #FORMATTER_MAP}.
     */
    private static final DateTimeFormatter[] FALLBACK_FORMATTERS = FORMATTER_MAP.values().toArray(new DateTimeFormatter[0]);

    /**
     * Converts a date-time string into a {@link LocalDateTime} using a specified format or fallback formats.
     * <p>
     * If a user-provided format is specified and is present in the supported formats, the method attempts to parse
     * the date-time string using that formatter. If parsing fails or no valid user-provided format is given,
     * the method iterates over the fallback formatters.
     * </p>
     *
     * @param dateTime           the date-time string to be converted; must not be {@code null} or empty
     * @param userProvidedFormat the key for the date-time format to be used (e.g., "ISO", "YYYY-MM-DD HH:mm:ss"),
     *                           or {@code null} to use fallback formatters
     * @return the corresponding {@link LocalDateTime} object
     * @throws IllegalArgumentException if the dateTime string is {@code null} or empty
     * @throws DateTimeParseException   if the date-time string cannot be parsed with any of the supported formats
     */
    public synchronized static LocalDateTime convertToLocalDateTime(String dateTime, String userProvidedFormat) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            throw new IllegalArgumentException("DateTime string is null or empty");
        }
        dateTime = dateTime.trim();

        if (userProvidedFormat != null && FORMATTER_MAP.containsKey(userProvidedFormat)) {
            try {
                return LocalDateTime.parse(dateTime, FORMATTER_MAP.get(userProvidedFormat));
            } catch (DateTimeParseException e) {
                throw new DateTimeParseException("Failed to parse using provided format: " + userProvidedFormat, dateTime, 0);
            }
        }

        for (DateTimeFormatter formatter : FALLBACK_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTime, formatter);
            } catch (DateTimeParseException e) {
                // Continue to try next formatter
            }
        }

        throw new DateTimeParseException("Unable to parse date: " + dateTime, dateTime, 0);
    }

    /**
     * Converts a string representation of a UUID into a {@link UUID} object.
     * <p>
     * The input string is trimmed before conversion.
     * </p>
     *
     * @param uuid the string representation of the UUID; must not be {@code null}
     * @return the corresponding {@link UUID} object
     * @throws IllegalArgumentException if the string is not a valid UUID format
     */
    public synchronized static UUID convertStringToUUID(String uuid) {
        return UUID.fromString(uuid.trim());
    }
}
