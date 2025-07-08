package com.ob.api.mtx.mt4.connector.util;

import com.ob.api.mtx.mt4.connector.entity.trading.OrderType;
import com.ob.broker.common.model.OrderTypeData;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HelpUtil {

    public static Long toLong(int value) {
        return (long) value;
    }

    public static Long toLong(Integer value) {
        if (value == null) {
            return null;
        }
        return value.longValue();
    }

    public static OrderTypeData toOrderTypeData(OrderType orderType) {
        return switch (orderType) {
            case BUY -> OrderTypeData.Buy;
            case SELL -> OrderTypeData.Sell;
            case BUY_LIMIT -> OrderTypeData.BuyLimit;
            case SELL_LIMIT -> OrderTypeData.SellLimit;
            case BUY_STOP -> OrderTypeData.BuyStop;
            case SELL_STOP -> OrderTypeData.SellStop;
            case CREDIT, BALANCE -> OrderTypeData.Balance;
            default -> throw new IllegalArgumentException("Unknown OperationType: " + orderType);
        };
    }
}
