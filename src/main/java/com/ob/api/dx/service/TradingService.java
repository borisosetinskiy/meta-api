package com.ob.api.dx.service;

import com.google.common.cache.Cache;
import com.ob.api.dx.TradeDxApi;
import com.ob.api.dx.common.DelayedRunnableProcessor;
import com.ob.api.dx.model.data.*;
import com.ob.api.dx.model.request.GroupOrders;
import com.ob.api.dx.model.request.PlaceOrder;
import com.ob.api.dx.model.request.inner.DxPendingRequest;
import com.ob.api.dx.model.request.inner.DxPositionRequest;
import com.ob.api.dx.model.request.inner.DxSetStopLossRequest;
import com.ob.api.dx.model.request.inner.DxSetTakeProfitRequest;
import com.ob.api.dx.model.request.message.AccountPortfoliosSubscriptionRequest;
import com.ob.api.dx.model.request.message.ServerEvent;
import com.ob.api.dx.model.response.GroupOrderExecutionResponse;
import com.ob.api.dx.model.response.OrderExecutionResponse;
import com.ob.api.dx.model.response.OrdersListResponse;
import com.ob.api.dx.model.response.PositionListResponse;
import com.ob.api.dx.util.ConnectionUtil;
import com.ob.api.dx.util.HttpUtil;
import com.ob.api.dx.util.TradeUtil;
import com.ob.broker.common.IBaseApi;
import com.ob.broker.common.JsonService;
import com.ob.broker.common.MetricService;
import com.ob.broker.common.client.rest.ResponseMessage;
import com.ob.broker.common.client.ws.ChannelClient;
import com.ob.broker.common.client.ws.ChannelListener;
import com.ob.broker.common.client.ws.ExternalWSClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.*;
import com.ob.broker.common.model.*;
import com.ob.broker.common.request.*;
import com.ob.broker.service.LocalEntryService;
import com.ob.broker.util.ErrorUtil;
import com.ob.broker.util.Util;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Data
@ToString(of = {"requestDelay"})
public class TradingService implements ChannelListener {
    final Integer requestDelay;
    final TradeDxApi tradeDxApi;
    final LocalEntryService localEntryService;
    final MetricService metricService;
    final Cache<String, OrderRequest> orderRequests;
    final Map<Long, DxOrderData> opened = new ConcurrentHashMap<>();
    final Map<Long, DxOrderData> pending = new ConcurrentHashMap<>();
    final Map<Long, DxConditionOrderData> sl = new ConcurrentHashMap<>();
    final Map<Long, DxConditionOrderData> tp = new ConcurrentHashMap<>();
    final Map<Object, DxConditionOrderData> lazySl = new ConcurrentHashMap<>();
    final Map<Object, DxConditionOrderData> lazyTp = new ConcurrentHashMap<>();
    final Map<Long, CloseOrderData> closedOrders = new ConcurrentHashMap<>();
    final AccountData accountData = new AccountData();
    final ChannelClient tradingChannel;
    final AtomicLong requests = new AtomicLong();
    final AtomicInteger count = new AtomicInteger();
    private Instant lastResetTime = Instant.now();
    DelayedRunnableProcessor delayedRunnableProcessor;
    OrderHandler orderHandler;

    public TradingService(Integer requestDelay, TradeDxApi tradeDxApi, LocalEntryService localEntryService, MetricService metricService, Cache<String, OrderRequest> orderRequests) {
        this.requestDelay = requestDelay;
        this.tradeDxApi = tradeDxApi;
        this.localEntryService = localEntryService;
        this.metricService = metricService;
        this.orderRequests = orderRequests;
        try {
            URI uri = new URI(getTradingUrl());
            this.tradingChannel = new ExternalWSClient(uri, this.getClass().getSimpleName() + ":" + this.getBrokerId() + "_" + this.getAccountId(), this);
        } catch (Exception e) {
            throw new CodeException(e, Code.INVALID_NETWORK_CONFIG);
        }
        delayedRunnableProcessor = new DelayedRunnableProcessor("TradingService"
                , requestDelay, getAccountId()
                , tradeDxApi.getExecutorManager().event()
                , metricService);
        orderHandler = new OrderHandler(tradeDxApi);

        tradeDxApi.addListener(EventTopic.ORDER, new EventConsumer() {
            @Override
            public void onNext(EventTopic topic, IBaseApi api, Event event) {
                if (event.getEventType().equals(EventType.OPEN_ORDER) && event instanceof OrderEvent orderEvent) {
                    var order = (DxOrderData) orderEvent.getOrder();
                    if (order != null) {
                        var tid = order.getTID();
                        if (tid != null) {
                            var slCondition = lazySl.get(tid);
                            if (slCondition != null) {
                                DxSetStopLossRequest stopLossRequest = new DxSetStopLossRequest();
                                stopLossRequest.setTicket(order.getTicket());
                                stopLossRequest.setPrice(slCondition.getPrice());
                                stopLossRequest.setAccountId(getAccountId());
                                stopLossRequest.setRequestType(RequestType.SET_SL);
                                stopLossRequest.setBrokerId(getBrokerId());
                                addToProcess(stopLossRequest);
                                lazySl.remove(tid);
                            }
                            var tpCondition = lazyTp.get(tid);
                            if (tpCondition != null) {
                                DxSetTakeProfitRequest takeProfitRequest = new DxSetTakeProfitRequest();
                                takeProfitRequest.setTicket(order.getTicket());
                                takeProfitRequest.setPrice(tpCondition.getPrice());
                                takeProfitRequest.setAccountId(getAccountId());
                                takeProfitRequest.setRequestType(RequestType.SET_TP);
                                takeProfitRequest.setBrokerId(getBrokerId());
                                addToProcess(takeProfitRequest);
                                lazyTp.remove(tid);
                            }
                        }
                    }
                }
            }

            @Override
            public String getId() {
                return "order_handler-"+getBrokerId()+"-"+getAccountId();
            }
        });
    }

