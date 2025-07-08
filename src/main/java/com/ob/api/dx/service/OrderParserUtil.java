package com.ob.api.dx.service;

import com.ob.api.dx.model.data.*;
import com.ob.broker.common.model.ContractData;
import com.ob.broker.common.model.OrderTypeData;
import com.ob.broker.util.Util;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UtilityClass
public class OrderParserUtil {

    public static DxOrders parseOrders(Long brokerId
            , Object accountId
            , InstrumentService instrumentService
            , List<Order> orderList) {
        final Map<Long, DxOrderData> orders = new ConcurrentHashMap<>();
        final Map<Long, DxCloseOrderData> closedOrders = new ConcurrentHashMap<>();
        final Map<Long, DxCanceledOrderData> canceledOrders = new ConcurrentHashMap<>();
        final Map<Long, DxConditionOrderData> sl = new HashMap<>();
        final Map<Long, DxConditionOrderData> tp = new HashMap<>();
        final Map<Long, DxRejectOrderData> rejectOrders = new HashMap<>();

        orderList.forEach(order -> {
            var dxOrder = OrderParserUtil.parseDxOrder(brokerId
                    , accountId
                    , order
                    , instrumentService);
            switch (dxOrder) {
                case DxOrderData orderData -> orders.put(orderData.getTicket(), orderData);
                case DxConditionOrderData conditionOrder -> {

                    if (conditionOrder.getType() == Types.Type.STOP) {
                        sl.put(conditionOrder.getTicket(), conditionOrder);

                    } else if (conditionOrder.getType() == Types.Type.LIMIT) {
                        tp.put(conditionOrder.getTicket(), conditionOrder);

                    }
                }
                case DxRejectOrderData rejectOrderData ->
                        rejectOrders.put(rejectOrderData.getTicket(), rejectOrderData);
                case DxCloseOrderData dxCloseOrderData ->
                        closedOrders.put(dxCloseOrderData.getTicket(), dxCloseOrderData);
                case DxCanceledOrderData dxCanceledOrderData ->
                        canceledOrders.put(dxCanceledOrderData.getTicket(), dxCanceledOrderData);
                case null, default -> {
                }
            }
        });
        return new DxOrders(orders
                , closedOrders
                , canceledOrders
                , rejectOrders
                , sl
                , tp);
    }

    public static boolean isClose(OrderLeg orderLeg) {
        return "CLOSE".equals(orderLeg.getPositionEffect());
    }

