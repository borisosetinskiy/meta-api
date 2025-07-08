package com.ob.api.mtx.mt5;

public enum ExpirationType {
    GTC(0),
    Today(1),
    Specified(2),
    SpecifiedDay(3);

    public static final int SIZE = Integer.SIZE;

    private int intValue;

    private ExpirationType(int value) {
        intValue = value;
    }

    public static ExpirationType forValue(int value) {
        for (ExpirationType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return GTC;
    }

    public int getValue() {
        return intValue;
    }
}