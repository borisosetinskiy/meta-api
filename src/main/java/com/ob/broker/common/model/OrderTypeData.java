package com.ob.broker.common.model;

import lombok.Getter;

@Getter
public enum OrderTypeData {
    Buy(0),
    Sell(1),
    BuyLimit(2),
    SellLimit(3),
    BuyStop(4),
    SellStop(5),
    BuyStopLimit(6),
    SellStopLimit(7),
    BuyLimitLocal(8),
    SellLimitLocal(9),
    BuyStopLocal(10),
    SellStopLocal(11),
    Balance(12),
    Credit(13),
    CloseBy(14),
    CloseCancel(15),
    TakeProfit(16),
    StopLoss(17);
    final int value;

    OrderTypeData(int value) {
        this.value = value;
    }

    public static OrderTypeData valueOf(int value) {
        for (OrderTypeData type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OrderTypeData value: " + value);
    }

}
