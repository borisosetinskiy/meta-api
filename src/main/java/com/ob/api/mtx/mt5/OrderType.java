package com.ob.api.mtx.mt5;

import com.ob.broker.common.model.OrderTypeData;
import lombok.Getter;

@Getter
public enum OrderType {
    Buy(0),
    Sell(1),
    BuyLimit(2),
    SellLimit(3),
    BuyStop(4),
    SellStop(5),
    BuyStopLimit(6),
    SellStopLimit(7),
    CloseBy(8);

    public static final int SIZE = Integer.SIZE;
    private final int intValue;

    private OrderType(int value) {
        intValue = value;
        getMappings().put(value, this);
    }

    private static java.util.HashMap<Integer, OrderType> getMappings() {
        return MappingsHolder.mappings;
    }

    public static OrderType forValue(int value) {
        return getMappings().get(value);
    }

    public int getValue() {
        return intValue;
    }

    public OrderTypeData toOrderTypeData() {
        return switch (this) {
            case Buy -> OrderTypeData.Buy;
            case Sell -> OrderTypeData.Sell;
            case BuyLimit -> OrderTypeData.BuyLimit;
            case SellLimit -> OrderTypeData.SellLimit;
            case BuyStop -> OrderTypeData.BuyStop;
            case SellStop -> OrderTypeData.SellStop;
            case BuyStopLimit -> OrderTypeData.BuyStopLimit;
            case SellStopLimit -> OrderTypeData.SellStopLimit;
            case CloseBy -> OrderTypeData.CloseBy;
        };
    }

    private static final class MappingsHolder {
        private static final java.util.HashMap<Integer, OrderType> mappings = new java.util.HashMap<Integer, OrderType>();
    }
}