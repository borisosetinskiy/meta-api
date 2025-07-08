package com.ob.broker.service;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReconnectConfig {
    static final LocalTime OPEN_CLOSE_TIME = LocalTime.of(17, 0, 0);
    static final int MAX_RECONNECTION_ATTEMPT = 7;
    static final int MAX_RECONNECTION_ON_WEEKEND_ATTEMPT = 60;
    static final long DELAY = 15_000;
    static final long DELAY_ON_CLOSE = 60_000;

    boolean disabled = false;
    int maxReconnectionAttempt = MAX_RECONNECTION_ATTEMPT;
    int maxReconnectionOnWeekendAttempt = MAX_RECONNECTION_ON_WEEKEND_ATTEMPT;

    static boolean isFridayClose(LocalDateTime nowDate, LocalTime nowTime) {
        return nowDate.getDayOfWeek() == DayOfWeek.FRIDAY && nowTime.isAfter(OPEN_CLOSE_TIME);
    }

    static boolean isSundayClose(LocalDateTime nowDate, LocalTime nowTime) {
        return nowDate.getDayOfWeek() == DayOfWeek.SUNDAY && nowTime.isBefore(OPEN_CLOSE_TIME);
    }

    static long calculateDelay() {
        LocalDateTime nowDate = LocalDateTime.now();
        LocalTime nowTime = nowDate.toLocalTime();
        if (isFridayClose(nowDate, nowTime)
            || nowDate.getDayOfWeek() == DayOfWeek.SATURDAY
            || isSundayClose(nowDate, nowTime)) {
            return DELAY_ON_CLOSE;
        }
        return DELAY;
    }

    int attempt() {
        LocalDateTime nowDate = LocalDateTime.now();
        LocalTime nowTime = nowDate.toLocalTime();
        if (isFridayClose(nowDate, nowTime)
            || nowDate.getDayOfWeek() == DayOfWeek.SATURDAY
            || isSundayClose(nowDate, nowTime)) {
            return maxReconnectionOnWeekendAttempt <= 0 ? Integer.MAX_VALUE : maxReconnectionOnWeekendAttempt;
        }
        return maxReconnectionAttempt <= 0 ? Integer.MAX_VALUE : maxReconnectionAttempt;
    }
}
