package com.ob.api.mtx.mt5;

public enum ConnectProgress {
    SendLogin,
    SendAccountPassword,
    AcceptAuthorized,
    RequestTradeInfo,
    Connected,
    Exception,
    Disconnect;

    public static final int SIZE = Integer.SIZE;

    public static ConnectProgress forValue(int value) {
        return values()[value];
    }

    public int getValue() {
        return this.ordinal();
    }
}