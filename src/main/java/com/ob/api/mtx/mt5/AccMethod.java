package com.ob.api.mtx.mt5;

import java.util.Map;

// Position accounting method
public enum AccMethod {
    Default(0),
    Netting(1),
    Hedging(2);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private AccMethod(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static Map<Integer, AccMethod> getMappings() {
        return MappingsHolder.mappings;
    }

    public static AccMethod forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final Map<Integer, AccMethod> mappings = new java.util.HashMap<>();
    }
}