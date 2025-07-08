package com.ob.api.mtx.mt4;

import com.ob.broker.common.ITradeBaseApi;
import com.ob.broker.common.OrderAction;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.model.OrderTypeData;
import com.ob.broker.common.request.OrderRequest;
import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Data
public class CloseOrderByTypeAction implements OrderAction {
    final BiConsumer<OrderRequest , Predicate<OrderData>> consumer;
    @Override
    public void execute(OrderRequest request, ITradeBaseApi tradeBaseApi, String id) {
        if (request.getType() == null)
            throw new CodeException("Type was not provided. Api error.", Code.INVALID_PARAM);
        consumer.accept(request, o -> (OrderTypeData.Buy.equals(o.getOrderType())
                                       || OrderTypeData.Sell.equals(o.getOrderType()))
                                      && o.getOrderType().getValue() == (request.getType().getValue()));
    }

}
