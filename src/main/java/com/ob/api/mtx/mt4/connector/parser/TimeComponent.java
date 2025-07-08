package com.ob.api.mtx.mt4.connector.parser;

import java.util.concurrent.atomic.AtomicLong;

import static com.ob.api.mtx.mt4.connector.util.TimeOffsetFile.TIME_OFFSET_FILE;

public final class TimeComponent {
    public final AtomicLong TZ = new AtomicLong(0L);
    public final String name;

    public TimeComponent(String name) {
        this.name = name;
        TZ.getAndSet(TIME_OFFSET_FILE.get(name));
    }

    public long timeNow(long t) {
        final long now = System.currentTimeMillis();
        final long mill = now % 1000;

        return ((t - (TZ.get() * 3_600)) * 1000) + mill;
    }

    public void setServerTimeZone(int timeDst, int timeZone) {
        int tz = timeDst + timeZone;
        if (TZ.get() != tz && validate(timeDst, timeZone)) {
            TZ.getAndSet(tz);
            TIME_OFFSET_FILE.add(name, tz);
        }
    }

    public long timeIn(long tInSec) {
        return ((tInSec - (TZ.get() * 3_600)) * 1000);
    }

    public boolean validate(int timeZone, int timeDst) {
        boolean validTimeDst = timeDst == 0 || timeDst == 1;
        boolean validTimeZone = timeZone >= -12 && timeZone <= 14;

        return validTimeDst && validTimeZone && isDiffBetween(timeDst + timeZone);
    }

    private boolean isDiffBetween(long diff) {
        return diff >= -13 && diff <= 15;
    }

}
