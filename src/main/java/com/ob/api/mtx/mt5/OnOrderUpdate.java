package com.ob.api.mtx.mt5;


@FunctionalInterface
public interface OnOrderUpdate {
    void invoke(MT5API sender, OrderUpdate update);
}