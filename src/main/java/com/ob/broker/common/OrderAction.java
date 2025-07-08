package com.ob.broker.common;


import com.ob.broker.common.event.Event;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.request.OrderRequest;

import java.util.Optional;

public interface OrderAction {
    void execute(OrderRequest request, ITradeBaseApi tradeBaseApi, String id);
    default Optional<Boolean> isSuccess(OrderRequest request, EventTopic topic, Event event, String id){
        return Optional.of(true);
    }

}
