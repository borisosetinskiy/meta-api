package com.ob.api.mtx.mt5;


@FunctionalInterface
public interface OnOrderProgress {
    void invoke(MT5API sender, OrderProgress progress);
}