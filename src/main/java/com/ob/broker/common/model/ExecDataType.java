package com.ob.broker.common.model;

public enum ExecDataType {
    REQUEST(0),
    INSTANT(1),
    MARKET(2);
    final int value;

    ExecDataType(int value) {
        this.value = value;
    }

    public static ExecDataType fromValue(int value) {
        return switch (value) {
            case 0 -> REQUEST;
            case 1 -> INSTANT;
            case 2 -> MARKET;
            default -> throw new IllegalArgumentException("Unexpected value: " + value);
        };
    }

}