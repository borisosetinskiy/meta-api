package com.ob.api.mtx.mt5;

import lombok.Getter;

@Getter
public enum TradeType {
    TradePrice(0),
    RequestExecution(1),
    InstantExecution(2),
    MarketExecution(3),
    ExchangeExecution(4),
    SetOrder(5),
    ModifyDeal(6),
    ModifyOrder(7),
    CancelOrder(8),
    Transfer(9),
    ClosePosition(10),
    ActivateOrder(100),
    ActivateStopLoss(101),
    ActivateTakeProfit(102),
    ActivateStopLimitOrder(103),
    ActivateStopOutOrder(104),
    ActivateStopOutPosition(105),
    ExpireOrder(106),
    ForSetOrder(200),
    ForOrderPrice(201),
    ForModifyDeal(202),
    ForModifyOrder(203),
    ForCancelOrder(204),
    ForActivateOrder(205),
    ForBalance(206),
    ForActivateStopLimitOrder(207),
    ForClosePosition(208);

    public static final int SIZE = Integer.SIZE;

    private int intValue;


    private TradeType(int value) {
        intValue = value;
    }

    public static TradeType forValue(int value) {
        for (TradeType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }

    public int getValue() {
        return intValue;
    }
}