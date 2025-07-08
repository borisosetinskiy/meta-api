package com.ob.api.mtx.mt5;

import lombok.Getter;

@Getter
public enum V3DaysSwap {
    Sunday(0),
    Monday(1),
    Tuesday(2),
    Wednesday(3),
    Thursday(4),
    Friday(5),
    Saturday(6);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private V3DaysSwap(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, V3DaysSwap> getMappings() {
        return MappingsHolder.mappings;
    }

    public static V3DaysSwap forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, V3DaysSwap> mappings = new java.util.HashMap<Integer, V3DaysSwap>();
    }
}