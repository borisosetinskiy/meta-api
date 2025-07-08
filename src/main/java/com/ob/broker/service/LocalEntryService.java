package com.ob.broker.service;

import com.ob.broker.common.*;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.*;
import com.ob.broker.common.local.LocalEntryOrder;
import com.ob.broker.common.local.LocalEntryStatus;
import com.ob.broker.common.local.OrderOperationType;
import com.ob.broker.common.model.*;
import com.ob.broker.common.request.OrderRequest;
import com.ob.broker.common.request.RequestType;
import com.ob.broker.util.ErrorUtil;
import com.ob.broker.util.LogUtil;
import com.ob.broker.util.TypeUtil;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.ob.broker.common.local.LocalEntryStatus.*;
import static com.ob.broker.common.local.OrderOperationType.BUY_LIMIT;
import static com.ob.broker.common.local.OrderOperationType.BUY_STOP;
import static com.ob.broker.util.Util.toLong;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class LocalEntryService implements EventConsumer {
    final TaskExecutor calculateTaskExecutor;
    final Map<Long, LocalEntryOrder> orderById = new ConcurrentHashMap<>();
    final Map<String, Map<LocalEntryOrder, Object>> ordersBySymbol = new ConcurrentHashMap<>();
    final ITradeBaseApi tradingAPI;
    final EventProducer eventProducer;
    final Set<EventType> type = Set.of(EventType.OPEN_ORDER, EventType.CANCEL_ORDER, EventType.REJECT_ORDER, EventType.PRICE);
    final Map<String, BigDecimal> maxPrice = new ConcurrentHashMap<>();
    final Map<String, BigDecimal> minPrice = new ConcurrentHashMap<>();
    String KEY ;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public LocalEntryService(ITradeBaseApi tradingAPI, EventProducer eventProducer, TaskExecutor calculateTaskExecutor) {
        KEY = Util.key(tradingAPI.getBrokerId(), tradingAPI.getAccountId());
        this.tradingAPI = tradingAPI;
        this.eventProducer = eventProducer;
        this.calculateTaskExecutor = calculateTaskExecutor;
        eventProducer.listener(EventTopic.PRICE, this);
        eventProducer.listener(EventTopic.ORDER, this);
    }

    public void create(OrderRequest command) {
        addOrder(command);
        sendEvent(OrderStateData.Placed, EventType.PENDING_ORDER, () -> toOrderData(command));
    }

    public void update(OrderRequest command) {
        var order = orderById.get(command.getTicket());
        if (order == null) throw new CodeException(Code.NOT_FOUND);
        if (command.getVersion() != order.getVersion()) {
            order.update(command);
            sendEvent(OrderStateData.Modified, EventType.UPDATE_PENDING_ORDER, order::toOrder);
        }
    }

    public void addOrder(OrderRequest command) {
        var order = LocalEntryOrder.create(command);
        ordersBySymbol.computeIfAbsent(command.getSymbol(), s -> new ConcurrentHashMap<>())
                .put(order, Boolean.TRUE);
        try {
            tradingAPI.subscribe(order.getSymbol(), true);
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
        orderById.put(order.getId(), order);
    }

    public void delete(Long ticketId) {
        var order = remove(ticketId);
        if (order != null) {
            order.getState().getAndSet(CANCELED);
            sendEvent(OrderStateData.Cancelled, EventType.CANCEL_ORDER, order::toOrder);
        }
    }

    public void delete(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) return;
        for (Long id : ticketIds) delete(id);
    }

    private LocalEntryOrder remove(Long id) {
        var order = orderById.remove(id);
        if (order != null) {
            var orders = ordersBySymbol.get(order.getSymbol());
            if (orders != null) {
                orders.remove(order);
                if (orders.isEmpty()) {
                    ordersBySymbol.remove(order.getSymbol());
                }
            }
        }
        return order;
    }

    private Set<LocalEntryOrder> compose(Set<Map.Entry<LocalEntryOrder, Object>> entries,
                                         Function<LocalEntryOrder, Boolean> condition) {
        Set<LocalEntryOrder> result = new HashSet<>(entries.size());
        for (Map.Entry<LocalEntryOrder, Object> entry : entries) {
            LocalEntryOrder order = entry.getKey();
            LocalEntryStatus state = order.getState().get();
            if ((state == CREATED || state == FAILED) && condition.apply(order)) {
                order.getState().getAndSet(TRIGGERED);
                result.add(order);
            }
        }
        return result;
    }

    private boolean test(LocalEntryOrder order, BigDecimal price, int bs) {
        OrderOperationType type = order.getType();
        return type.getSide() == bs && price.compareTo(order.getPrice()) == type.getDirection();
    }

    private OrderRequest buildRequestFrom(LocalEntryOrder order, BigDecimal price, IBaseApi api) {
        return OrderRequest.builder()
                .requestType(RequestType.OPEN)
                .accountId(api.getAccountId())
                .brokerId(api.getBrokerId())
                .lot(order.getLots())
                .symbol(order.getSymbol())
                .comment(order.getComment())
                .sl(order.getSl() != null ? order.getSl() : ZERO)
                .tp(order.getTp() != null ? order.getTp() : ZERO)
                .price(price)
                .type(order.getType().getSide() == 0 ? OrderTypeData.Buy : OrderTypeData.Sell)
                .version(0)
                .time(System.currentTimeMillis())
                .TID(order.getId())
                .build();
    }

    private void sendEvent(OrderStateData state, EventType eventType, Supplier<IOrder> supplier) {
        IOrder orderData = supplier.get();
        OrderEvent event = OrderEvent.builder()
                .eventType(eventType)
                .orderStateData(state)
                .brokerId(tradingAPI.getBrokerId())
                .accountId(tradingAPI.getAccountId())
                .symbol(orderData.getSymbol())
                .order(orderData)
                .ticket(orderData.getTicket())
                .TID(orderData.getTID())
                .build();
        eventProducer.eventConsumer(EventTopic.LOCAl_ORDER).accept(tradingAPI, event);
    }



    private void processOrder(LocalEntryOrder order, BigDecimal price, IBaseApi api) {
        if (ConnectionStatus.DEAD == api.getConnectionStatus() || order.getState().get() != TRIGGERED) return;
        try {
            log.info("stealth-on-triggered order {} ", order);
            var request = buildRequestFrom(order, price, api);
            tradingAPI.execute(request);
            order.getState().getAndSet(PROCESS);
        } catch (Exception e) {
            LogUtil.log("stealth-on-error", "api", api.getAccountId(), api.getBrokerId(),
                    text -> log.error("{} \"order\":{} \"error\":\"{}\"", text, order, e.getMessage()));
            order.getState().getAndSet(FAILED);
        }
    }


    @Override
    public void onNext(EventTopic topic, IBaseApi api, Event event) {
        if (api.getConnectionStatus() == ConnectionStatus.DEAD || ordersBySymbol.isEmpty()) return;
        if (type.contains(event.getEventType())) {
            if (event.getEventType() == EventType.PRICE) {
                QuoteData quote = (QuoteData) event;
                Map<LocalEntryOrder, Object> ordersMap = ordersBySymbol.get(quote.getSymbol());
                if (ordersMap != null && !ordersMap.isEmpty()) {
                    final Task task = new SimpleTask() {
                        @Override
                        public String connectorId() {
                            return KEY;
                        }
                        @Override
                        public void run() {
                            Set<LocalEntryOrder> out = compose(ordersMap.entrySet(), order ->
                                    test(order, quote.getAsk(), 0) || test(order, quote.getBid(), 1));
                            for (LocalEntryOrder order : out) {
                                BigDecimal price = (order.getType() == BUY_LIMIT || order.getType() == BUY_STOP)
                                        ? quote.getAsk() : quote.getBid();
                                processOrder(order, price, api);
                            }
                        }
                    };
                    calculateTaskExecutor.submit(KEY, task);
                }
            } else if (event instanceof OrderEvent orderEvent) {
                handleOrderEvent(api, orderEvent);
            }
        }
    }

    @Override
    public String getId() {
        return "ENTRY-SERVICE";
    }

    private void handleOrderEvent(IBaseApi api, OrderEvent orderEvent) {
        IOrder order = orderEvent.getOrder();
        long tid = toLong(orderEvent.getTID());
        LocalEntryOrder localOrder = remove(tid);

        if (localOrder != null) {
            if (orderEvent.getEventType() == EventType.OPEN_ORDER) {
                log.info("stealth-on-open order {} local-order {}", order, localOrder);
                localOrder.getState().getAndSet(COMPLETED);
                sendEvent(OrderStateData.Cancelled, EventType.CANCEL_ORDER, localOrder::toOrder);
            } else if (orderEvent.getEventType() == EventType.CANCEL_ORDER ||
                       orderEvent.getEventType() == EventType.REJECT_ORDER) {
                localOrder.getState().getAndSet(CANCELED);
                sendEvent(OrderStateData.Cancelled, EventType.CANCEL_ORDER, localOrder::toOrder);
                log.info("stealth-on-reject order {} local-order {}", order, localOrder);
            }
        }
    }

    private OrderData toOrderData(OrderRequest request) {
        Long id = TypeUtil.castType(request.getTID(), Long.class);
        return OrderData.builder()
                .symbol(request.getSymbol())
                .lot(request.getLot())
                .comment(request.getComment())
                .ticket(id)
                .price(request.getPrice())
                .time(request.getTime())
                .orderType(request.getType())
                .sl(request.getSl())
                .tp(request.getTp())
                .TID(id)
                .build();
    }

    @Override
    public String toString() {
        return "LocalEntryService";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o != null && getClass() == o.getClass());
    }


}

