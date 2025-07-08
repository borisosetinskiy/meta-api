package com.ob.api.mtx.mt5;

public enum ExecutionType {
    Request(0),
    Instant(1),
    Market(2),
    Exchange(3);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    ExecutionType(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, ExecutionType> getMappings() {
        return MappingsHolder.mappings;
    }

    public static ExecutionType forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, ExecutionType> mappings = new java.util.HashMap<Integer, ExecutionType>();
    }
}