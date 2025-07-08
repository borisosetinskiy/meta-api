package com.ob.broker.common;


import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.OrderRequest;

import java.util.function.BiConsumer;

public interface OrderTaskExecutor {
    void execute(OrderRequest request, Runnable command, BiConsumer<OrderData, Exception> errorHandler);
}