    public static DxOrder parseDxOrder(Long brokerId, Object accountId
            , Order order, InstrumentService instrumentService) {

        Types.Type type = order.getType();
        OrderLeg orderLeg = order.getLegs().getFirst();
        var positionCode = orderLeg.getPositionCode();
        var positionId = Long.valueOf(positionCode);

        BigDecimal price = orderLeg.getAveragePrice();
        if (!Util.isMoreThanZero(price)) {
            price = orderLeg.getPrice();
        }
        final Types.Side side = order.getSide();
        final OrderTypeData orderTypeData = getOrderTypeData(side, type);
        ContractData contractData = instrumentService.getInstruments().get(order.getInstrument());
        DxOrder dxOrder = switch (order.getStatus()) {
            case COMPLETED -> {
                if (isClose(orderLeg)) {
                    DxCloseOrderData closeOrderData = new DxCloseOrderData();
                    closeOrderData.setAccountId(accountId);
                    closeOrderData.setTicket(positionId);
                    closeOrderData.setBrokerId(brokerId);
                    closeOrderData.setClosePrice(price);
                    closeOrderData.setCloseTime(order.getTransactionTime().toEpochSecond() * 1000);
                    closeOrderData.setOrderType(orderTypeData);
                    closeOrderData.setTID(order.getClientOrderId());
                    closeOrderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                    closeOrderData.setSymbol(order.getInstrument());
                    closeOrderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                    var cashTransactions = order.getCashTransactions();
                    if (cashTransactions != null && !cashTransactions.isEmpty()) {
                        var cashTransaction = cashTransactions.getFirst();
                        closeOrderData.setProfit(cashTransaction.getValue());
                    }
                    closeOrderData.setVersion(order.getVersion());
                    yield closeOrderData;
                } else {
                    DxOrderData orderData = new DxOrderData();
                    orderData.setAccountId(accountId);
                    orderData.setTicket(positionId);
                    orderData.setBrokerId(brokerId);
                    orderData.setPrice(price);
                    orderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                    orderData.setOrderType(orderTypeData);
                    orderData.setTID(order.getClientOrderId());
                    orderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                    orderData.setSymbol(order.getInstrument());
                    orderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                    var cashTransactions = order.getCashTransactions();
                    if (cashTransactions != null && !cashTransactions.isEmpty()) {
                        var cashTransaction = cashTransactions.getFirst();
                        if (CashTransaction.Type.COMMISSION.equals(cashTransaction.getType())) {
                            orderData.setCommission(cashTransaction.getValue());
                        } else if (CashTransaction.Type.EX_DIVIDEND.equals(cashTransaction.getType())) {
                            orderData.setSwap(cashTransaction.getValue());
                        }
                    }
                    orderData.setVersion(order.getVersion());
                    yield orderData;
                }
            }
            case WORKING -> {
                if (isClose(orderLeg)) {
                    DxConditionOrderData dxConditionOrderData = new DxConditionOrderData();
                    dxConditionOrderData.setPrice(orderLeg.getPrice());
                    dxConditionOrderData.setType(type);
                    dxConditionOrderData.setTicket(positionId);
                    dxConditionOrderData.setOrderCode(order.getOrderCode());
                    dxConditionOrderData.setTID(order.getClientOrderId());
                    dxConditionOrderData.setVersion(order.getVersion());
                    yield dxConditionOrderData;
                } else {
                    DxOrderData orderData = new DxOrderData();
                    orderData.setAccountId(accountId);
                    orderData.setTicket(positionId);
                    orderData.setBrokerId(brokerId);
                    orderData.setPrice(price);
                    orderData.setOrderType(orderTypeData);
                    orderData.setTID(order.getClientOrderId());
                    orderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                    orderData.setSymbol(order.getInstrument());
                    orderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                    orderData.setOrderCode(order.getOrderCode());
                    orderData.setOrderId(order.getOrderId());
                    orderData.setVersion(order.getVersion());
                    yield orderData;
                }
            }
            case CANCELED -> {
                if ("OPEN".equals(orderLeg.getPositionEffect())) {
                    DxCanceledOrderData canceledOrderData = new DxCanceledOrderData();
                    canceledOrderData.setPrice(orderLeg.getPrice());
                    canceledOrderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                    canceledOrderData.setOrderCode(order.getOrderCode());
                    canceledOrderData.setOrderId(order.getOrderId());
                    canceledOrderData.setTicket(positionId);
                    canceledOrderData.setTID(order.getClientOrderId());
                    canceledOrderData.setSymbol(order.getInstrument());
                    canceledOrderData.setOrderType(orderTypeData);
                    canceledOrderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                    canceledOrderData.setVersion(order.getVersion());
                    yield canceledOrderData;
                } else {
                    DxConditionOrderData dxConditionOrderData = new DxConditionOrderData();
                    dxConditionOrderData.setPrice(BigDecimal.ZERO);
                    dxConditionOrderData.setType(type);
                    dxConditionOrderData.setTicket(positionId);
                    dxConditionOrderData.setOrderCode(order.getOrderCode());
                    dxConditionOrderData.setTID(order.getClientOrderId());
                    dxConditionOrderData.setVersion(order.getVersion());
                    yield dxConditionOrderData;
                }
            }
            case REJECTED -> {
                DxRejectOrderData rejectOrderData = new DxRejectOrderData();
                rejectOrderData.setTicket(positionId);
                rejectOrderData.setPrice(orderLeg.getPrice());
                rejectOrderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                rejectOrderData.setTID(order.getClientOrderId());
                rejectOrderData.setSymbol(order.getInstrument());
                rejectOrderData.setOrderType(orderTypeData);
                rejectOrderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                rejectOrderData.setVersion(order.getVersion());
                try {
                    var execution = order.getExecutions();
                    if (execution != null && !execution.isEmpty()) {
                        var exec = execution.getFirst();
                        rejectOrderData.setReason(exec.getRejectReason());
                    }
                } catch (Exception ignored) {
                }
                yield rejectOrderData;
            }
            case EXPIRED -> {
                DxCanceledOrderData canceledOrderData = new DxCanceledOrderData();
                canceledOrderData.setPrice(orderLeg.getPrice());
                canceledOrderData.setTime(order.getTransactionTime().toEpochSecond() * 1000);
                canceledOrderData.setOrderCode(order.getOrderCode());
                canceledOrderData.setOrderId(order.getOrderId());
                canceledOrderData.setTicket(positionId);
                canceledOrderData.setTID(order.getClientOrderId());
                canceledOrderData.setSymbol(order.getInstrument());
                canceledOrderData.setOrderType(orderTypeData);
                canceledOrderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                canceledOrderData.setVersion(order.getVersion());
                yield canceledOrderData;
            }
            default -> null;
        };
        return dxOrder;
    }

