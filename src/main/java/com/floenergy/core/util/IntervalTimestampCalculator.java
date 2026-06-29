package com.floenergy.core.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class IntervalTimestampCalculator {


    private static final DateTimeFormatter NEM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public static Timestamp calculateIntervalTimestamp(
            LocalDate intervalDate,
            int intervalNumber,
            int intervalLengthMinutes
    ) {
        LocalDateTime timestamp = intervalDate
                .atStartOfDay()
                .plusMinutes((long) intervalNumber * intervalLengthMinutes);

        return Timestamp.valueOf(timestamp);
    }

    public static LocalDate parseNemDate(String dateValue) {
        return LocalDate.parse(dateValue, NEM_DATE_FORMAT);
    }

}
