package com.ob.api.mtx.mt5;


import java.io.IOException;


@FunctionalInterface
public interface OnTradeHistory {
    void invoke(MT5API sender, OrderHistoryEventArgs args) throws IOException, TimeoutException;
}