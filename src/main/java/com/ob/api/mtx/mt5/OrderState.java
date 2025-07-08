package com.ob.api.mtx.mt5;

public enum OrderState {
    Started(0),
    Placed(1),
    Cancelled(2),
    Partial(3),
    Filled(4),
    Rejected(5),
    Expired(6),
    RequestAdding(7),
    RequestModifying(8),
    RequestCancelling(9);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private OrderState(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, OrderState> getMappings() {
        return MappingsHolder.mappings;
    }

    public static OrderState forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, OrderState> mappings = new java.util.HashMap<Integer, OrderState>();
    }
}