package com.ob.broker.common;

import com.ob.broker.common.event.Event;
import com.ob.broker.common.event.EventConsumer;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.request.OrderRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Data
public class OrderTask implements Runnable, EventConsumer {
    static final AtomicLong counter = new AtomicLong();
    int maxAttempts;
    int currentAttempt;
    OrderRequest request;
    String id;
    ITradeBaseApi tradeBaseApi;
    OrderAction orderAction;
    OrderTaskExecutor executor;

    public OrderTask(int maxAttempts
            , OrderRequest request
            , ITradeBaseApi tradeBaseApi
            , OrderAction orderAction
            , OrderTaskExecutor executor) {
        this.maxAttempts = maxAttempts;
        this.request = request;
        this.id = request.getBrokerId() + "." + request.getAccountId() + "." + counter.incrementAndGet();
        this.tradeBaseApi = tradeBaseApi;
        this.orderAction = orderAction;
        this.executor = executor;
        tradeBaseApi.addListener(EventTopic.ERROR, this);
        tradeBaseApi.addListener(EventTopic.ORDER_REQUEST, this);
        tradeBaseApi.addListener(EventTopic.ORDER, this);
    }

    @Override
    public void onNext(EventTopic topic, IBaseApi api, Event event) {

        var optional = orderAction.isSuccess(request, topic, event, id);
        optional.ifPresent(result -> {
            try {
                if (result) {
                    removeListeners();
                } else if (maxAttempts > 1 && currentAttempt < maxAttempts) {
                    executor.execute(request, OrderTask.this,
                            (orderData, e) -> log.error("Trading task {} for request {} failed", id, request, e)
                    );
                }
            } catch (Exception e) {
                log.error("Trading task {} for request {} failed", id, request, e);
            }
        });
    }

    private void removeListeners() {
        tradeBaseApi.removeListener(EventTopic.ERROR, this);
        tradeBaseApi.removeListener(EventTopic.ORDER_REQUEST, this);
        tradeBaseApi.removeListener(EventTopic.ORDER, this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void run() {
        currentAttempt++;
        orderAction.execute(request, tradeBaseApi, id);
    }
}
