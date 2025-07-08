package com.ob.api.dx.service;

import com.ob.api.dx.TradeDxApi;
import com.ob.api.dx.model.data.*;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.event.EventType;
import com.ob.broker.common.event.OrderEvent;
import com.ob.broker.common.model.IOrder;
import com.ob.broker.common.model.OrderStateData;
import com.ob.broker.common.model.OrderTypeData;
import com.ob.broker.util.Util;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Data
public class OrderHandler {
    final TradeDxApi tradeDxApi;

    public void handle(Portfolio portfolio) {
        var instrumentService = tradeDxApi.getInstrumentService();
        var positions = OrderParserUtil.parsePositions(getBrokerId()
                , getAccountId()
                , instrumentService
                , portfolio.getPositions());
        var dxOrders = OrderParserUtil.parseOrders(getBrokerId()
                , getAccountId()
                , instrumentService
                , portfolio.getOrders());
        positions.forEach((aLong, orderData) -> handleTrade(orderData, dxOrders));
        var orders = dxOrders.orders();
        orders.forEach((orderId, orderData) -> handleOrder(orderData, dxOrders));

        var canceledOrders = dxOrders.canceledOrders();
        canceledOrders.forEach((orderId, orderData) -> handleCancelOrder(orderData));

        var rejectOrders = dxOrders.rejectOrders();
        rejectOrders.forEach((orderId, orderData) -> handleRejectOrder(orderData));

    }