    public void execute(IRequest request) {
        if (request instanceof OrderRequest orderRequest) {
            Object id = orderRequest.getTID();
            if (id == null) {
                id = orderRequest.getTicket();
            }
            orderRequests.put(String.valueOf(id), orderRequest);
            if (orderRequest.isLocal()) {
                metricService.increment("order_request_local_init", "type", orderRequest.getRequestType().name()
                        , "symbol", Util.toString(orderRequest.getSymbol()));
                processRequest(orderRequest);
            } else {
                metricService.increment("order_request_init"
                        , "type", orderRequest.getRequestType().name()
                        , "symbol", Util.toString(orderRequest.getSymbol()));
                addToProcess(orderRequest);
            }
        } else if (request instanceof DxPendingRequest dxPendingRequest) {
            addToProcess(dxPendingRequest);
            // delayedProcessor.add(dxPendingRequest.getRequestType().getIntValue(), dxPendingRequest);
        } else if (request instanceof DxPositionRequest dxPositionRequest) {
            //  delayedProcessor.add(dxPositionRequest.getRequestType().getIntValue(), dxPositionRequest);
            addToProcess(dxPositionRequest);
        } else if (request instanceof DataRequest dataRequest) {
            processRequest(dataRequest);
        }else if (request instanceof GroupOrderRequest groupOrderRequest) {
            processRequest(groupOrderRequest);
        }
    }

    void addToProcess(IRequest request) {
        delayedRunnableProcessor.add(() -> {
            try {
                if (request instanceof DxPendingRequest) {
                    requestOrders();
                } else if (request instanceof DxPositionRequest) {
                    requestPositions();
                } else {
                    processRequest(request);
                }
            } catch (Exception e) {
                errorNotify(request, e);
            }
        });
    }

    public void start() {
//        delayedProcessor.start();
        delayedRunnableProcessor.start();
        tradingChannel.connect();
    }


    public void stop() {
        delayedRunnableProcessor.stop();
        try {
            tradingChannel.close();
        } catch (Exception ignored) {
        }
    }

    public void clear() {
        delayedRunnableProcessor.clear();
        try {
            tradingChannel.clear();
        } catch (Exception ignored) {
        }
    }


    private void errorNotify(IRequest request, Exception e) {
        if (request instanceof IOrderRequest orderRequest) {
            log.error("Error processing request {}", orderRequest, e);
            metricService.increment("command_request_error",
                    "type", orderRequest.getRequestType().name(),
                    "brokerId", String.valueOf(getBrokerId()),
                    "accountId", String.valueOf(getAccountId()));

            var error = ErrorUtil.toError(e);
            var event = new OrderErrorEvent(request.getBrokerId(), request.getAccountId()
                    , request.getTID(), error
                    , EventType.REJECT_ORDER
                    , request
                    , null);

            tradeDxApi.getEventProducer().eventConsumer(EventTopic.ORDER_REQUEST)
                    .accept(tradeDxApi, event);
        }
    }

    void processRequest(IRequest request) {
        try {
            if (!ConnectionUtil.isConnected(tradeDxApi.getConnectionStatus())) {
                throw new CodeException("Connection is not established", Code.NO_CONNECTION);
            }
            if (request instanceof OrderRequest orderRequest) {
                switch (orderRequest.getRequestType()) {
                    case CLOSE -> {
                        if (orderRequest.getTicket() == null)
                            throw new CodeException("TicketId was not provided", Code.INVALID_PARAM);
                        var order = getOrder(orderRequest.getTicket(), opened());
                        closeOrder(order, orderRequest);
                    }
                    case CLOSE_SYMBOL -> closeOrders(opened().values().stream()
                            .filter(order -> order.getSymbol().equals(orderRequest.getSymbol()))
                            .toList());
                    case CLOSE_ALL -> {
                        closeOrders(opened().values()
                                .stream().toList());
                    }
                    case CLOSE_TYPE -> closeOrders(opened().values().stream()
                            .filter(order ->
                                    (order.getOrderType().equals(OrderTypeData.Buy))
                                    || (order.getOrderType().equals(OrderTypeData.Sell))
                                       && order.getOrderType() == (orderRequest.getType())
                            ).toList());
                    case OPEN -> {
                        var type = orderRequest.getType();
                        if (!OrderTypeData.Buy.equals(type) && !OrderTypeData.Sell.equals(type))
                            throw new CodeException(Code.INVALID_PARAM);
                        if (orderRequest.getSymbol() == null)
                            throw new CodeException("Symbol was not provided", Code.INVALID_PARAM);
                        marketOrder(orderRequest);
                    }
                    case UPDATE -> {
                        if (orderRequest.getTicket() == null)
                            throw new CodeException("TicketId was not provided", Code.INVALID_PARAM);
                        var order = getOrder(orderRequest.getTicket(), opened());
                        update(order, orderRequest);
                    }
                    case CANCEL_PENDING -> {
                        if (orderRequest.getTicket() != null) {
                            var order = getOrder(orderRequest.getTicket(), pending());
                            cancel(order.getOrderCode());
                        } else if (orderRequest.getTID() != null) {
                            cancel(String.valueOf(orderRequest.getTID()));
                        }

                    }
                    case PENDING -> pending(orderRequest);
                    case UPDATE_PENDING -> {
                        if (orderRequest.getTicket() == null)
                            throw new CodeException("TicketId was not provided", Code.INVALID_PARAM);
                        var order = getOrder(orderRequest.getTicket(), pending());
                        updatePending(order, orderRequest);
                    }
                    case CANCEL_LOCAL_PENDING -> localEntryService.delete(orderRequest.getTicket());
                    case LOCAL_PENDING -> localEntryService.create(orderRequest);
                    case UPDATE_LOCAL_PENDING -> localEntryService.update(orderRequest);
                    case LOAD_LOCAL_PENDING -> localEntryService.addOrder(orderRequest);
                    case LOAD_CLOSE -> loadClose(orderRequest);
                    default -> throw new CodeException(Code.INVALID_PARAM);
                }

            } else if (request instanceof GroupOrderRequest groupOrderRequest) {
                switch (groupOrderRequest.getRequestType()) {
                    case CLOSE_GROUP -> {
                        var orders = groupOrderRequest.getTickets().stream()
                                .map(ticket -> {
                                    try {
                                        return getOrder(ticket, opened());
                                    } catch (Exception e) {
                                        OrderRequest closeRequest = new OrderRequest();
                                        closeRequest.setTime(request.getTime());
                                        closeRequest.setTID(request.getTID());
                                        closeRequest.setRequestType(RequestType.CLOSE);
                                        closeRequest.setTicket(ticket);
                                        errorNotify(closeRequest, e);
                                    }
                                    return null;
                                }).filter(Objects::nonNull)
                                .toList();
                        closeOrders(orders);
                    }
                    case CANCEL_GROUP -> {
                        var orders = groupOrderRequest.getTickets().stream()
                                .map(ticket -> {
                                    try {
                                        return getOrder(ticket, pending());
                                    } catch (Exception e) {
                                        OrderRequest closeRequest = new OrderRequest();
                                        closeRequest.setTime(request.getTime());
                                        closeRequest.setTID(request.getTID());
                                        closeRequest.setRequestType(RequestType.CANCEL_PENDING);
                                        closeRequest.setTicket(ticket);
                                        errorNotify(closeRequest, e);
                                    }
                                    return null;
                                }).filter(Objects::nonNull)
                                .toList();
                        cancelOrders(orders);
                    }
                    case CANCEL_LOCAL_GROUP -> {
                        groupOrderRequest.getTickets().forEach(localEntryService::delete);
                    }
                }

            } else if (request instanceof IOrderRequest orderRequest) {
                switch (orderRequest.getRequestType()) {
                    case SET_TP -> {
                        var order = getOrder(orderRequest.getTicket(), opened());
                        processCondition(order, orderRequest.getPrice(), Types.Type.LIMIT, orderRequest);
                    }
                    case SET_SL -> {
                        var order = getOrder(orderRequest.getTicket(), opened());
                        processCondition(order, orderRequest.getPrice(), Types.Type.STOP, orderRequest);
                    }
                }
            }else if(request instanceof DataRequest dataRequest){
                switch (dataRequest.getRequestType()){
                    case LOAD_PENDING -> requestOrders();
                    case LOAD_OPEN -> requestPositions();
                    case LOAD_HISTORY -> loadHistory();
                }
            }


        } catch (Exception e) {
            errorNotify(request, e);
            if (e instanceof CodeException codeException) {
                if (codeException.getCode() == Code.UNAUTHORIZED.getValue()) {
                    GeneralErrorEvent generalErrorEvent = new GeneralErrorEvent();
                    generalErrorEvent.setAccountId(getAccountId());
                    generalErrorEvent.setBrokerId(getBrokerId());
                    generalErrorEvent.setError(codeException);
                    generalErrorEvent.setEventType(EventType.DISCONNECT);
                    tradeDxApi.getEventProducer().eventConsumer(EventTopic.CONNECT)
                            .accept(tradeDxApi, generalErrorEvent);
                }
            }
        }
    }

