package com.dataflow.dataingestionservice.Utils;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


public class ColumnFormatter {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static LocalDateTime convertISOtoLocalDateTime(String dateTime){

        return LocalDateTime.parse(dateTime,formatter);
    }

    public static UUID convertStringToUUID(String uuid){

        return UUID.fromString(uuid.trim());
    }
}
