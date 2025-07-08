package com.ob.api.mtx.mt5;

public enum MarginMode {
    MarginForex(0),
    MarginFutures(1),
    vMarginCFD(2),
    MarginCFDIndex(3);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private MarginMode(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, MarginMode> getMappings() {
        return MappingsHolder.mappings;
    }

    public static MarginMode forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, MarginMode> mappings = new java.util.HashMap<Integer, MarginMode>();
    }
}