    private void handleRejectOrder(DxRejectOrderData orderData) {
        var request = tradeDxApi.getTradingService().getOrderRequests().getIfPresent(orderData.getTID());
        var event = orderEvent(orderData.getTID(), EventType.REJECT_ORDER, orderData);
        event.setRequest(request);
        event.setError(new CodeException(orderData.getReason(), Code.REQUEST_REJECTED));
        tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER_REQUEST)
                .accept(tradeDxApi, event);
    }

    public void handleCancelOrder(DxCanceledOrderData dxCanceledOrderData) {
        var pendingOrders = tradeDxApi.getTradingService().getPending();
        if (pendingOrders.containsKey(dxCanceledOrderData.getTicket())) {
            pendingOrders.remove(dxCanceledOrderData.getTicket());
            tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                    .accept(tradeDxApi, orderEvent(dxCanceledOrderData.getTID(), EventType.CANCEL_ORDER, dxCanceledOrderData));
        }
    }

    public void handleOrder(DxOrderData order, DxOrders dxOrders) {
        if (OrderTypeData.Buy.equals(order.getOrderType())
            || OrderTypeData.Sell.equals(order.getOrderType())) {
            return;
        }
        var slOrders = dxOrders.sl();
        var tpOrders = dxOrders.tp();
        var oldOrder = tradeDxApi.getTradingService().getPending().get(order.getTicket());
        if (oldOrder != null) {
            setConditions(order, oldOrder, slOrders, tpOrders);
            tradeDxApi.getTradingService().getPending().put(order.getTicket(), order);
            tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                    .accept(tradeDxApi, orderEvent(order.getTID(), EventType.UPDATE_PENDING_ORDER, order));
        } else {
            setConditions(order, null, slOrders, tpOrders);
            tradeDxApi.getTradingService().getPending().put(order.getTicket(), order);
            tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                    .accept(tradeDxApi, orderEvent(order.getTID(), EventType.PENDING_ORDER, order));
        }
    }

    public void handleTrade(DxOrderData trade, DxOrders dxOrders) {
        var closedOrders = dxOrders.closedOrders();
        var orders = dxOrders.orders();
        var openedOrder = orders.get(trade.getTicket());
        var slOrders = dxOrders.sl();
        var tpOrders = dxOrders.tp();
        var closedOrder = closedOrders.get(trade.getTicket());
        var trades = tradeDxApi.getTradingService().getOpened();
        var oldTrade = trades.get(trade.getTicket());
        if (closedOrder != null) {
            if (oldTrade != null) {
                closedOrder.setOpenTime(oldTrade.getTime());
                closedOrder.setOpenPrice(oldTrade.getPrice());
                closedOrder.setBalance(getTradeDxApi().getAccountData().getBalance());
                var lot = trade.getLot();
                if (lot == null || lot.compareTo(BigDecimal.ZERO) == 0) {
                    //FULL CLOSE
                    trades.remove(trade.getTicket());
                    tradeDxApi.getTradingService().getClosedOrders().put(trade.getTicket(), closedOrder);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(trade.getTID(), EventType.CLOSE_ORDER, closedOrder));

                } else {
                    //PARTIAL CLOSE
                    setCondition(trade, Types.Type.STOP, oldTrade.getSlTID(), oldTrade.getSlOrderId()
                            , oldTrade.getSlOrderCode()
                            , oldTrade.getSl(), oldTrade.getSlVersion());
                    setCondition(trade, Types.Type.LIMIT, oldTrade.getTpTID(), oldTrade.getTpOrderId()
                            , oldTrade.getTpOrderCode()
                            , oldTrade.getTp(), oldTrade.getTpVersion());
                    trades.put(trade.getTicket(), trade);
                    tradeDxApi.getTradingService().getClosedOrders().put(trade.getTicket(), closedOrder);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(trade.getTID(), EventType.CLOSE_PARTIAL_ORDER, closedOrder));
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(trade.getTID(), EventType.OPEN_ORDER, trade));

                }
            }
        } else {
            setConditions(Objects.requireNonNullElse(openedOrder, trade), oldTrade, slOrders, tpOrders);
            if (oldTrade == null) {
                //Open
                if (openedOrder == null) {
                    trades.put(trade.getTicket(), trade);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(trade.getTicket(), EventType.OPEN_ORDER, trade));
                } else {
                    trades.put(openedOrder.getTicket(), openedOrder);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(openedOrder.getTID(), EventType.OPEN_ORDER, openedOrder));
                }

            } else {
                //Modify
                if (openedOrder == null) {
                    tradeDxApi.getTradingService().getOpened().put(trade.getTicket(), trade);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(trade.getTicket(), EventType.UPDATE_ORDER, trade));
                } else {
                    tradeDxApi.getTradingService().getOpened().put(openedOrder.getTicket(), openedOrder);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER)
                            .accept(tradeDxApi, orderEvent(openedOrder.getTID(), EventType.UPDATE_ORDER, openedOrder));
                }


            }
        }
    }

    boolean lessVersion(Integer a, Integer b) {
        return a != null && b != null && a < b;
    }

    OrderEvent orderEvent(Object TID, EventType eventType, IOrder order) {
        var builder = OrderEvent.builder();
        builder.accountId(getAccountId());
        builder.brokerId(getBrokerId());
        builder.eventType(eventType);
        builder.order(order);
        builder.symbol(order.getSymbol());
        builder.TID(TID);
        builder.ticket(order.getTicket());
        switch (eventType) {
            case OPEN_ORDER:
                builder.orderStateData(OrderStateData.Opened);
                break;
            case CLOSE_ORDER:
                builder.orderStateData(OrderStateData.Closed);
                break;
            case CLOSE_PARTIAL_ORDER:
                builder.orderStateData(OrderStateData.PartialClosed);
                break;
            case UPDATE_ORDER, UPDATE_PENDING_ORDER:
                builder.orderStateData(OrderStateData.Modified);
                break;
            case PENDING_ORDER:
                builder.orderStateData(OrderStateData.Placed);
                break;
            case CANCEL_ORDER:
                builder.orderStateData(OrderStateData.Cancelled);
                break;
            case REJECT_ORDER:
                builder.orderStateData(OrderStateData.Rejected);
                break;
        }
        return builder.build();
    }

    void setConditions(DxOrderData newOrder, DxOrderData oldOrder
            , Map<Long, DxConditionOrderData> slOrders
            , Map<Long, DxConditionOrderData> tpOrders) {
        var slOrder = slOrders.get(newOrder.getTicket());
        if (slOrder != null) {
            setCondition(newOrder, slOrder.getType(), String.valueOf(slOrder.getTID()), slOrder.getTicket()
                    , slOrder.getOrderCode()
                    , slOrder.getPrice(), slOrder.getVersion());
        } else {
            if (oldOrder != null) {
                setCondition(newOrder, Types.Type.STOP, oldOrder.getSlTID(), oldOrder.getSlOrderId()
                        , oldOrder.getSlOrderCode()
                        , oldOrder.getSl(), oldOrder.getSlVersion());
            }
        }
        var tpOrder = tpOrders.get(newOrder.getTicket());
        if (tpOrder != null) {
            setCondition(newOrder, tpOrder.getType(), String.valueOf(tpOrder.getTID()), tpOrder.getTicket()
                    , tpOrder.getOrderCode()
                    , tpOrder.getPrice(), tpOrder.getVersion());
        } else {
            if (oldOrder != null) {
                setCondition(newOrder, Types.Type.LIMIT, oldOrder.getTpTID(), oldOrder.getTpOrderId()
                        , oldOrder.getTpOrderCode()
                        , oldOrder.getTp(), oldOrder.getTpVersion());
            }
        }

    }

    void setCondition(DxOrderData order, Types.Type type, String tid, Long orderId, String orderCode, BigDecimal price, Integer version) {
        if (Types.Type.LIMIT.equals(type)) {
            order.setTp(price);
            if (Util.isMoreThanZero(price)) {
                order.setTpOrderId(orderId);
                order.setTpTID(tid);
                order.setTpVersion(version);
                order.setTpOrderCode(orderCode);
            } else {
                order.setTpOrderId(null);
                order.setTpTID(null);
                order.setTpVersion(0);
            }
        } else if (Types.Type.STOP.equals(type)) {
            order.setSl(price);
            if (Util.isMoreThanZero(price)) {
                order.setSlOrderId(orderId);
                order.setSlTID(tid);
                order.setSlVersion(version);
                order.setSlOrderCode(orderCode);
            } else {
                order.setSlOrderId(null);
                order.setSlTID(null);
                order.setSlVersion(0);
            }
        }
    }

    private Object getAccountId() {
        return tradeDxApi.getAccountId();
    }

    private Long getBrokerId() {
        return tradeDxApi.getBrokerId();
    }
}
/*

eventProducer.eventConsumer(EventTopic.ORDER)
                            .accept(TradingForexwareApi.this, event);
OrderEvent toOrderEvent(ClientApiMt4.OrderState orderState) {
        var builder = OrderEvent.builder();
        builder.accountId(getAccountId());
        builder.brokerId(getBrokerId());
        var status = orderState.getStatus();
        if (ClientApiMt4.OrderStatus.EXECUTED.equals(status)) {
            if (orderState.hasOrder()) {
                var order = orderState.getOrder();
                var contractInfo = toContractInfo(order.getContract().getSymbol());
                if (order.getCloseTime() > 0) {
                    var tradeData = toCloseOrderData(getBrokerId(), getAccountId(), accountStateRef.get(), order, contractInfo);
                    builder.order(tradeData);
                    builder.orderStateData(OrderStateData.Closed);
                    builder.eventType(EventType.CLOSE_ORDER);
                } else {
                    var orderData = toOrderData(getBrokerId(), getAccountId(), order, contractInfo.getDigits());
                    builder.order(orderData);
                    if (openOrders.containsKey(order.getOrderId().getOrderId())) {
                        builder.orderStateData(OrderStateData.Modified);
                        builder.eventType(EventType.UPDATE_ORDER);
                    } else {
                        builder.orderStateData(OrderStateData.Opened);
                        builder.eventType(EventType.OPEN_ORDER);
                    }
                }
            }
        } else if (ClientApiMt4.OrderStatus.CANCELED.equals(status) ||
                   ClientApiMt4.OrderStatus.REJECTED.equals(status)) {
            var error = FxddUtil.toCodeException(orderState);
            builder.error(error);
            if (ClientApiMt4.OrderStatus.CANCELED.equals(status)) {
                builder.orderStateData(OrderStateData.Cancelled);
                builder.eventType(EventType.CANCEL_ORDER);
            } else {
                builder.orderStateData(OrderStateData.Rejected);
                builder.eventType(EventType.REJECT_ORDER);
            }
        } else if (ClientApiMt4.OrderStatus.PENDING.equals(status)) {
            if (orderState.hasOrder()) {
                var order = orderState.getOrder();
                var contractInfo = toContractInfo(order.getContract().getSymbol());
                var orderData = toOrderData(getBrokerId(), getAccountId(), order, contractInfo.getDigits());
                builder.order(orderData);
                if (pendingOrders.containsKey(order.getOrderId().getOrderId())) {
                    builder.orderStateData(OrderStateData.Modified);
                    builder.eventType(EventType.UPDATE_PENDING_ORDER);
                } else {
                    builder.orderStateData(OrderStateData.Placed);
                    builder.eventType(EventType.PENDING_ORDER);
                }
            }
        }
        if (orderState.hasOrder()) {
            var order = orderState.getOrder();
            var tid = (long) order.getMagicNumber();
            builder.TID(tid < 0 ? tid * -1 : tid);
            builder.ticket(order.getOrderId().getOrderId());
            builder.symbol(order.getContract().getSymbol());
        }
        return builder.build();
    }

    private OrderEvent onOrderState(ClientApiMt4.OrderState orderState) {
        var event = toOrderEvent(orderState);
        var status = orderState.getStatus();
        if (ClientApiMt4.OrderStatus.EXECUTED.equals(status)) {
            if (orderState.hasOrder()) {
                var order = orderState.getOrder();
                if (order.getCloseTime() > 0) {
                    openOrders.remove(order.getOrderId().getOrderId());
                    closeOrders.put(order.getOrderId().getOrderId(), order);
                } else {
                    addOrder(order);
                }
            }
        } else if (ClientApiMt4.OrderStatus.CANCELED.equals(status)) {
            if (orderState.hasOrder()) {
                var order = orderState.getOrder();
                pendingOrders.remove(order.getOrderId().getOrderId());
            }
        } else if (ClientApiMt4.OrderStatus.PENDING.equals(status)) {
            if (orderState.hasOrder()) {
                var order = orderState.getOrder();
                pendingOrders.put(order.getOrderId().getOrderId(), order);
            }
        }
        return event;
    }

 */