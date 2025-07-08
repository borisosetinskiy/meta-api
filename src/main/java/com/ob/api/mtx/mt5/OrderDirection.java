package com.ob.api.mtx.mt5;

public enum OrderDirection {
    In(0),
    Out(1),
    InOut(2);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private OrderDirection(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, OrderDirection> getMappings() {
        return MappingsHolder.mappings;
    }

    public static OrderDirection forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, OrderDirection> mappings = new java.util.HashMap<Integer, OrderDirection>();
    }
}