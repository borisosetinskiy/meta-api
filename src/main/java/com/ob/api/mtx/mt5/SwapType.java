package com.ob.api.mtx.mt5;

public enum SwapType {
    SwapNone(0),
    InPoints(1),
    SymInfo_s408(2), //???
    MarginCurrency(3),
    Currency(4),
    PercCurPrice(5), //In percentage terms, using current price
    PercOpenPrice(6), //In percentage terms, using open price
    PointClosePrice(7), //In points, reopen position by close price
    PointBidPrice(8); //In points, reopen position by bid price

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private SwapType(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, SwapType> getMappings() {
        return MappingsHolder.mappings;
    }

    public static SwapType forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, SwapType> mappings = new java.util.HashMap<Integer, SwapType>();
    }
}