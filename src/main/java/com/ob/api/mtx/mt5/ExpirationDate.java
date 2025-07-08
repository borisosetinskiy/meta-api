package com.ob.api.mtx.mt5;

public enum ExpirationDate {
    GTC(0),
    Today(1),
    Specified(2),
    SpecifiedDay(3);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private ExpirationDate(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, ExpirationDate> getMappings() {
        return MappingsHolder.mappings;
    }

    public static ExpirationDate forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, ExpirationDate> mappings = new java.util.HashMap<Integer, ExpirationDate>();
    }
}