    DxOrderData parsePosition(Long brokerId, Object accountId, Position position, InstrumentService
            instrumentService) {
        try {
            var positionCode = position.getPositionCode();
            Long ticketId = Long.valueOf(positionCode);
            DxOrderData orderData = new DxOrderData();
            orderData.setAccountId(accountId);
            orderData.setTicket(ticketId);
            Types.Side side = position.getSide();
            orderData.setOrderType(Types.Side.BUY.equals(side) ? OrderTypeData.Buy : OrderTypeData.Sell);
            orderData.setBrokerId(brokerId);
            orderData.setPrice(position.getOpenPrice());
            orderData.setTime(position.getOpenTime().toEpochSecond() * 1000);
            orderData.setSymbol(position.getSymbol());
            ContractData contractData = instrumentService.getInstruments().get(position.getSymbol());
            var quantity = position.getQuantity();
            if (quantity == null || quantity.equals(BigDecimal.ZERO)) {
                orderData.setLot(BigDecimal.ZERO);
            } else {
                orderData.setLot(position.getQuantity().divide(contractData.getContractSize()));
            }
            orderData.setVersion(position.getVersion());
            return orderData;
        } catch (Exception e) {
            log.error("Error while processing position {}", position, e);
        }
        return null;

    }


    public static Map<Long, DxOrderData> parsePositions(Long brokerId, Object accountId, InstrumentService
            instrumentService, List<Position> positionList) {
        final Map<Long, DxOrderData> orders = new ConcurrentHashMap<>();
        positionList.forEach(position -> {
            var orderData = parsePosition(brokerId, accountId, position, instrumentService);
            if (orderData != null)
                orders.put(orderData.getTicket(), orderData);

        });
        return orders;
    }

    public static void populateOrdersWithCondition
            (Map<Long, DxConditionOrderData> conditions, Map<Long, DxOrderData> orders) {
        orders.forEach((ticketId, orderData) ->
                populateOrderWithCondition(conditions, orderData));
    }

    public static void populateOrderWithCondition(Map<Long, DxConditionOrderData> conditions, DxOrderData
            dxOrderData) {
        var condition = conditions.get(dxOrderData.getTicket());
        if (condition != null) {
            if (Types.Type.STOP.equals(condition.getType())) {
                dxOrderData.setSl(condition.getPrice());
                dxOrderData.setSlOrderId(condition.getTicket());
                dxOrderData.setSlTID(String.valueOf(condition.getTID()));
                dxOrderData.setSlOrderCode(condition.getOrderCode());
                dxOrderData.setSlVersion(condition.getVersion());
            } else {
                dxOrderData.setTp(condition.getPrice());
                dxOrderData.setTpOrderId(condition.getTicket());
                dxOrderData.setTpTID(String.valueOf(condition.getTID()));
                dxOrderData.setTpOrderCode(condition.getOrderCode());
                dxOrderData.setTpVersion(condition.getVersion());
            }
            log.info("Order: {}", dxOrderData);
        }
    }

    private static OrderTypeData getOrderTypeData(Types.Side side, Types.Type type) {
        OrderTypeData orderTypeData;
        if (Types.Side.BUY.equals(side)) {
            if (Types.Type.LIMIT.equals(type)) {
                orderTypeData = OrderTypeData.BuyLimit;
            } else if (Types.Type.STOP.equals(type)) {
                orderTypeData = OrderTypeData.BuyStop;
            } else {
                orderTypeData = OrderTypeData.Buy;
            }
        } else {
            if (Types.Type.LIMIT.equals(type)) {
                orderTypeData = OrderTypeData.SellLimit;
            } else if (Types.Type.STOP.equals(type)) {
                orderTypeData = OrderTypeData.SellStop;
            } else {
                orderTypeData = OrderTypeData.Sell;
            }
        }
        return orderTypeData;
    }
}