    void marketOrder(OrderRequest orderRequest) {
        postOrder(orderRequest.getSymbol()
                , String.valueOf(orderRequest.getTID())
                , orderRequest.getLot()
                , orderRequest.getType() == OrderTypeData.Buy ? Types.Side.BUY : Types.Side.SELL
                , null
                , Types.PositionEffect.OPEN
                , Types.Type.MARKET
                , null
                , orderRequest);
        String tid = String.valueOf(orderRequest.getTID());
        if (Util.isMoreThanZero(orderRequest.getTp())) {
            DxConditionOrderData tpCondition = new DxConditionOrderData();
            tpCondition.setOrderCode(tid);
            tpCondition.setPrice(orderRequest.getTp());
            tpCondition.setType(Types.Type.LIMIT);
            tpCondition.setSymbol(orderRequest.getSymbol());
            lazyTp.put(tid, tpCondition);
        }
        if (Util.isMoreThanZero(orderRequest.getSl())) {
            DxConditionOrderData slCondition = new DxConditionOrderData();
            slCondition.setOrderCode(tid);
            slCondition.setPrice(orderRequest.getSl());
            slCondition.setType(Types.Type.STOP);
            slCondition.setSymbol(orderRequest.getSymbol());
            lazySl.put(tid, slCondition);
        }

    }


    PlaceOrder newPlaceOrder(String symbol
            , String TID
            , BigDecimal lot
            , Types.Side side
            , Types.Type type) {
        ContractData contractData = getTradeDxApi().getInstrumentService()
                .getInstruments().get(symbol);
        BigDecimal amount = BigDecimal.ZERO;
        if (lot != null) {
            amount = lot.multiply(contractData.getContractSize());
        }
        PlaceOrder placeOrder = new PlaceOrder();
        placeOrder.setAccount(tradeDxApi.getUserSession().getAccountCode());
        placeOrder.setOrderCode(TID);
        if (type != null) {
            placeOrder.setType(type);
        }
        placeOrder.setInstrument(symbol);
        placeOrder.setQuantity(amount.intValue());
        placeOrder.setTif(Types.Tif.GTC);
        placeOrder.setSide(side);
        return placeOrder;
    }

    void postOrder(String symbol
            , String TID
            , BigDecimal lot
            , Types.Side side
            , String positionCode
            , Types.PositionEffect positionEffect
            , Types.Type type
            , BigDecimal price
            , IOrderRequest orderRequest) {
        PlaceOrder placeOrder = newPlaceOrder(symbol, TID, lot, side, type);
        if (type == Types.Type.LIMIT) {
            placeOrder.setLimitPrice(price);
        } else if (type == Types.Type.STOP) {
            placeOrder.setStopPrice(price);
        }
        if (StringUtils.hasText(positionCode))
            placeOrder.setPositionCode(positionCode);
        placeOrder.setPositionEffect(positionEffect);
        postOrder(placeOrder, type, OrderExecutionResponse.class, orderRequest);
    }

    void putOrder(String symbol
            , String TID
            , BigDecimal lot
            , Types.Side side
            , Types.Type type
            , String positionCode
            , Types.PositionEffect positionEffect
            , BigDecimal price
            , IOrderRequest orderRequest) {
        PlaceOrder placeOrder = newPlaceOrder(symbol, TID, lot, side, null);
        if (type == Types.Type.LIMIT) {
            placeOrder.setLimitPrice(price);
        } else if (type == Types.Type.STOP) {
            placeOrder.setStopPrice(price);
        }
        if (StringUtils.hasText(positionCode))
            placeOrder.setPositionCode(positionCode);

        placeOrder.setPositionEffect(positionEffect);
        putOrder(placeOrder, type, OrderExecutionResponse.class, orderRequest);
    }

