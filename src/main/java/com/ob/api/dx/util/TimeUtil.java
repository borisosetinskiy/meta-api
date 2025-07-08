package com.ob.api.dx.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class TimeUtil {

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static long parseSessionTimeOut(String timeString) {
        final LocalTime localTime = LocalTime.parse(timeString);
        final Duration duration = Duration.between(LocalTime.MIN, localTime);
        return duration.toMillis();
    }

    public static String nowAsString() {
        return now().format(formatter);
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now();
    }

    public static long nowEpoch() {
        return now().toEpochSecond() * 1000;
    }

    public static long daysEpoch(int days) {
        return now().minusDays(days).toEpochSecond() * 1000;
    }
}
