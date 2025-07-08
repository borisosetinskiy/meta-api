package com.ob.api.mtx.mt5;

public enum GTCMode //Good till
{
    Cancelled(0),
    TodayIncludeSL_TP(1),
    TodayExcludeSL_TP(2);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private GTCMode(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, GTCMode> getMappings() {
        return MappingsHolder.mappings;
    }

    public static GTCMode forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, GTCMode> mappings = new java.util.HashMap<Integer, GTCMode>();
    }
}