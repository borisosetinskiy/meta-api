package com.ob.api.mtx.mt4;

import com.ob.broker.common.ITradeBaseApi;
import com.ob.broker.common.OrderAction;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.Event;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.request.OrderRequest;
import lombok.Data;

import java.util.Optional;
import java.util.function.Consumer;

import static com.ob.broker.util.Util.toLong;

@Data
public class NewOrderAction implements OrderAction {
    final Consumer<OrderRequest> consumer;
    @Override
    public void execute(OrderRequest request, ITradeBaseApi tradeBaseApi, String id) {
        if (request.getSymbol() == null)
            throw new CodeException("Symbol was not provided", Code.INVALID_PARAM);
        consumer.accept(request);
    }
    @Override
    public Optional<Boolean> isSuccess(OrderRequest request, EventTopic topic, Event event, String id) {
        return OrderUtil.isSuccess(request, topic, event);
    }
}
