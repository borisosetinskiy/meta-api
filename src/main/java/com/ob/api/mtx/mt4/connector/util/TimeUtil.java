package com.ob.api.mtx.mt4.connector.util;

public class TimeUtil {
    public static final long TICKS_AT_EPOCH = 621355968000000000L;

    public static long tick() {
        return System.currentTimeMillis() * 10000 + TICKS_AT_EPOCH;
    }

}