    void postOrder(Object payload, Types.Type type, Class<?> responseClass, IOrderRequest orderRequest) {
        final String body = JsonService.JSON.toJson(payload);
        var url = getOrderUrl();
        HttpUtil.post(url, getTradeDxApi().getUserSession()
                , getTradeDxApi().getRestApiClient()
                , body
                , responseMessage -> {
                    parseResponse(responseMessage, type, responseClass, orderRequest);
                });
    }


    void putOrder(Object payload, Types.Type type, Class<?> responseClass, IOrderRequest orderRequest) {
        final String body = JsonService.JSON.toJson(payload);
        var url = getOrderUrl();
        HttpUtil.put(url, getTradeDxApi().getUserSession()
                , getTradeDxApi().getRestApiClient()
                , body
                , responseMessage -> {
                    parseResponse(responseMessage, type, responseClass, orderRequest);
                });
    }

    void parseResponse(ResponseMessage responseMessage, Types.Type type, Class<?> responseClass, IOrderRequest orderRequest) {
        var response = JsonService.JSON.fromJson(responseMessage.message(), responseClass);
        if (response != null) {
            log.info("Request result {}", response);
            final RequestEvent requestEvent = new RequestEvent();
            requestEvent.setOrderRequest(orderRequest);
            requestEvent.setAccountId(getAccountId());
            requestEvent.setBrokerId(getBrokerId());
            if (type == Types.Type.LIMIT || type == Types.Type.STOP)
                requestEvent.setType(RequestEvent.Type.ORDER);
            else if (type == Types.Type.MARKET)
                requestEvent.setType(RequestEvent.Type.TRADE);
            requestEvent.setResponse(response);
            tradeDxApi.getEventProducer().eventConsumer(EventTopic.REQUEST)
                    .accept(tradeDxApi, requestEvent);
        }
    }

    void pending(OrderRequest orderRequest) {
        Types.Type type = orderRequest.isLimit()
                ? Types.Type.LIMIT : Types.Type.STOP;
        if (Util.isMoreThanZero(orderRequest.getSl()) || Util.isMoreThanZero(orderRequest.getTp())) {
            Types.Side side = orderRequest.isBuy()
                    ? Types.Side.BUY : Types.Side.SELL;
            var groupOrders = groupPending(String.valueOf(orderRequest.getTID())
                    , type
                    , side
                    , orderRequest.getPrice()
                    , orderRequest.getSymbol()
                    , orderRequest.getLot()
                    , generateUniqueCode()
                    , generateUniqueCode()
                    , orderRequest.getTp()
                    , orderRequest.getSl());
            postOrder(groupOrders, type, GroupOrderExecutionResponse.class, orderRequest);

        } else {
            postOrder(orderRequest.getSymbol()
                    , String.valueOf(orderRequest.getTID())
                    , orderRequest.getLot()
                    , orderRequest.isBuy()
                            ? Types.Side.BUY : Types.Side.SELL
                    , null
                    , Types.PositionEffect.OPEN
                    , type
                    , orderRequest.getPrice()
                    , orderRequest
            );
        }
    }

    void loadClose(OrderRequest orderRequest) {
        var url = getHistoryUrl(orderRequest.getTicket());
        HttpUtil.get(url, getTradeDxApi().getUserSession()
                , getTradeDxApi().getRestApiClient()
                , responseMessage -> {
                    OrderExecutionResponse orderExecutionResponse = JsonService.JSON.fromJson(responseMessage.message(), OrderExecutionResponse.class);
                    if (orderExecutionResponse != null) {
                        log.info("Order result {}", orderExecutionResponse);
                        final RequestEvent requestEvent = new RequestEvent();
                        requestEvent.setAccountId(getAccountId());
                        requestEvent.setBrokerId(getBrokerId());
                        requestEvent.setType(RequestEvent.Type.ORDER);
                        requestEvent.setResponse(orderExecutionResponse);
                        tradeDxApi.getEventProducer().eventConsumer(EventTopic.REQUEST)
                                .accept(tradeDxApi, requestEvent);
                    }
                });
    }


    void closeOrder(DxOrderData dxOrderData, OrderRequest orderRequest) {
        postOrder(dxOrderData.getSymbol()
                , (orderRequest.getTID() + ":" + System.currentTimeMillis())
                , orderRequest.getLot() == null ? dxOrderData.getLot() : orderRequest.getLot()
                , dxOrderData.getOrderType() == OrderTypeData.Buy ? Types.Side.SELL : Types.Side.BUY
                , dxOrderData.getTicket().toString()
                , Types.PositionEffect.CLOSE
                , Types.Type.MARKET
                , null
                , orderRequest);
    }

    void closeOrders(List<DxOrderData> dxOrderDataList) {
        dxOrderDataList.forEach(order ->
                {
                    OrderRequest closeRequest = new OrderRequest();
                    closeRequest.setRequestType(RequestType.CLOSE);
                    closeRequest.setTID(System.currentTimeMillis());
                    closeRequest.setTicket(order.getTicket());
                    addToProcess(closeRequest);
                }
        );
    }

    void cancelOrders(List<DxOrderData> dxOrderDataList) {
        dxOrderDataList.forEach(order ->
                {
                    OrderRequest cancelOrder = new OrderRequest();
                    cancelOrder.setRequestType(RequestType.CANCEL_PENDING);
                    cancelOrder.setTID(System.currentTimeMillis());
                    cancelOrder.setTicket(order.getTicket());
                    addToProcess(cancelOrder);
                }
        );
    }


    void processCondition(DxOrderData dxOrderData, BigDecimal price, Types.Type type, IOrderRequest orderRequest) {
        if (Types.Type.LIMIT == type) {
            if (dxOrderData.getTpOrderId() != null) {
                updateCondition(dxOrderData, price, type, orderRequest);
            } else {
                setCondition(dxOrderData, price, type, orderRequest);
            }
        } else if (Types.Type.STOP == type) {
            if (dxOrderData.getSlOrderId() != null) {
                updateCondition(dxOrderData, price, type, orderRequest);
            } else {
                setCondition(dxOrderData, price, type, orderRequest);
            }
        } else {
            throw new CodeException("Invalid type", Code.INVALID_PARAM);
        }
    }

