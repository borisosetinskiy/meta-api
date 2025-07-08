package com.ob.api.mtx.mt5;

public enum FillPolicy {
    FillOrKill(0),
    ImmediateOrCancel(1),
    FlashFill(2);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private FillPolicy(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, FillPolicy> getMappings() {
        return MappingsHolder.mappings;
    }

    public static FillPolicy forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, FillPolicy> mappings = new java.util.HashMap<Integer, FillPolicy>();
    }
}