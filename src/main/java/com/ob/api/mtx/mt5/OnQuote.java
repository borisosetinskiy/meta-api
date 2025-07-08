package com.ob.api.mtx.mt5;


@FunctionalInterface
public interface OnQuote {
    void invoke(MT5API sender, Quote quote);
}