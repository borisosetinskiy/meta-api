package com.ob.api.mtx.mt5;


@FunctionalInterface
public interface OnQuoteHistory {
    void invoke(MT5API sender, QuoteHistoryEventArgs args);
}
