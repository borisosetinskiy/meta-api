package com.ob.api.mtx.mt5;

/**
 * Deal type
 */
public enum DealType {
    DealBuy(0),
    DealSell(1),
    Balance(2),
    Credit(3),
    Charge(4),
    Correction(5),
    Bonus(6),
    Commission(7),
    DailyCommission(8),
    MonthlyCommission(9),
    DailyAgentCommission(10),
    MonthlyAgentCommission(11),
    InterestRate(12),
    CanceledBuy(13),
    CanceledSell(14),
    Dividend(15),
    Tax(17);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private DealType(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, DealType> getMappings() {
        return MappingsHolder.mappings;
    }

    public static DealType forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, DealType> mappings = new java.util.HashMap<Integer, DealType>();
    }
}