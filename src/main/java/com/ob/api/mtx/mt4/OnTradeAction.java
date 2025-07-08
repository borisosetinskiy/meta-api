package com.ob.api.mtx.mt4;

import com.ob.broker.common.ITradeBaseApi;
import com.ob.broker.common.OrderAction;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.Event;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.OrderRequest;
import lombok.Data;

import java.util.Optional;
import java.util.function.BiConsumer;

@Data
public class OnTradeAction implements OrderAction {
    final BiConsumer<OrderRequest, OrderData> consumer;
    @Override
    public void execute(OrderRequest request, ITradeBaseApi tradeBaseApi, String id) {
        if (request.getTicket() == null)
            throw new CodeException("TicketId was not provided. Api error.", Code.INVALID_PARAM);
        var order = OrderUtil.getOrder(request.getTicket(), tradeBaseApi.trades());
        try {
            consumer.accept(request, order);
        }catch (CodeException e) {
            throw new OrderException(e, order);
        }
    }
    @Override
    public Optional<Boolean> isSuccess(OrderRequest request, EventTopic topic, Event event, String id) {
        return OrderUtil.isSuccess(request, topic, event);
    }
}
