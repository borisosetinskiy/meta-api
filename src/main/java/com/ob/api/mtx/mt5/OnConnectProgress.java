package com.ob.api.mtx.mt5;

@FunctionalInterface
public interface OnConnectProgress {
    void invoke(MT5API sender, ConnectEventArgs args);
}