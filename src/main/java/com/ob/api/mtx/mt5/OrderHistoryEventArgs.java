package com.ob.api.mtx.mt5;

import java.util.List;

public class OrderHistoryEventArgs {
    public List<Order> Orders;
    public List<DealInternal> InternalDeals;
    public List<OrderInternal> InternalOrders;
    public int Action;
}