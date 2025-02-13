package com.dataflow.dataingestionservice.Utils;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;


public class ColumnFormatter {
    private static final Map<String, DateTimeFormatter> FORMATTER_MAP = Map.of(
            "ISO", DateTimeFormatter.ISO_LOCAL_DATE_TIME,         // 2025-02-11T03:36:00
            "YYYY-MM-DD HH:mm:ss", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2025-02-11 03:36:00
            "MM/DD/YYYY HH:mm:ss", DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"), // 02/11/2025 03:36:00
            "YYYY/MM/DD HH:mm:ss", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")  // 2025/02/11 03:36:00
    );
    private static final DateTimeFormatter[] FALLBACK_FORMATTERS = FORMATTER_MAP.values().toArray(new DateTimeFormatter[0]);

    public static LocalDateTime convertToLocalDateTime(String dateTime, String userProvidedFormat) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            System.out.println("DateTime string is null or empty");
            throw new IllegalArgumentException("DateTime string is null or empty");
        }
        dateTime = dateTime.trim();

        // If user provides a format, use that directly
        if (userProvidedFormat != null && FORMATTER_MAP.containsKey(userProvidedFormat)) {
            try {
                return LocalDateTime.parse(dateTime, FORMATTER_MAP.get(userProvidedFormat));
            } catch (DateTimeParseException e) {
                System.out.println("Failed to parse using provided format: " + userProvidedFormat+ dateTime);
                throw new DateTimeParseException("Failed to parse using provided format: " + userProvidedFormat, dateTime, 0);
            }
        }

        // If no format is provided, try the fallbacks
        for (DateTimeFormatter formatter : FALLBACK_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTime, formatter);
            } catch (DateTimeParseException e) {
                // Try the next formatter
            }
        }
        System.out.println("Failed to parse using provided format: " + userProvidedFormat+ dateTime);
        throw new DateTimeParseException("Unable to parse date: " + dateTime, dateTime, 0);
    }


    public static UUID convertStringToUUID(String uuid){

        return UUID.fromString(uuid.trim());
    }
}
