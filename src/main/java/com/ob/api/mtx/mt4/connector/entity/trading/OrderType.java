package com.ob.api.mtx.mt4.connector.entity.trading;

import lombok.Getter;

import java.util.Map;

@Getter
public enum OrderType {
    BUY(0, 0), SELL(1, 1), BUY_LIMIT(2, 0), SELL_LIMIT(3, 1), BUY_STOP(4, 0), SELL_STOP(5, 1), BALANCE(6, 6), CREDIT(7, 6);

    final static Map<Integer, OrderType> VALUES = Map.of(
            OrderType.BUY.getValue(), OrderType.BUY
            , OrderType.SELL.getValue(), OrderType.SELL
            , OrderType.BUY_LIMIT.getValue(), OrderType.BUY_LIMIT
            , OrderType.SELL_LIMIT.getValue(), OrderType.SELL_LIMIT
            , OrderType.BUY_STOP.getValue(), OrderType.BUY_STOP
            , OrderType.SELL_STOP.getValue(), OrderType.SELL_STOP
            , OrderType.BALANCE.getValue(), OrderType.BALANCE
            , OrderType.CREDIT.getValue(), OrderType.CREDIT
    );
    private final int value;
    private final int type;

    OrderType(int value, int type) {
        this.value = value;
        this.type = type;
    }

    public static OrderType of(int key) {
        return VALUES.get(key);
    }

}
