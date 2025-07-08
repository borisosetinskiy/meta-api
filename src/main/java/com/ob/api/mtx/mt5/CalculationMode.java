package com.ob.api.mtx.mt5;

public enum CalculationMode {
    Forex(0),
    Futures(1),
    CFD(2),
    CFDIndex(3),
    CFDLeverage(4),
    CalcMode5(5),
    ExchangeStocks(32),
    ExchangeFutures(33),
    FORTSFutures(34),
    ExchangeOption(35),
    ExchangeMarginOption(36),
    ExchangeBounds(37),
    Collateral(64);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private CalculationMode(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, CalculationMode> getMappings() {
        return MappingsHolder.mappings;
    }

    public static CalculationMode forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, CalculationMode> mappings = new java.util.HashMap<Integer, CalculationMode>();
    }
}