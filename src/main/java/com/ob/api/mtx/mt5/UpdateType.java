package com.ob.api.mtx.mt5;

import lombok.Getter;

@Getter
public enum UpdateType {
    Unknown,
    PendingClose,
    MarketOpen,
    PendingOpen,
    MarketClose,
    PartialClose,
    Started,
    Filled,
    Cancelling,
    MarketModify,
    PendingModify,
    OnStopLoss,
    OnTakeProfit,
    OnStopOut,
    Balance,
    Expired,
    Rejected;

    public static final int SIZE = Integer.SIZE;

    public static UpdateType forValue(int value) {
        return values()[value];
    }

    public int getValue() {
        return this.ordinal();
    }
}