    void setCondition(DxOrderData dxOrderData, BigDecimal price, Types.Type type, IOrderRequest orderRequest) {
        postOrder(dxOrderData.getSymbol()
                , String.valueOf(System.currentTimeMillis())
                , dxOrderData.getLot()
                , dxOrderData.getOrderType() == OrderTypeData.Sell
                        ? Types.Side.BUY : Types.Side.SELL
                , String.valueOf(dxOrderData.getTicket())
                , Types.PositionEffect.CLOSE
                , type
                , price
                , orderRequest
        );
    }

    void updateCondition(DxOrderData dxOrderData, BigDecimal price, Types.Type type, IOrderRequest orderRequest) {
        String id;
        if (Types.Type.LIMIT == type) {
            id = dxOrderData.getTpTID();
        } else if (Types.Type.STOP == type) {
            id = dxOrderData.getSlTID();
        } else {
            throw new CodeException("Invalid type", Code.INVALID_PARAM);
        }
        putOrder(dxOrderData.getSymbol()
                , id
                , dxOrderData.getLot()
                , TradeUtil.side(dxOrderData.getOrderType())
                , type
                , String.valueOf(dxOrderData.getTicket())
                , TradeUtil.positionEffect(dxOrderData.getOrderType())
                , price
                , orderRequest
        );
    }

    private RequestType toRequestTypeForSlAndTp(OrderRequest orderRequest, boolean tp) {
        switch (orderRequest.getRequestType()) {
            case UPDATE -> {
                if (tp) {
                    return RequestType.SET_TP;
                } else {
                    return RequestType.SET_SL;
                }
            }
            case UPDATE_PENDING -> {
                if (tp) {
                    return RequestType.SET_TP_PENDING;
                } else {
                    return RequestType.SET_SL_PENDING;
                }
            }
            default -> throw new CodeException("Invalid request type", Code.INVALID_PARAM);
        }
    }

    void cancelRequest(DxOrderData dxOrderData, boolean tp) {
        OrderRequest cancelRequest = new OrderRequest();
        cancelRequest.setRequestType(RequestType.CANCEL_PENDING);
        cancelRequest.setTID(tp ? dxOrderData.getTpOrderCode() : dxOrderData.getSlOrderCode());
        cancelRequest.setComment("Cancel take profit order");
        addToProcess(cancelRequest);
    }


    void cancelOrderIfNotNull(DxOrderData dxOrderData) {
        if (dxOrderData.getTpOrderCode() != null) {
            cancelRequest(dxOrderData, true);
        }
        if (dxOrderData.getSlOrderCode() != null) {
            cancelRequest(dxOrderData, false);
        }
    }

    GroupOrders groupPending(DxOrderData dxOrderData, OrderRequest orderRequest) {
        Types.Type type = TradeUtil.type(dxOrderData.getOrderType());
        Types.Side side = TradeUtil.side(dxOrderData.getOrderType());
        BigDecimal price = orderRequest.getPrice() != null
                ? orderRequest.getPrice() : dxOrderData.getPrice();
        return groupPending(dxOrderData.getOrderCode(), type, side, price
                , dxOrderData.getSymbol()
                , dxOrderData.getLot()
                , dxOrderData.getTpOrderCode(), dxOrderData.getSlOrderCode()
                , orderRequest.getTp()
                , orderRequest.getSl());
    }

    GroupOrders groupPending(String orderCode
            , Types.Type type
            , Types.Side side
            , BigDecimal price
            , String symbol
            , BigDecimal lot
            , String tpCode
            , String slCode
            , BigDecimal tp, BigDecimal sl) {
        GroupOrders groupOrders = new GroupOrders();
        List<PlaceOrder> orders = new ArrayList<>();
        groupOrders.setOrders(orders);
        PlaceOrder pendingOrder = new PlaceOrder();
        pendingOrder.setAccount(tradeDxApi.getUserSession().getAccountCode());
        pendingOrder.setOrderCode(orderCode);
        pendingOrder.setPositionEffect(Types.PositionEffect.OPEN);
        pendingOrder.setType(type);
        pendingOrder.setSide(side);
        pendingOrder.setTif(Types.Tif.GTC);
        if (Types.Type.STOP.equals(type)) {
            pendingOrder.setStopPrice(price);
        } else {
            pendingOrder.setLimitPrice(price);
        }
        pendingOrder.setInstrument(symbol);
        ContractData contractData = tradeDxApi.getInstrumentService().getInstruments().get(symbol);
        pendingOrder.setQuantity(lot.multiply(contractData.getContractSize()).intValue());

        orders.add(pendingOrder);
        Types.Side conditionSide = side == Types.Side.BUY ? Types.Side.SELL : Types.Side.BUY;

        if (Util.isMoreThanZero(tp) && !Util.isMoreThanZero(sl)) {
            if (tpCode == null && slCode != null) {
                tpCode = slCode;
            }
        } else if (Util.isMoreThanZero(sl) && !Util.isMoreThanZero(tp)) {
            if (slCode == null && tpCode != null) {
                slCode = tpCode;
            }
        } else if (Util.isMoreThanZero(tp) && Util.isMoreThanZero(sl)) {
            if (tpCode == null) {
                tpCode = generateUniqueCode();
            }
            if (slCode == null) {
                slCode = generateUniqueCode();
            }
        }
        if (Util.isMoreThanZero(tp)) {
            PlaceOrder takeProfitOrder = new PlaceOrder();
            takeProfitOrder.setAccount(tradeDxApi.getUserSession().getAccountCode());
            takeProfitOrder.setOrderCode(tpCode);
            takeProfitOrder.setPositionEffect(Types.PositionEffect.CLOSE);
            takeProfitOrder.setType(Types.Type.LIMIT);
            takeProfitOrder.setSide(conditionSide);
            takeProfitOrder.setTif(Types.Tif.GTC);
            takeProfitOrder.setLimitPrice(tp);
            takeProfitOrder.setInstrument(symbol);
            takeProfitOrder.setQuantity(0);
            orders.add(takeProfitOrder);
        }
        if (Util.isMoreThanZero(sl)) {
            PlaceOrder stopLossOrder = new PlaceOrder();
            stopLossOrder.setAccount(tradeDxApi.getUserSession().getAccountCode());
            stopLossOrder.setOrderCode(slCode);
            stopLossOrder.setPositionEffect(Types.PositionEffect.CLOSE);
            stopLossOrder.setType(Types.Type.STOP);
            stopLossOrder.setSide(conditionSide);
            stopLossOrder.setTif(Types.Tif.GTC);
            stopLossOrder.setStopPrice(sl);
            stopLossOrder.setInstrument(symbol);
            stopLossOrder.setQuantity(0);
            orders.add(stopLossOrder);
        }
        return groupOrders;
    }

