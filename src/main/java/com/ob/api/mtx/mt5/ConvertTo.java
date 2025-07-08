package com.ob.api.mtx.mt5;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ConvertTo {
    private static LocalDateTime StartTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);
    private static double[] DegreeP = {1.0, 1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5, 1.0e6, 1.0e7, 1.0e8, 1.0e9, 1.0e10, 1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15};

    public static LocalDateTime DateTime(long time) {
        return StartTime.plusSeconds(time);
    }

    public static LocalDateTime DateTimeMs(long time) {
        return StartTime.plusNanos(time * 1000000);
    }

    public static LocalDateTime toLocal(LocalDateTime dateTime, ZoneId zoneId) {
        ZonedDateTime zdt = dateTime.atZone(zoneId);
        return LocalDateTime.ofInstant(zdt.toInstant(), ZoneId.systemDefault());
    }

    public static long Long(LocalDateTime time) {
        return Duration.between(StartTime, time).getSeconds();
    }

    public static double LongLongToDouble(int digits, long value) {
        digits = Math.min(digits, 11);
        return Math.round(((double) (value) / DegreeP[digits]) * Math.pow(10, digits)) / Math.pow(10, digits);
    }

}