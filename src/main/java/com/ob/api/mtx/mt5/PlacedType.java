package com.ob.api.mtx.mt5;

public enum PlacedType {
    //	Manually(0),
//	ByExpert(1),
//	ByDealer(2),
//	OnSL(3),
//	OnTP(4),
//	OnStopOut(5),
//	OnRollover(6),
//	Mobile(16),
//	Web(17);
    Manually(0),         //The deal was executed as a result of activation of an order placed from a desktop terminal
    Mobile(1),         //The deal was executed as a result of activation of an order placed from a mobile application
    Web(2),            //The deal was executed as a result of activation of an order placed from the web platform
    ByExpert(3),         //The deal was executed as a result of activation of an order placed from an MQL5 program, i.e. an Expert Advisor or a script
    OnSL(4),             //The deal was executed as a result of Stop Loss activation
    OnTP(5),             //The deal was executed as a result of Take Profit activation
    OnStopOut(6),             //The deal was executed as a result of the Stop Out event
    OnRollover(7),       //The deal was executed due to a rollover
    OnVmargin(8),        //The deal was executed after charging the variation margin
    OnSplit(18);       //The deal was executed after the split (price reduction) of an instrument, which had an open position during split announcement

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private PlacedType(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, PlacedType> getMappings() {
        return MappingsHolder.mappings;
    }

    public static PlacedType forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, PlacedType> mappings = new java.util.HashMap<Integer, PlacedType>();
    }
}