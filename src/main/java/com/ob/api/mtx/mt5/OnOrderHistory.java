package com.ob.api.mtx.mt5;


@FunctionalInterface
public interface OnOrderHistory {
    void invoke(MT5API sender, Order[] orders, DealInternal[] internal_deals, OrderInternal[] internal_orders);
}