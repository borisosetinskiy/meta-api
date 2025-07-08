package com.ob.api.mtx.mt5;

public enum TradeMode {
    Disabled(0),
    LongOnly(1),
    ShortOnly(2),
    CloseOnly(3),
    FullAccess(4);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private TradeMode(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, TradeMode> getMappings() {
        return MappingsHolder.mappings;
    }

    public static TradeMode forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, TradeMode> mappings = new java.util.HashMap<Integer, TradeMode>();
    }
}