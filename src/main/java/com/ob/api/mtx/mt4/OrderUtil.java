package com.ob.api.mtx.mt4;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.Event;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.event.OrderErrorEvent;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.OrderRequest;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Optional;

@UtilityClass
public class OrderUtil {
    public static OrderData getOrder(Long ticket, Map<Integer, OrderData> orders) {
        OrderData order = orders.get(ticket.intValue());
        if (order == null)
            throw new CodeException(String.format("Can't find order %s. Api error.", ticket), Code.NOT_FOUND);
        return order;
    }

    public static Optional<Boolean> isSuccess(OrderRequest request, EventTopic topic, Event event) {
        if(event instanceof OrderErrorEvent errorEvent){
            if(errorEvent.getRequest() instanceof OrderRequest errorOrderRequest){
                if(request.getRequestId().equals(errorOrderRequest.getRequestId())){
                    return Optional.of(false);
                }
            }
        }else{
            if(topic.equals(EventTopic.ORDER_REQUEST)){
                if(event instanceof OrderRequest processedOrderRequest){
                    if(request.getRequestId().equals(processedOrderRequest.getRequestId())){
                        return Optional.of(true);
                    }else{
                        var tid = request.getTID();
                        var processedTid = processedOrderRequest.getTID();
                        if(tid != null && tid.equals(processedTid)){
                            return Optional.of(true);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