    String generateUniqueCode() {
        return String.valueOf(System.currentTimeMillis()) + increment();
    }

    Lock lock = new ReentrantLock();

    public int increment() {
        lock.lock();
        try {
            Instant now = Instant.now();
            long secondsSinceEpoch = now.getEpochSecond();
            if (secondsSinceEpoch % 5 == 0 && now.truncatedTo(ChronoUnit.SECONDS).equals(now)) {
                count.getAndSet(0);
                lastResetTime = now;
            }
            return count.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    void updatePending(DxOrderData dxOrderData, OrderRequest orderRequest) {
        boolean tp = Util.isMoreThanZero(orderRequest.getTp());
        boolean sl = Util.isMoreThanZero(orderRequest.getSl());
        if (tp || sl) {
            var groupOrders = groupPending(dxOrderData, orderRequest);
            Types.Type type = TradeUtil.type(dxOrderData.getOrderType());
            putOrder(groupOrders, type, GroupOrderExecutionResponse.class, orderRequest);
        } else {
            cancelOrderIfNotNull(dxOrderData);
        }
    }

    void update(DxOrderData dxOrderData, OrderRequest orderRequest) {
        if (Util.isMoreThanZero(orderRequest.getTp())) {
            if (dxOrderData.getTp() == null || dxOrderData.getTp().compareTo(orderRequest.getTp()) != 0) {
                DxSetTakeProfitRequest takeProfitRequest = new DxSetTakeProfitRequest();
                takeProfitRequest.setTicket(dxOrderData.getTicket());
                takeProfitRequest.setPrice(orderRequest.getTp());
                takeProfitRequest.setAccountId(getAccountId());
                takeProfitRequest.setRequestType(toRequestTypeForSlAndTp(orderRequest, true));
                takeProfitRequest.setBrokerId(getBrokerId());
                addToProcess(takeProfitRequest);
            }
        } else {
            if (dxOrderData.getTpOrderCode() != null) {
                OrderRequest cancelRequest = new OrderRequest();
                cancelRequest.setRequestType(RequestType.CANCEL_PENDING);
                cancelRequest.setTID(dxOrderData.getTpOrderCode());
                cancelRequest.setComment("Cancel take profit order");
                addToProcess(cancelRequest);
            }
        }
        if (Util.isMoreThanZero(orderRequest.getSl())) {
            if (dxOrderData.getSl() == null || dxOrderData.getSl().compareTo(orderRequest.getSl()) != 0) {
                DxSetStopLossRequest stopLossRequest = new DxSetStopLossRequest();
                stopLossRequest.setTicket(dxOrderData.getTicket());
                stopLossRequest.setPrice(orderRequest.getSl());
                stopLossRequest.setAccountId(getAccountId());
                stopLossRequest.setRequestType(toRequestTypeForSlAndTp(orderRequest, false));
                stopLossRequest.setBrokerId(getBrokerId());
                addToProcess(stopLossRequest);
            }
        } else {
            if (dxOrderData.getSlOrderCode() != null) {
                OrderRequest cancelRequest = new OrderRequest();
                cancelRequest.setRequestType(RequestType.CANCEL_PENDING);
                cancelRequest.setTID(dxOrderData.getSlOrderCode());
                cancelRequest.setComment("Cancel stop loss order");
                addToProcess(cancelRequest);
            }
        }
    }


    public void cancel(DxOrderData dxOrderData) {
        cancel(dxOrderData.getOrderCode());
    }

    void cancel(String orderCode) {
        var url = getOrderUrl() + "/" + orderCode;
        HttpUtil.delete(url, getTradeDxApi().getUserSession()
                , getTradeDxApi().getRestApiClient()
                , responseMessage -> {
                    OrderExecutionResponse orderExecutionResponse = JsonService.JSON.fromJson(responseMessage.message(), OrderExecutionResponse.class);
                    if (orderExecutionResponse != null) {
                        log.info("Order result {}", orderExecutionResponse);
                        final RequestEvent requestEvent = new RequestEvent();
                        requestEvent.setAccountId(getAccountId());
                        requestEvent.setBrokerId(getBrokerId());
                        requestEvent.setType(RequestEvent.Type.ORDER);
                        requestEvent.setResponse(orderExecutionResponse);
                        tradeDxApi.getEventProducer().eventConsumer(EventTopic.REQUEST)
                                .accept(tradeDxApi, requestEvent);
                    }
                });
    }

    private DxOrderData getOrder(Long ticket, Map<Long, DxOrderData> orders) {
        var order = orders.get(ticket);
        if (order == null)
            throw new CodeException(String.format("Can't find order %s", ticket), Code.NOT_FOUND);
        return order;
    }


    String getHistoryUrl(Long orderId) {
        return tradeDxApi.getUrlConfig().getRestApiUrl() + "/accounts/orders/history?accounts="
               + tradeDxApi.getUserSession().getAccountCode()
               + "&in-status=COMPLETED&with-order-id=" + orderId;
        //https://demo.dx.trade/dxsca-web/accounts/orders/history?accounts=default%3Am_1056663&in-status=COMPLETED&with-order-id=718109778
    }


    private String getTradingUrl() {
        return tradeDxApi.getUrlConfig().getMessageWebSocketUrl() + "/";
    }


    public void loadHistory() {
        try {
            var url = getHistoryOrderUrl();
            HttpUtil.get(url, tradeDxApi.getUserSession(), tradeDxApi.getRestApiClient(), responseMessage -> {
                OrdersListResponse ordersListResponse = JsonService.JSON.fromJson(responseMessage.message(), OrdersListResponse.class);
                parseHistory(ordersListResponse);
            });
        } catch (Exception e) {

        }

    }

    Map<String, Order> filter(List<Order> orders, String status, Function<Order, String> keyMapper) {
        return orders.stream()
                .filter(order -> order.getLegCount() > 0 && status.equals(order.getLegs().getFirst().getPositionEffect()))
                .collect(Collectors.toMap(keyMapper, order -> order));
    }

    private void parseHistory(OrdersListResponse ordersListResponse) {
        var orders = ordersListResponse.getOrders();
        final Map<String, Order> open = filter(orders, "OPEN", order -> String.valueOf(order.getOrderId()));
        final Map<String, Order> close = filter(orders, "CLOSE", order -> order.getLegs().getFirst().getPositionCode());

        close.forEach((s, closeOrder) -> {
            var orderLeg = closeOrder.getLegs().getFirst();
            String positionId = orderLeg.getPositionCode();
            try {
                ContractData contractData = getTradeDxApi().getInstrumentService().getInstruments().get(closeOrder.getInstrument());
                if (contractData == null) {
                    log.error("Contract data not found for symbol {} orderId {} accountId {} brokerId {}"
                            , closeOrder.getInstrument()
                            , closeOrder.getOrderId()
                            , getAccountId()
                            , getBrokerId());
                    return;
                }

                Long ticketId = Long.valueOf(positionId);

                CloseOrderData closeOrderData = new CloseOrderData();
                closeOrderData.setTicket(ticketId);
                closeOrderData.setSymbol(closeOrder.getInstrument());
                closeOrderData.setCloseTime(closeOrder.getTransactionTime().toEpochSecond() * 1000);
                closeOrderData.setClosePrice(orderLeg.getPrice());
                closeOrderData.setAccountId(getAccountId());
                closeOrderData.setBrokerId(getBrokerId());
                closeOrderData.setLot(orderLeg.getQuantity().divide(contractData.getContractSize()));
                closeOrderData.setOrderType(Types.Side.BUY.equals(closeOrder.getSide()) ? OrderTypeData.Buy : OrderTypeData.Sell);

                try {
                    var cashTransactions = closeOrder.getCashTransactions();
                    if (cashTransactions != null || !cashTransactions.isEmpty()) {
                        var cashTransaction = cashTransactions.getFirst();
                        closeOrderData.setProfit(cashTransaction.getValue());
                    }
                } catch (Exception e) {
                }

                var openOrder = open.get(positionId);
                var openLeg = openOrder.getLegs().getFirst();
                if (openLeg.getPrice() != null) {
                    closeOrderData.setOpenPrice(openLeg.getPrice());
                } else {
                    closeOrderData.setOpenPrice(openLeg.getAveragePrice());
                }
                closeOrderData.setOpenTime(openOrder.getTransactionTime().toEpochSecond() * 1000);
            } catch (Exception e) {
                log.error("Error while parsing order {} accountId {} brokerId {}"
                        , positionId
                        , getAccountId()
                        , getBrokerId()
                        , e);
            }

        });
    }

    private Object getAccountId() {
        return tradeDxApi.getAccountId();
    }

    private Long getBrokerId() {
        return tradeDxApi.getBrokerId();
    }


    public Map<Long, DxOrderData> pending() {
        return Map.copyOf(pending);
    }

    public Map<Long, DxOrderData> opened() {
        return Map.copyOf(opened);
    }

    public void loadAccount() {
        if (ConnectionUtil.isConnected(tradeDxApi.getUserSession().getConnectionStatus().get())) {
            try {
                var url = getAccountUrl();
                HttpUtil.get(url, tradeDxApi.getUserSession(), tradeDxApi.getRestApiClient(), responseMessage -> {
                    Portfolios portfoliosResponse = JsonService.JSON.fromJson(responseMessage.message(), Portfolios.class);
                    log.info("Load account {} brokerId {} result: {}", getAccountId()
                            , getBrokerId()
                            , responseMessage);
                    parse(portfoliosResponse);
                });
            } catch (Exception e) {
                log.error("Error while load account", e);
            }
        }
    }

    private void parse(Portfolios portfoliosResponse) {
        var portfolios = portfoliosResponse.getPortfolios();
        if (portfolios == null) {
            throw new CodeException("Can not get account data", Code.ACCOUNT_LOAD_ERROR);
        }
        Portfolio portfolio = portfolios.getFirst();
        if (portfolio == null) {
            throw new CodeException("Can not get account data", Code.ACCOUNT_LOAD_ERROR);
        }
        Balance balance = portfolio.getBalances().getFirst();
        setBalance(balance);
        processPositions(portfolio.getPositions());
        var dxOrders = OrderParserUtil.parseOrders(getBrokerId()
                , getAccountId()
                , tradeDxApi.getInstrumentService()
                , portfolio.getOrders());
        var sl = dxOrders.sl();
        var tp = dxOrders.tp();
        OrderParserUtil.populateOrdersWithCondition(sl, opened);
        OrderParserUtil.populateOrdersWithCondition(tp, opened);
        var orders = dxOrders.orders();
        OrderParserUtil.populateOrdersWithCondition(sl, orders);
        OrderParserUtil.populateOrdersWithCondition(tp, orders);
        pending.putAll(orders);
        pending.keySet().forEach(key -> {
            if (!orders.containsKey(key)) {
                pending.remove(key);
            }
        });
    }

    private void processPositions(List<Position> positionList) {
        var positions = OrderParserUtil.parsePositions(
                getBrokerId()
                , getAccountId()
                , tradeDxApi.getInstrumentService()
                , positionList);
        opened.putAll(positions);
        opened.keySet().forEach(key -> {
            if (!positions.containsKey(key)) {
                opened.remove(key);
            }
        });

    }


    private void handle(Portfolios portfoliosResponse) {
        var portfolios = portfoliosResponse.getPortfolios();
        if (portfolios == null) {
            log.warn("No portfolios found during handle");
            return;
        }
        Portfolio portfolio = portfolios.getFirst();
        if (portfolio == null) {
            log.warn("No portfolios found during handle");
            return;
        }
        handle(portfolio.getBalances().getFirst());
        orderHandler.handle(portfolio);
    }


    void setBalance(Balance balance) {
        accountData.setAccountId(getAccountId());
        accountData.setBrokerId(getBrokerId());
        accountData.setCurrency(balance.getCurrency());
        accountData.setBalance(balance.getValue());
    }

    void handle(Balance balance) {
        setBalance(balance);
        tradeDxApi.getEventProducer().eventConsumer(EventTopic.ACCOUNT_UPDATE)
                .accept(tradeDxApi, accountData);
    }


    private String getAccountUrl() {
        return tradeDxApi.getUrlConfig().getRestApiUrl() + "/accounts/"
               + tradeDxApi.getUserSession().getAccountCode()
               + "/portfolio";
    }

    private String getPositionUrl() {
        return tradeDxApi.getUrlConfig().getRestApiUrl() + "/accounts/"
               + tradeDxApi.getUserSession().getAccountCode() + "/positions";
    }

    private String getOrderUrl() {
        return tradeDxApi.getUrlConfig().getRestApiUrl() + "/accounts/"
               + tradeDxApi.getUserSession().getAccountCode() + "/orders";
    }


    private String getHistoryOrderUrl() {
        return tradeDxApi.getUrlConfig().getRestApiUrl() + "/accounts/"
               + tradeDxApi.getUserSession().getAccountCode() + "/orders/history?in-status=COMPLETED";
    }


    public void requestPositions() {
        if (ConnectionUtil.isConnected(tradeDxApi.getUserSession().getConnectionStatus().get())) {
            try {
                var url = getPositionUrl();
                HttpUtil.get(url, tradeDxApi.getUserSession(), tradeDxApi.getRestApiClient(), responseMessage -> {
                    PositionListResponse positionListResponse = JsonService.JSON.fromJson(responseMessage.message(), PositionListResponse.class);
                    log.info("Load positions account {} brokerId {} result: {}", getAccountId()
                            , getBrokerId()
                            , responseMessage);
                    processPositions(positionListResponse.getPositions());
                });
            } catch (Exception e) {
                log.error("Error while load positions", e);
            }
        }
    }

    public void requestOrders() {
        if (ConnectionUtil.isConnected(tradeDxApi.getUserSession().getConnectionStatus().get())) {
            try {
                var url = getOrderUrl();
                HttpUtil.get(url, tradeDxApi.getUserSession(), tradeDxApi.getRestApiClient(), responseMessage -> {
                    OrdersListResponse ordersListResponse = JsonService.JSON.fromJson(responseMessage.message(), OrdersListResponse.class);
                    log.info("Load orders account {} brokerId {} result: {}", getAccountId()
                            , getBrokerId()
                            , responseMessage);
                    OrderParserUtil.parseOrders(getBrokerId()
                            , getAccountId()
                            , tradeDxApi.getInstrumentService()
                            , ordersListResponse.getOrders());
                });
            } catch (Exception e) {
                log.error("Error while load orders", e);
            }
        }
    }


    @Override
    public void onOpen() {
        try {
            final AccountPortfoliosSubscriptionRequest accountPortfoliosSubscriptionRequest
                    = new AccountPortfoliosSubscriptionRequest();
            accountPortfoliosSubscriptionRequest.setSession(tradeDxApi.getUserSession().getToken());
            accountPortfoliosSubscriptionRequest.setRequestId(String.valueOf(requests.incrementAndGet()));
            accountPortfoliosSubscriptionRequest.setTimestamp(ZonedDateTime.now());
            var payload = new AccountPortfoliosSubscriptionRequest.Payload();
            payload.setAccounts(List.of(tradeDxApi.getUserSession().getAccountCode()));
            accountPortfoliosSubscriptionRequest.setPayload(payload);
            var request = JsonService.JSON.toJson(accountPortfoliosSubscriptionRequest);
            tradingChannel.send(request);
        } catch (Exception e) {
            log.error("Error while sending message", e);
        }
    }

    public boolean isOpen() {
        return tradingChannel.isOpen();
    }

    @Override
    public void onClose(ChannelStatus status) {

    }

    @Override
    public void onEvent(String eventMessage) {
        try {
//            System.out.println(eventMessage);
            log.info(eventMessage);
            var event = JsonService.JSON.fromJson(eventMessage, ServerEvent.class);
            //noinspection unchecked
            event.execute(tradeDxApi, tradingChannel, (BiConsumer<String, Object>) (name, o) -> {
                if ("AccountPortfolios".equals(name)) {
                    handle((Portfolios) o);
                }
            });
        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }


    @Override
    public void onError(Exception e) {

    }


}
/*
{
  "errorCode": "100",
  "description": "Entity already exists at server"
}
Response Code
409
 */



/*
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header 'Authorization: DXAPI 3opem3bcnainh01lfmaf4qlnip' -d '{ \
   "account": "default:m_1056663", \
   "orderCode": "12334452354", \
   "type": "MARKET", \
   "instrument": "EUR/USD", \
   "quantity": 10000, \
   "positionEffect": "OPEN", \
   "side": "BUY", \
   "tif": "GTC" \
 }' 'https://demo.dx.trade/dxsca-web/accounts/default%3Am_1056663/orders'
 */


/*market
{
  "account": "default:m_1056663",
  "orderCode": "smt1",
  "type": "MARKET",
  "instrument": "EUR/USD",
  "quantity": 10000,
  "positionEffect": "OPEN",
  "side": "BUY",
  "tif": "GTC"
}
 */

/* pending
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header 'Authorization: DXAPI 4r21tllsrpblkti71110ijehq3' -d '{ \
   "account": "default:m_1056663", \
   "orderCode": "smt7", \
   "type": "LIMIT", \
   "instrument": "EUR/USD", \
   "quantity": 10000, \
  "limitPrice": 1.08678,  \
   "positionEffect": "OPEN", \
   "side": "SELL", \
   "tif": "GTC" \
 }' 'https://demo.dx.trade/dxsca-web/accounts/default%3Am_1056663/orders'
 */