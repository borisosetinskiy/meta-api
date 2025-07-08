package com.ob.broker.common;

import com.ob.broker.common.model.AccountData;
import com.ob.broker.common.model.CloseOrderData;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.IRequest;

import java.util.List;
import java.util.Map;

public interface ITradeBaseApi extends IBaseApi {
    void execute(List<IRequest> requests);

    void execute(IRequest request);


    List<? extends OrderData> opened();

    List<? extends OrderData> pending();

    List<? extends CloseOrderData> closed();


    Map<Integer, OrderData> trades();
    Map<Integer, OrderData> orders();
    Map<Integer, CloseOrderData> history();

    OrderData findOrder(Long ticket);

    List<? extends OrderData> findOrders(List<Long> tickets);

    AccountData getAccountData();
}
