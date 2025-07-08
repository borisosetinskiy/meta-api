package com.ob.api.mtx.mt5;

public enum ThreeDaysSwap {
    vSunday(0),
    vMonday(1),
    vTuesday(2),
    vWednesday(3),
    vThursday(4),
    vFriday(5),
    vSaturday(6);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private ThreeDaysSwap(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, ThreeDaysSwap> getMappings() {
        return MappingsHolder.mappings;
    }

    public static ThreeDaysSwap forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, ThreeDaysSwap> mappings = new java.util.HashMap<Integer, ThreeDaysSwap>();
    }
}