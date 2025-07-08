package com.ob.api.mtx.mt5;

public enum Direction {
    In(0),
    Out(1),
    InOut(2),
    OutBy(3);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private Direction(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, Direction> getMappings() {
        return MappingsHolder.mappings;
    }

    public static Direction forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, Direction> mappings = new java.util.HashMap<Integer, Direction>();
    }
}