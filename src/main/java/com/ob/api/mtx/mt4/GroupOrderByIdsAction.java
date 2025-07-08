package com.ob.api.mtx.mt4;

import com.ob.broker.common.ITradeBaseApi;
import com.ob.broker.common.OrderAction;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.GroupOrderRequest;
import com.ob.broker.common.request.OrderRequest;
import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Data
public class GroupOrderByIdsAction implements OrderAction {
    final BiConsumer<OrderRequest , Predicate<OrderData>> consumer;
    @Override
    public void execute(OrderRequest request, ITradeBaseApi tradeBaseApi, String id) {
        if(request instanceof GroupOrderRequest groupOrderRequest){
            var ids = groupOrderRequest.getTickets();
            consumer.accept(request, o -> ids.contains(o.getTicket()));
        }
    }
}
