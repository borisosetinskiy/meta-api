package com.ob.api.mtx.mt4;

import com.google.common.cache.Cache;
import com.google.common.collect.Sets;
import com.ob.api.mtx.mt4.connector.connection.*;
import com.ob.api.mtx.mt4.connector.connection.auth.AuthUtil;
import com.ob.api.mtx.mt4.connector.connection.codec.MT4Crypt;
import com.ob.api.mtx.mt4.connector.connection.codec.vCRC32Java;
import com.ob.api.mtx.mt4.connector.connection.codec.vSHA1Java;
import com.ob.api.mtx.mt4.connector.entity.Mt4Account;
import com.ob.api.mtx.mt4.connector.entity.OrderHistoryTask;
import com.ob.api.mtx.mt4.connector.entity.OrderUpdateEvent;
import com.ob.api.mtx.mt4.connector.entity.StatusCode;
import com.ob.api.mtx.mt4.connector.entity.dto.*;
import com.ob.api.mtx.mt4.connector.entity.trading.Cmd;
import com.ob.api.mtx.mt4.connector.entity.trading.ExecutionType;
import com.ob.api.mtx.mt4.connector.entity.trading.OrderType;
import com.ob.api.mtx.mt4.connector.entity.trading.TradingEvent;
import com.ob.api.mtx.mt4.connector.error.DecompressorException;
import com.ob.api.mtx.mt4.connector.error.InvestorRoleError;
import com.ob.api.mtx.mt4.connector.error.NotConnectedException;
import com.ob.api.mtx.mt4.connector.error.RequoteException;
import com.ob.api.mtx.mt4.connector.parser.ConGroupBufParser;
import com.ob.api.mtx.mt4.connector.parser.InstrumentBufParser;
import com.ob.api.mtx.mt4.connector.parser.OrderBufParser;
import com.ob.api.mtx.mt4.connector.parser.TimeComponent;
import com.ob.api.mtx.mt4.connector.util.*;
import com.ob.api.mtx.util.BrokerUtil;
import com.ob.api.mtx.util.HostAndPortService;
import com.ob.broker.common.*;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.*;
import com.ob.broker.common.model.*;
import com.ob.broker.common.request.GroupOrderRequest;
import com.ob.broker.common.request.IRequest;
import com.ob.broker.common.request.OrderRequest;
import com.ob.broker.common.request.RequestType;
import com.ob.broker.service.LocalEntryService;
import com.ob.broker.service.ReconnectCronService;
import com.ob.broker.service.ReconnectService;
import com.ob.broker.util.ErrorUtil;
import com.ob.broker.util.LogUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscArrayQueue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ob.api.mtx.util.BrokerUtil.eventType;
import static com.ob.broker.common.error.Code.*;
import static com.ob.broker.util.Util.toInt;
import static com.ob.broker.util.Util.toLong;

@Slf4j
public class TradingMt4Api implements AutoCloseable, ApiConnectAction, ITradeBaseApi {
    @Setter
    Long extraLoopDelayInMills = 10L;
    @Setter
    Long maxSpinBeforeExtraLoopDelay = 5L;
    final HostAndPortService hostAndPortService = new HostAndPortService();
    final OrderHistoryClient orderHistoryClient = new OrderHistoryClient();
    final InstrumentComponent instrumentComponent = new InstrumentComponent();
    final Mt4Account mt4Account = new Mt4Account();
    final LoginListener loginListener = new LoginListener();
    final MessageHandlerListener messageHandlerListener = new MessageHandlerListener();
    @Setter
    Boolean isHedgeProhibited = false;
    @Setter
    MetricService metricService;

    final AtomicReference<Mt4ApiCredentials> apiCredentials = new AtomicReference<>(
            new Mt4ApiCredentials(0L, 0L, "", List.of(HostPort.DEFAULT), false, "", ""));


    final AtomicReference<ConnectionStatus> status = new AtomicReference<>(ConnectionStatus.OFFLINE);
    final OrderComponent orderComponent = new OrderComponent(true);
    final QuoteComponent quoteComponent = new QuoteComponent();
    final DataLoaderService dataLoaderService = new DataLoaderService();
    final MessageHandlerService messageHandlerService = new MessageHandlerService();
    final CommandComponent commandComponent = new CommandComponent();
    final Lock connectLock = new ReentrantLock();
    final Key key = new Key();

    private final Lock statusLock = new ReentrantLock();
    private final TransactionService transactionService = new TransactionService();
    private final ReconnectCronService reconnectCronService;
    private final TaskExecutor orderSendingExecutor;
    private final TaskExecutor connectTaskExecutor;
    private final ExecutorService messageHandlerExecutor;
    private final AtomicInteger RequestId = new AtomicInteger(1);
    Cache<String, OrderRequest> requestCache;
    LocalEntryService localEntryService;
    private TimeComponent timeComponent;
    private ConnectionWrapper cw;
    private final EventProducer eventProducer;
    private final ReconnectService reconnectService;
    final ApiOrderTaskExecutor apiTradingTaskExecutor = new ApiOrderTaskExecutor();
    final ErrorOrderNotifier errorOrderNotifier = new ErrorOrderNotifier();
    final DataRef dataRef = new DataRef();

    public TradingMt4Api(ReconnectCronService reconnectCronService
            , TaskExecutor calculateTaskExecutor
            , TaskExecutor taskReceiver
            , TaskExecutor orderSendingExecutor
            , TaskExecutor connectTaskExecutor
            , ExecutorService messageHandlerExecutor
            , Cache<String, OrderRequest> requestCache) {
        this.reconnectCronService = reconnectCronService;
        this.orderSendingExecutor = orderSendingExecutor;
        this.connectTaskExecutor = connectTaskExecutor;
        this.messageHandlerExecutor = messageHandlerExecutor;
        this.requestCache = requestCache;
        eventProducer = new EventProducer(taskReceiver, key);
        reconnectService = new ReconnectService(Set.of(ACCOUNT_DISABLED.getValue()
                , OLD_VERSION.getValue()
                , UNALLOWED_COMPANY.getValue()
                , INVALID_ACCOUNT.getValue()
                , INVESTOR_PASSWORD.getValue()), Set.of(), this);
        reconnectCronService.register(reconnectService);
        eventProducer.listener(EventTopic.CONNECT, reconnectService);
        this.localEntryService = new LocalEntryService(this
                , eventProducer
                , calculateTaskExecutor);
        eventProducer.listener(EventTopic.CONNECT, new EventConsumer() {
            @Override
            public void onNext(EventTopic topic, IBaseApi api, Event event) {
                if (event instanceof ConnectEvent) {
                    quoteComponent.resubscribe();
                }
            }

            @Override
            public String getId() {
                return "CONNECT";
            }
        });

        requestHandlers.put(RequestType.CLOSE, this::handleClose);
        requestHandlers.put(RequestType.CLOSE_SYMBOL, r -> runTask(r, new CloseOrderBySymbolAction(this::closeWithFilter)));
        requestHandlers.put(RequestType.CLOSE_ALL, r -> runTask(r, new CloseOrderAllAction(this::closeWithFilter)));
        requestHandlers.put(RequestType.CLOSE_GROUP, r -> runTask(r, new GroupOrderByIdsAction(this::closeWithFilter)));
        requestHandlers.put(RequestType.CLOSE_TYPE, r -> runTask(r, new CloseOrderByTypeAction(this::closeWithFilter)));
        requestHandlers.put(RequestType.OPEN, r -> runTask(r, new NewOrderAction(this::marketOrder)));
        requestHandlers.put(RequestType.UPDATE, r -> runTask(r, new OnTradeAction(this::updateOrder)));
        requestHandlers.put(RequestType.CANCEL_PENDING, r -> runTask(r, new OnOrderAction(this::cancelOrder)));
        requestHandlers.put(RequestType.CANCEL_GROUP, r -> runTask(r, new GroupOrderByIdsAction(this::cancelWithFilter)));
        requestHandlers.put(RequestType.PENDING, r -> runTask(r, new NewOrderAction(this::pendingOrder)));
        requestHandlers.put(RequestType.UPDATE_PENDING, r -> runTask(r, new OnOrderAction(this::updateOrder)));
        requestHandlers.put(RequestType.CANCEL_LOCAL_GROUP, this::handleCancelLocalGroup);
        requestHandlers.put(RequestType.CANCEL_LOCAL_PENDING, r -> deleteLocalOrder(r));
        requestHandlers.put(RequestType.LOCAL_PENDING, r -> createLocalOrder(r));
        requestHandlers.put(RequestType.UPDATE_LOCAL_PENDING, r -> updateLocalOrder(r));
        requestHandlers.put(RequestType.LOAD_LOCAL_PENDING, r -> addLocalOrder(r));
    }


    private static boolean isNotAccepted(OrderStateData orderStateData) {
        return OrderStateData.Cancelled.equals(orderStateData)
               || OrderStateData.Rejected.equals(orderStateData);
    }

    public static ConSymbolGroupDto parseConSymbolGroupDto(byte[] buf, int offset) throws Exception {
        ConSymbolGroupDto conSymbolGroup = new ConSymbolGroupDto();
        conSymbolGroup.setName(readString(buf, offset, 16));
        conSymbolGroup.setDescription(readString(buf, offset + 16, 64));
        return conSymbolGroup;
    }

    private static String readString(byte[] buf, int offset, int length) {
        byte[] bytes = Arrays.copyOfRange(buf, offset, offset + length);
        String str = new String(bytes, StandardCharsets.UTF_8);
        int nullIndex = str.indexOf('\0');
        return nullIndex == -1 ? str : str.substring(0, nullIndex);
    }

    private static String readStringASCII(byte[] buf, int offset, int length) {
        List<Byte> res = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            if (buf[offset + i] == 0) {
                if (i > 0) break;
            } else {
                res.add(buf[offset + i]);
            }
        }
        byte[] byteArray = new byte[res.size()];
        for (int i = 0; i < res.size(); i++) {
            byteArray[i] = res.get(i);
        }
        return new String(byteArray, StandardCharsets.US_ASCII);
    }


    @Override
    public void close() throws Exception {
        try {
            disconnect();
        } catch (Exception ignored) {
        }
        try {
            reconnectCronService.unregister(reconnectService);
            orderSendingExecutor.shutdown(Util.key(getBrokerId(), getAccountId()));
            eventProducer.shutdown();
        } catch (Exception ignored) {
        }
    }

    public HostPort currentHost() {
        return hostAndPortService.current();
    }

    @Override
    public void connect(ApiCredentials apiCredentials) {
        setApiCredentials(apiCredentials);
        connect();
    }

    @Override
    public void connect() {
        try {
            if (isConnected())
                throw new CodeException("Already connected", Code.CONNECT_ALREADY_EXIST);
            var credentials = apiCredentials.get();
            key.setAccountId(credentials.getAccountId());
            key.setBrokerId(credentials.getBrokerId());
            key.setKey(Util.key(getBrokerId(), getAccountId()));
            var cc = new ConnectionData(credentials);
            this.cw = new ConnectionWrapper(cc, new Session(credentials));
            hostAndPortService.init(getBrokerId(), credentials.getBrokerName(), "mt4", credentials.getHostPorts());
            tryConnect();
        } catch (Exception e) {
            log.error("Error while processing key {}", key, e);
            var error = ErrorUtil.toError(e);
            var event = new GeneralErrorEvent();
            event.setError(error);
            event.setAccountId(getAccountId());
            event.setBrokerId(getBrokerId());
            event.setEventType(EventType.LOGON);
            produce(EventTopic.CONNECT, event);
        }
    }

    @Override
    public void simulateConnect() {

    }

    @Override
    public void disconnect() {
        connectLock.lock();
        setStatus(ConnectionStatus.DEAD);
        try {
            try {
                if (cw != null) {
                    cw.getConnection().disconnect();
                }
            } catch (Exception ignored) {
            }
            clearAndStop();
            var event = new DisconnectEvent(getBrokerId(), getAccountId());
            produce(EventTopic.CONNECT, event);
        } catch (Exception ignored) {

        } finally {
            connectLock.unlock();
        }
    }

    void tryConnect() {
        connectLock.lock();

        try {
            if (isConnected())
                return;
            setStatus(ConnectionStatus.CONNECTING);
            clearAndStop();
            HostPort hostPort = hostAndPortService.next();
            try {
                if (!hostPort.isValid()) {
                    throw new CodeException("%s %s".formatted(hostPort.getHost(), hostPort.getPort()), BAD_HOST);
                }
                final String key = hostPort.getHost() + ":" + hostPort.getPort();
                timeComponent = new TimeComponent(key);
                this.cw.resetConnection();
                orderComponent.clear();
                LogUtil.log("connect-begin", "api", getAccountId(), getBrokerId(),
                        msg ->
                                log.info("{},\"host\":\"{}\"",
                                        msg
                                        , hostPort.getHost()));
                dataLoaderService.loadAccount(hostPort);
                messageHandlerService.start();
            } catch (Exception e) {
                handleConnectionException(e, hostPort);
            }
        } finally {
            connectLock.unlock();
        }

    }

    private void handleConnectionException(Exception e, HostPort hostPort) {

        final CodeException connectError;

        if (e instanceof CodeException ce) {
            boolean isNetworkError = ce.getCode() == UNKNOWN_HOST.getValue()
                                     || ce.getCode() == CONNECT_TIMEOUT_ERROR.getValue();
            if (isNetworkError) {
                hostPort.setValid(false);
                connectError = CodeException.addMessage(ce, hostPort.toString());
            } else {
                connectError = ce;
            }
        } else if (e instanceof SocketTimeoutException ste) {
            hostPort.setValid(false);
            connectError = new CodeException(ste.getMessage() + " | " + hostPort.getHost() + ":" + hostPort.getPort(), CONNECT_TIMEOUT_ERROR);
        } else {
            connectError = ErrorUtil.toError(e);
        }
        log.error("FAILED-CONNECT {}", Util.key(getBrokerId(), getAccountId()), connectError);
        notifyConnectError(connectError, EventType.CONNECT);
    }

    @Override
    public IBaseApi setApiCredentials(ApiCredentials apiCredentials) {
        if ((apiCredentials instanceof Mt4ApiCredentials mt4ApiCredentials)) {
            this.apiCredentials.getAndSet(mt4ApiCredentials);
        }
        return this;
    }

    @Override
    public IBaseApi setApiSetting(ApiSetting apiSetting) {
        reconnectService.reconnectConfig.setDisabled(apiSetting.isDisableReconnect());
        if (apiSetting.isDisableReconnect()) {
            if (apiSetting.getMaxConnectAttempt() != null)
                reconnectService.reconnectConfig.setMaxReconnectionAttempt(apiSetting.getMaxConnectAttempt());
            if (apiSetting.getMaxConnectOnWeekendAttempt() != null)
                reconnectService.reconnectConfig.setMaxReconnectionOnWeekendAttempt(apiSetting.getMaxConnectOnWeekendAttempt());
        }
        return this;
    }

    private void clearAndStop() {
        LogUtil.log("stop message handler", "api", getAccountId(), getBrokerId()
                , log::debug);
        messageHandlerService.running.set(false);
        instrumentComponent.clear();
        orderComponent.clear();
        quoteComponent.clear();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return status.get();
    }

    @Override
    public void subscribe(String symbol, boolean critical) {
        if (isConnected()) {
            try {
                this.quoteComponent.subscribe(symbol);
            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                if (critical) {
                    throw codeException;
                }
                produce(EventTopic.SUBSCRIBE, new SubscribeErrorEvent(getBrokerId()
                                , getAccountId()
                                , symbol
                                , true
                                , codeException
                                , EventType.SUBSCRIBE
                        )
                );
            }
        } else {
            CodeException codeException = new CodeException("Not connected", Code.NO_CONNECTION);
            if (critical) {
                throw codeException;
            }
            produce(EventTopic.SUBSCRIBE, new SubscribeErrorEvent(getBrokerId()
                    , getAccountId()
                    , symbol
                    , true
                    , codeException
                    , EventType.SUBSCRIBE)
            );
        }
    }

    @Override
    public void subscribe(String symbol) {
        subscribe(symbol, false);
    }

    @Override
    public void unsubscribe(String symbol) {
        if (isConnected()) {
            try {
                this.quoteComponent.unsubscribe(symbol);
            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                produce(EventTopic.SUBSCRIBE, new SubscribeErrorEvent(getBrokerId()
                                , getAccountId()
                                , symbol
                                , false
                                , codeException
                                , EventType.SUBSCRIBE
                        )
                );
            }
        } else {
            CodeException codeException = new CodeException("Not connected", Code.NO_CONNECTION);
            produce(EventTopic.SUBSCRIBE, new SubscribeErrorEvent(getBrokerId()
                    , getAccountId()
                    , symbol
                    , false
                    , codeException
                    , EventType.SUBSCRIBE)
            );
        }
    }

    @Override
    public void addListener(EventTopic topic, EventConsumer eventConsumer) {
        eventProducer.listener(topic, eventConsumer);
    }

    @Override
    public void removeListener(EventTopic topic, EventConsumer eventConsumer) {
        var listeners = eventProducer.getConsumers().get(topic);
        if (listeners != null && eventConsumer.getId() != null) {
            listeners.remove(eventConsumer.getId());
        }
    }

    @Override
    public List<ContractData> getAllContractData() {
        return instrumentComponent.getInstruments().values().stream()
                .map((Function<Instrument, ContractData>) instrument -> instrument)
                .collect(Collectors.toList());
    }

    @Override
    public ContractData getContractData(String symbol) {
        return instrumentComponent.getInstruments().get(symbol);
    }

    @Override
    public void shutdown() {
        try {
            close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void update(ConnectionStatus connectionStatus) {
        setStatus(connectionStatus);
    }

    public boolean isConnected() {
        return status.get() == ConnectionStatus.ONLINE && cw.getConnection().isConnected()
               && messageHandlerService.running.get();
    }

    @Override
    public boolean isDead() {
        return status.get() == ConnectionStatus.DEAD;
    }

    @Override
    public void fireError(Throwable throwable) {
        var error = ErrorUtil.toError(throwable);
        var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, EventType.RECONNECT);
        produce(EventTopic.ERROR, event);
    }

    @Override
    public void fireReconnectError(Throwable throwable) {
        executeConnectTask(() -> {
            var error = ErrorUtil.toError(throwable);
            var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, EventType.RECONNECT);
            produce(EventTopic.CONNECT, event);
            disconnect();
        });

    }

    @Override
    public void fireConnect() throws Exception {
        executeConnectTask(this::connect);
    }

    private void executeConnectTask(Runnable task) {
        var id = Util.key(getBrokerId(), getAccountId());
        long time = System.currentTimeMillis();
        connectTaskExecutor.submit(id
                , new Task() {
                    @Override
                    public String connectorId() {
                        return id;
                    }

                    @Override
                    public long timestamp() {
                        return time;
                    }

                    @Override
                    public void run() {
                        task.run();
                    }
                });
    }

    @Override
    public Long getBrokerId() {
        return apiCredentials.get().getBrokerId();
    }

    @Override
    public Object getAccountId() {
        return apiCredentials.get().getAccountId();
    }


    double selectPrice(int operation, double price, QuoteData quoteEvent) {
        if (quoteEvent != null) {
            if (operation == OrderType.BUY.getValue()) {
                return quoteEvent.getAsk().doubleValue();
            } else {
                return quoteEvent.getBid().doubleValue();
            }
        }
        return price;
    }

    double getPrice(String symbol, int operation, double price) {
        return selectPrice(operation, price, getPrice(symbol));
    }


    QuoteData getPrice(String symbol) {
        QuoteData quoteEvent = quoteComponent.getQuote(symbol);
        if (quoteEvent == null) {
            try {
                if (!quoteComponent.isSubscribed(symbol)) {
                    quoteComponent.subscribe(symbol);
                }
            } catch (Exception ignored) {
            }
        }
        return quoteEvent;
    }

    @Override
    public void execute(List<IRequest> requests) {
        requests.forEach(this::execute);
    }

    private final Map<RequestType, Consumer<OrderRequest>> requestHandlers = new EnumMap<>(RequestType.class);

    private void prepareOrderRequest(OrderRequest request) {
        request.setRequestId(RequestId.incrementAndGet());
        request.setAccountId(getAccountId());
        request.setBrokerId(getBrokerId());
    }

    @Override
    public void execute(IRequest request) {
        if (!(request instanceof OrderRequest orderRequest)) return;
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString().replace("-", "");
        orderRequest.setRID(BrokerUtil.key(getBrokerId(), getAccountId())+"_"+id);
        try {
            if (!isConnected()) {
                throw new CodeException("Can't execute request " + request.getTID() + ", Please reconnect.", Code.CONNECT_TIMEOUT_ERROR);
            }

            prepareOrderRequest(orderRequest);

            var handler = requestHandlers.get(orderRequest.getRequestType());
            if (handler != null) {
                handler.accept(orderRequest);
            } else {
                throw new CodeException(Code.INVALID_PARAM);
            }
        } catch (Exception e) {
            errorHandler(orderRequest, null, e);
        }
    }

    private void runTask(OrderRequest request, OrderAction action) {
        runTask(request, action, 1);
    }


    void deleteLocalOrder(OrderRequest request) {
        localEntryService.delete(request.getTicket());
    }

    void createLocalOrder(OrderRequest request) {
        localEntryService.create(request);
    }

    void updateLocalOrder(OrderRequest request) {
        localEntryService.update(request);
    }

    void addLocalOrder(OrderRequest request) {
        localEntryService.addOrder(request);
    }

    private void runTask(OrderRequest orderRequest, OrderAction orderAction, int attempts) {
        apiTradingTaskExecutor.execute(orderRequest, new OrderTask(
                        attempts
                        , orderRequest
                        , this
                        , orderAction
                        , apiTradingTaskExecutor)
                , (orderData, e) -> errorHandler(orderRequest, orderData, e)
        );
    }

    private void handleClose(OrderRequest request) {
        int attempts = isHedgeProhibited ? 1 : 3;
        runTask(request, new OnTradeAction(this::closeOrder), attempts);
    }

    private void handleCancelLocalGroup(OrderRequest request) {
        if (request instanceof GroupOrderRequest groupOrderRequest) {
            localEntryService.delete(groupOrderRequest.getTickets());
        }
    }

    class ApiOrderTaskExecutor implements OrderTaskExecutor {
        @Override
        public void execute(OrderRequest request, Runnable command, BiConsumer<OrderData, Exception> errorHandler) {
            long startTime = System.currentTimeMillis();
            Task task = new SimpleTask() {
                @Override
                public String connectorId() {
                    return key.getKey();
                }

                @Override
                public void run() {
                    try {
                        command.run();
                        if (metricService != null) {
                            long time = System.currentTimeMillis();
                            String operation = request.getRequestType().toString();
                            metricService.record("api_execution", time - startTime, "type", operation);
                            metricService.record("request_execution", time - request.getTimestamp(), "type", operation);
                        }
                    } catch (OrderException e) {
                        errorHandler.accept(e.getOrderData(), e);
                    } catch (Exception e) {
                        errorHandler.accept(null, e);
                    }
                }
            };
            orderSendingExecutor.submit(BrokerUtil.key(getBrokerId(), getAccountId()), task);
        }
    }

    void cancelWithFilter(OrderRequest orderRequest, Predicate<OrderData> filter) {
        var orders = pending().stream().filter(filter)
                .sorted(Comparator.comparing(OrderData::getTicket))
                .toList();
        if (orders.isEmpty()) {
            throw new CodeException("No orders", Code.NOT_FOUND);
        }
        orders.forEach(order -> {
            OrderRequest request = new OrderRequest();
            request.setTID(order.getTID());
            request.setTime(orderRequest.getTime());
            request.setComment(orderRequest.getComment());
            request.setPrice(order.getPrice());
            request.setLot(order.getLot());
            request.setType(order.getOrderType());
            request.setSymbol(order.getSymbol());
            request.setTicket(order.getTicket());
            request.setAccountId(getAccountId());
            request.setBrokerId(getBrokerId());
            request.setRequestType(RequestType.CANCEL_PENDING);
            try {
                execute(request);
            } catch (Exception e) {
                errorHandler(orderRequest, order, e);
            }
        });
    }


    void closeWithFilter(OrderRequest orderRequest, Predicate<OrderData> filter) {
        var orders = opened().stream().filter(filter)
                .sorted(Comparator.comparing(OrderData::getTicket))
                .toList();
        if (orders.isEmpty()) {
            throw new CodeException("No orders", Code.NOT_FOUND);
        }
        orders.forEach(order -> {
            OrderRequest request = new OrderRequest();
            request.setTID(order.getTID());
            request.setTime(orderRequest.getTime());
            request.setComment(orderRequest.getComment());
            request.setPrice(order.getPrice());
            request.setLot(order.getLot());
            request.setType(order.getOrderType());
            request.setSymbol(order.getSymbol());
            request.setTicket(order.getTicket());
            request.setAccountId(getAccountId());
            request.setBrokerId(getBrokerId());
            request.setRequestType(RequestType.CLOSE);
            try {
                execute(request);
            } catch (Exception e) {
                errorHandler(orderRequest, order, e);
            }
        });
    }

    private void errorHandler(OrderRequest request, OrderData order, Exception e) {
        var error = ErrorUtil.toError(e);
        var event = new OrderErrorEvent(getBrokerId(), getAccountId()
                , request.getTID(), error
                , EventType.REJECT_ORDER
                , request
                , order);
        produce(EventTopic.ORDER_REQUEST, event);

    }

    public void cancelOrder(OrderRequest request
            , OrderData order) {
        try {
            var command = new OrderDelete(dataRef, request, order);
            if (!messageHandlerService.queue.offer(command)) {
                log.error("Can't send order {}, Queue issue", request);
                if (metricService != null) {
                    metricService.increment("order_request", "type", request.getRequestType().name());
                }
            }
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    public static int orderType(OrderTypeData type) {
        switch (type) {
            case Buy -> {
                return OrderType.BUY.getValue();
            }
            case Sell -> {
                return OrderType.SELL.getValue();
            }
            case BuyStop -> {
                return OrderType.BUY_STOP.getValue();
            }
            case SellStop -> {
                return OrderType.SELL_STOP.getValue();
            }
            case BuyLimit -> {
                return OrderType.BUY_LIMIT.getValue();
            }
            case SellLimit -> {
                return OrderType.SELL_LIMIT.getValue();
            }
            default -> throw new CodeException(UNSUPPORTED_OPERATION);
        }
    }

    public void pendingOrder(OrderRequest request) {
        try {
            var command = new OrderPending(dataRef, request);
            if (!messageHandlerService.queue.offer(command)) {
                log.error("Can't send order {}, Queue issue", request);
                if (metricService != null) {
                    metricService.increment("order_request", "type", request.getRequestType().name());
                }
            }
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    public void updateOrder(OrderRequest request, OrderData order) {
        try {
            var command = new OrderModify(dataRef, request, order);
            if (!messageHandlerService.queue.offer(command)) {
                log.error("Can't send order {}, Queue issue", request);
                if (metricService != null) {
                    metricService.increment("order_request", "type", request.getRequestType().name());
                }
            }

        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    public void marketOrder(OrderRequest request) {
        try {
            var command = new OrderMarket(dataRef, request);
            if (!messageHandlerService.queue.offer(command)) {
                log.error("Can't send order {}, Queue issue", request);
                if (metricService != null) {
                    metricService.increment("order_request", "type", request.getRequestType().name());
                }
            }
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }

    }

    public void closeOrder(OrderRequest request, OrderData order) {
        try {
            var command = new OrderClose(dataRef, order, request);
            if (!messageHandlerService.queue.offer(command)) {
                log.error("Can't send order {}, Queue issue", request);
                if (metricService != null) {
                    metricService.increment("order_request", "type", request.getRequestType().name());
                }
            }
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    @Override
    public List<OrderData> opened() {
        return List.copyOf(trades().values());
    }

    @Override
    public List<OrderData> pending() {
        return List.copyOf(orders().values());
    }

    @Override
    public List<CloseOrderData> closed() {
        return List.copyOf(history().values());
    }

    @Override
    public Map<Integer, OrderData> trades() {
        return orderComponent.openedOrders;
    }

    @Override
    public Map<Integer, OrderData> orders() {
        return orderComponent.pendingOrders;
    }

    @Override
    public Map<Integer, CloseOrderData> history() {
        return orderComponent.closedOrders;
    }

    @Override
    public OrderData findOrder(Long ticket) {
        var order = orderComponent.openedOrders.get(ticket.intValue());
        if (order == null) {
            order = orderComponent.pendingOrders.get(ticket.intValue());
        }
        if (order == null)
            throw new CodeException("Can't find order " + ticket, Code.NOT_FOUND);
        return order;
    }

    @Override
    public List<OrderData> findOrders(List<Long> tickets) {
        return tickets.stream().map(this::findOrder).collect(Collectors.toList());
    }

    @Override
    public AccountData getAccountData() {
        return toAccountData();
    }

    OrderEvent toOrderEvent(OrderRequest request, OrderStateData orderStateData
            , OrderTypeData orderTypeData, Mt4Order order, CodeException codeException) {
        boolean notAccepted = isNotAccepted(orderStateData);
        IOrder iorder = getIOrder(orderStateData, order, notAccepted);
        return OrderEvent.builder()
                .request(request)
                .error(notAccepted ? codeException : null)
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .order(iorder)
                .orderStateData(orderStateData)
                .eventType(eventType(orderStateData, orderTypeData))
                .symbol(order != null ? order.getSymbol() : null)
                .TID(order != null ? HelpUtil.toLong(order.getClOrdId()) : request.getTID())
                .ticket(order != null ? HelpUtil.toLong(order.getTicket()) : null)
                .build();
    }

//    OrderErrorEvent toErrorOrderEvent(OrderRequest request, OrderStateData orderStateData
//            , OrderTypeData orderTypeData, Mt4Order order, CodeException codeException) {
//
//        boolean notAccepted = isNotAccepted(orderStateData);
//        IOrder iorder = getIOrder(orderStateData, order, notAccepted);
//        return new OrderErrorEvent(getBrokerId()
//                , getAccountId()
//                , order != null ? HelpUtil.toLong(order.getClOrdId()) : request.getTID()
//                , codeException
//                , eventType(orderStateData, orderTypeData)
//                , request
//                , iorder);
//
//
//    }

    OrderErrorEvent toErrorOrderEvent(OrderRequest request, OrderStateData orderStateData
            , OrderTypeData orderTypeData, OrderData order, CodeException codeException) {
        return new OrderErrorEvent(getBrokerId()
                , getAccountId()
                , order != null ? order.getTID() : request.getTID()
                , codeException
                , eventType(orderStateData, orderTypeData)
                , request
                , order);
    }

    private IOrder getIOrder(OrderStateData orderStateData, Mt4Order order, boolean notAccepted) {
        if (order == null)
            return null;
        IOrder iorder = null;
        if (OrderStateData.Closed.equals(orderStateData)) {
            iorder = toCloseOrderData(order);
        } else if (OrderStateData.Opened.equals(orderStateData)
                   || OrderStateData.Filled.equals(orderStateData)
                   || OrderStateData.Modified.equals(orderStateData)
                   || OrderStateData.Placed.equals(orderStateData)
        ) {
            iorder = toOrderData(order);
        } else if (notAccepted) {
            iorder = toOrderData(order);
        }
        return iorder;
    }

    void sendOrderEvent(EventTopic eventTopic
            , OrderRequest request
            , OrderStateData orderStateData, OrderTypeData orderTypeData, Mt4Order order, CodeException exception) {
        OrderEvent orderEvent = toOrderEvent(request, orderStateData, orderTypeData, order, exception);
        eventProducer.eventConsumer(eventTopic)
                .accept(TradingMt4Api.this, orderEvent);
    }

    class ErrorOrderNotifier {
        void notifyOrderError(EventTopic eventTopic
                , OrderRequest request
                , OrderStateData orderStateData
                , OrderTypeData orderTypeData
                , OrderData order
                , CodeException exception) {
            sendErrorOrderEvent(eventTopic, request, orderStateData, orderTypeData, order, exception);
        }
    }

    void sendErrorOrderEvent(EventTopic eventTopic
            , OrderRequest request
            , OrderStateData orderStateData
            , OrderTypeData orderTypeData
            , OrderData order
            , CodeException exception) {
        OrderErrorEvent orderErrorEvent = toErrorOrderEvent(request, orderStateData, orderTypeData, order, exception);
        eventProducer.eventConsumer(eventTopic)
                .accept(TradingMt4Api.this, orderErrorEvent);
    }


    private String getRequestKey(Object id) {
        return getBrokerId() + "_" + getAccountId() + "_" + id;
    }


    class DataRef {
        CommandComponent commandComponent() {
            return commandComponent;
        }

        ErrorOrderNotifier errorNotifier() {
            return errorOrderNotifier;
        }

        InstrumentComponent instrumentComponent() {
            return instrumentComponent;
        }
    }

    public ConGroupSecDto getSymbolGroupParams(int symbolGroup) {
        return this.mt4Account.getConGroupDto().get() != null
               && this.mt4Account.getConGroupDto().get().getSecgroups() != null
               && this.mt4Account.getConGroupDto().get().getSecgroups().length > symbolGroup
                ? this.mt4Account.getConGroupDto().get().getSecgroups()[symbolGroup] : null;
    }

    OrderData toOrderData(Mt4Order mt4Order) {
        return OrderData.builder()
                .TID(HelpUtil.toLong(mt4Order.getClOrdId()))
                .ticket(HelpUtil.toLong(mt4Order.getTicket()))
                .comment(mt4Order.getComment())
                .lot(mt4Order.getLots())
                .price(mt4Order.getOpenPrice())
                .sl(mt4Order.getStopLossPrice())
                .tp(mt4Order.getTakeProfitPrice())
                .symbol(mt4Order.getSymbol())
                .orderType(HelpUtil.toOrderTypeData(mt4Order.getOrderType()))
                .taxes(mt4Order.getTaxes())
                .swap(mt4Order.getSwap())
                .profit(mt4Order.getProfit())
                .commission(mt4Order.getCommission())
                .time(mt4Order.getOpenTimestamp())
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .build();
    }

    CloseOrderData toCloseOrderData(Mt4Order mt4Order) {
        return CloseOrderData.builder()
                .TID(HelpUtil.toLong(mt4Order.getClOrdId()))
                .ticket(HelpUtil.toLong(mt4Order.getTicket()))
                .comment(mt4Order.getComment())
                .lot(mt4Order.getLots())
                .openPrice(mt4Order.getOpenPrice())
                .closePrice(mt4Order.getClosePrice())
                .closeTime(mt4Order.getCloseTimestamp())
                .openTime(mt4Order.getOpenTimestamp())
                .sl(mt4Order.getStopLossPrice())
                .tp(mt4Order.getTakeProfitPrice())
                .symbol(mt4Order.getSymbol())
                .orderType(HelpUtil.toOrderTypeData(mt4Order.getOrderType()))
                .taxes(mt4Order.getTaxes())
                .swap(mt4Order.getSwap())
                .profit(mt4Order.getProfit())
                .commission(mt4Order.getCommission())
                .time(mt4Order.getCloseTimestamp())
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .build();
    }

    private void notifyConnectError(Throwable throwable, EventType eventType) {
        if (!isDead()) {
            setStatus(ConnectionStatus.OFFLINE);
            var error = ErrorUtil.toError(throwable);
            var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, eventType);
            produce(EventTopic.CONNECT, event);
        }
    }

    void produce(EventTopic topic, Event event) {
        if (event == null) {
            return;
        }
        try {
            eventProducer.eventConsumer(topic)
                    .accept(TradingMt4Api.this, event);
        } catch (Exception e) {
            log.warn("Event {} topic {} issue ", event, topic, e);
        }
    }

    private void setStatus(ConnectionStatus s) {
        statusLock.lock();
        try {
            LogUtil.log("connection-status-update", "api", getAccountId()
                    , getBrokerId(),
                    text ->
                            log.info("{}, \"status\":\"{}\"", text, s));
            status.getAndSet(s);
            if (s == ConnectionStatus.OFFLINE || s == ConnectionStatus.DEAD) {
                messageHandlerService.running.getAndSet(false);
            }
        } finally {
            statusLock.unlock();
        }
    }

    AccountSnapshot toAccountSnapshot() {
        return AccountSnapshot.builder()
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .balance(mt4Account.getBalance().get())
                .credit(mt4Account.getCredit().get())
                .currency(mt4Account.getConGroupDto().get().getCurrency())
                .marginMode(mt4Account.getConGroupDto().get().getMarginMode())
                .leverage(mt4Account.getLeverage())
                .amountScale(2)
                .build();
    }

    AccountData toAccountData() {
        return AccountData.builder()
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .leverage(mt4Account.getLeverage())
                .currency(mt4Account.getConGroupDto().get().getCurrency())
                .balance(mt4Account.getBalance().get())
                .credit(mt4Account.getCredit().get())
                .amountScale(2)
                .build();
    }

    @Getter
    static class InstrumentComponent {
        private final Map<String, Instrument> instruments = new ConcurrentHashMap<>();
        private final Map<Short, Instrument> instrumentsById = new ConcurrentHashMap<>();
        private final Map<Short, String> nameById = new ConcurrentHashMap<>();

        public void update(Collection<Instrument> inputInstruments) {
            for (Instrument instrument : inputInstruments) {
                instruments.put(instrument.getSymbol(), instrument);
                instrumentsById.put(instrument.getSymbolIndex(), instrument);
                nameById.put(instrument.getSymbolIndex(), instrument.getSymbol());
            }
        }

        public void updateAllSymbolsMargin(ConGroupMarginDto[] conGroupMargin, int size) {
            for (int i = 0; i < size; i++)
                updateMargin(conGroupMargin[i]);
        }

        void updateMargin(ConGroupMarginDto cgm) {
            if (!instruments.containsKey(ByteUtil.byteToString(cgm.getSymbol(), 0)))
                return;

            String instrumentName = ByteUtil.byteToString(cgm.getSymbol(), 0);
            Instrument instrument = instruments.get(instrumentName);

            if (cgm.getMarginDivider() != Double.MAX_VALUE) {
                instrument.setMarginDivider(BigDecimal.valueOf(cgm.getMarginDivider()));
            }
            if (cgm.getSwapLong() != Double.MAX_VALUE) {
                instrument.setSwapLong(BigDecimal.valueOf(cgm.getSwapLong()));
            }
            if (cgm.getSwapShort() != Double.MAX_VALUE) {
                instrument.setSwapShort(BigDecimal.valueOf(cgm.getSwapShort()));
            }
        }

        public Collection<String> getSymbols() {
            return instruments.keySet();
        }

        public Collection<Short> getIds() {
            return nameById.keySet();
        }

        public Instrument getInstrument(String symbol) {
            final Instrument instrument = instruments.get(symbol);
            if (instrument == null) {
                throw new CodeException(symbol + " not exist", NOT_FOUND);
            }
            return instrument;
        }

        public boolean isExist(String symbol) {
            return instruments.containsKey(symbol);
        }

        public short getCode(String symbol) throws Exception {
            Instrument instrument = instruments.get(symbol);
            if (instrument == null)
                throw new Exception("Instrument not found for symbol " + symbol);
            return instrument.getSymbolIndex();
        }

        public String getSymbol(short code) {
            return nameById.get(code);
        }

        public void clear() {
            instruments.clear();
            instrumentsById.clear();
            nameById.clear();
        }
    }

    class LoginListener {

        public void onLoadInstrument(byte[] buf) throws Exception {
            List<Instrument> instruments = InstrumentBufParser.parseInstruments(buf);
            instrumentComponent.update(instruments);
        }

        public void onLoadOrderHistory() throws Exception {
            ZonedDateTime fromDateTime = ZonedDateTime.now().minusDays(30);
            ZonedDateTime toDateTime = ZonedDateTime.now().plusDays(1);
            int from = (int) fromDateTime.toEpochSecond();
            int to = (int) toDateTime.toEpochSecond();
            List<Mt4Order> historyMt4Orders = new ArrayList<>();
            try {
                OrderHistoryUtil.read(cw.getConnection(), from, to
                        , historyMt4Orders::add, timeComponent);
            } catch (Exception e) {
                LogUtil.log("history-load", "api", getAccountId(), getBrokerId()
                        , msg -> log.error("{}, \"from\":{},\"to\":{},\"error\":{}",
                                msg
                                , fromDateTime
                                , toDateTime
                                , e.getMessage()));
                throw new CodeException(Code.HISTORY_LOAD_ERROR);
            }
            List<CloseOrderData> closed =
                    historyMt4Orders.stream().map(mt4Order -> {
                        try {
                            return CloseOrderData.builder()
                                    .accountId(getAccountId())
                                    .brokerId(getBrokerId())
                                    .lot(mt4Order.getLots())
                                    .comment(mt4Order.getComment())
                                    .ticket(HelpUtil.toLong(mt4Order.getTicket()))
                                    .TID(HelpUtil.toLong(mt4Order.getClOrdId()))
                                    .symbol(mt4Order.getSymbol())
                                    .profit(mt4Order.getProfit())
                                    .commission(mt4Order.getCommission())
                                    .taxes(mt4Order.getTaxes())
                                    .balance(mt4Account.getBalance().get())
                                    .orderType(HelpUtil.toOrderTypeData(mt4Order.getOrderType()))
                                    .openPrice(mt4Order.getOpenPrice())
                                    .openTime(mt4Order.getOpenTimestamp())
                                    .closePrice(mt4Order.getClosePrice())
                                    .closeTime(mt4Order.getCloseTimestamp())
                                    .build();
                        } catch (Exception ignored) {
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
            orderComponent.setClosedOrders(closed);
        }


        public void onLoadAccount(byte[] buf) {
            ConGroupDto conGroupDto = ConGroupBufParser.parse(buf, 0x58);
            mt4Account.accountId = toLong(getAccountId());
            mt4Account.brokerId = getBrokerId();
            mt4Account.getConGroupDto().set(conGroupDto);
            String name = ByteUtil.byteToString(buf, 4);
            int leverage = ByteUtil.getInt(buf, 0x44);
            double balance = ByteUtil.getDouble(buf, 0x48);
            double credit = ByteUtil.getDouble(buf, 0x50);
            int accountMode = ByteUtil.getInt(buf, 0x35D8);
            /* Account mode. 0 - master, 1 - investor */
            if (accountMode == 1)
                throw new InvestorRoleError();
            mt4Account.setName(name);
            mt4Account.setLeverage(leverage);
            mt4Account.getBalance().set(BigDecimal.valueOf(balance));
            mt4Account.getCredit().set(BigDecimal.valueOf(credit));
            mt4Account.setAccountMode(accountMode);
            updateAllSymbolsMargin();
            try {
                int timeZone = buf[0x35E0] & 0xff;
                int timeDst = buf[0x35E1] & 0xff;
                timeComponent.setServerTimeZone(timeZone, timeDst);
            } catch (Exception e) {
                log.error("Broker {} Account {} time-zone", getBrokerId(), getAccountId(), e);
            }
        }


        public void onLoadOrder(byte[] buf, int serverBuildVersion) {
            List<Mt4Order> mt4Orders = OrderBufParser.readOrders(buf, serverBuildVersion, timeComponent);
            mt4Orders = mt4Orders.stream()
                    .filter(mt4Order -> mt4Order.getOrderType() != OrderType.CREDIT && mt4Order.getOrderType() != OrderType.BALANCE)
                    .collect(Collectors.toList());
            orderComponent.update(mt4Orders);
        }


        public void updateAllSymbolsMargin() {
            instrumentComponent.updateAllSymbolsMargin(mt4Account.getConGroupDto().get().getConGroupMargin()
                    , mt4Account.getConGroupDto().get().getSecMarginsTotal());
        }


        public void onServerDetails(ServerDetails serverDetails) {
            if (serverDetails != null)
                hostAndPortService.update(serverDetails.toServerData(), "mt4");
        }
    }

    class MessageHandlerListener {
        public void onQuote(byte[] buf) throws Exception {
            quoteComponent.onQuote(buf);
        }

        public void onInstrument(byte[] buf) throws Exception {
            eventProducer.execute(EventTopic.CUSTOM, () -> {
                try {
                    Collection<Instrument> instruments = InstrumentBufParser.parseInstruments4Update(buf);
                    instrumentComponent.update(instruments);
                    ConGroupDto conGroupDto = mt4Account.getConGroupDto().get();
                    instrumentComponent.updateAllSymbolsMargin(conGroupDto.getConGroupMargin(), conGroupDto.getSecMarginsTotal());
                } catch (Exception e) {
                    log.error("Error on instrument update", e);
                }
            });

        }


        public void onOrders(byte[] buf) throws IOException {
            eventProducer.execute(EventTopic.ORDER, () -> {
                try {
                    List<OrderUpdateEvent> orderUpdateEvents = OrderBufParser.parseOrderEvents(buf, timeComponent);
                    orderUpdateEvents.forEach(orderUpdateEvent -> {
                        final Mt4Order mt4Order = orderUpdateEvent.getMt4Order();
                        LogUtil.log("order-process", "api", getAccountId(), getBrokerId()
                                , msg -> log.info("{},\"order\":{},\"action\":\"{}\"",
                                        msg
                                        , orderUpdateEvent.getMt4Order()
                                        , orderUpdateEvent.getAction()));

                        BigDecimal balance = orderUpdateEvent.getBalance();

                        BigDecimal credit = orderUpdateEvent.getCredit();
                        if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
                            mt4Account.getBalance().set(balance);
                        }
                        if (credit != null && credit.compareTo(BigDecimal.ZERO) != 0) {
                            mt4Account.getCredit().set(credit);
                        }
                        if (mt4Order != null) {
                            if (orderUpdateEvent.getAction() == TradingEvent.POSITION_MODIFY) {
                                if ((balance == null || balance.equals(BigDecimal.ZERO))
                                    && mt4Order.getSwap() != null
                                    && !mt4Order.getSwap().equals(BigDecimal.ZERO)) {
                                    final BigDecimal newRecalculatedBalance
                                            = mt4Account.getBalance().get().add(mt4Order.getSwap());
                                    orderUpdateEvent.setBalance(newRecalculatedBalance);
                                    mt4Account.getBalance().set(newRecalculatedBalance);
                                    eventProducer.eventConsumer(EventTopic.ACCOUNT_UPDATE)
                                            .accept(TradingMt4Api.this, getAccountData());
                                    return;
                                }
                            } else if (orderUpdateEvent.getAction() == TradingEvent.BALANCE
                                       || orderUpdateEvent.getAction() == TradingEvent.CREDIT) {
                                eventProducer.eventConsumer(EventTopic.ACCOUNT_UPDATE)
                                        .accept(TradingMt4Api.this, getAccountData());
                                return;
                            }
                        }
                        orderComponent.onOrderUpdate(mt4Account, orderUpdateEvent);
                    });
                } catch (Exception e) {
                    log.error("Error on order update. broker {} account {}", getBrokerId(), getAccountId(), e);
                }
            });
        }


        public void onConGroup(byte[] buf) {
            eventProducer.execute(EventTopic.CUSTOM, () -> {
                ConGroupDto conGroupDto = ConGroupBufParser.parseConGroup(buf, mt4Account.getConGroupDto().get().getGroupName());
                if (conGroupDto != null) {
                    mt4Account.getConGroupDto().set(conGroupDto);
                    try {
                        if (conGroupDto.getConGroupMargin() != null)
                            instrumentComponent.updateAllSymbolsMargin(conGroupDto.getConGroupMargin()
                                    , conGroupDto.getSecMarginsTotal());
                    } catch (Exception e) {
                        log.error("Error on margin update. broker {} account {} ", getBrokerId(), getAccountId(), e);
                    }
                }
            });

        }

        public void onTransactionResponse(byte[] buf) throws Exception {
            transactionService.receiveOrderNotify(buf);
        }


        public void onSubscribe() {
            try {
                quoteComponent.onSubscribe();
            } catch (Exception e) {
                log.error("Error on subscribe. Broker {}, Account {}", getBrokerId(), getAccountId(), e);
            }
        }
    }

    private void sendToBroker(OrderRequest request, byte[] buf) throws Exception {
        requestCache.put(getRequestKey(request.getRequestId()), request);
        if (request.getTID() != null) {
            requestCache.put(getRequestKey(request.getTID()), request);
        }
        transactionService.executeRequest(buf, request.getRequestId());
    }

    class TransactionService {

        private byte[] createTransactionPacket(byte[] orderRequest, Integer requestId) throws Exception {
            final Session session = cw.getSession();
            int i;
            // fill Symbol
            for (i = 0; i < 12; i++)
                if (orderRequest[i + 13] == 0)
                    break;
            int seed = (int) TimeUtil.tick();
            for (i += 1; i < 12; i++) {
                seed = seed * 214013 + 2531011;
                orderRequest[i + 13] = (byte) ((seed >> 16) & 0xFF);
            }
            // fill Comment
            for (i = 0; i < 32; i++)
                if (orderRequest[i + 57] == 0)
                    break;
            seed = (int) TimeUtil.tick();
            for (i += 1; i < 32; i++) {
                seed = seed * 214013 + 2531011;
                orderRequest[i + 57] = (byte) ((seed >> 16) & 0xFF);
            }
            // calculate CRC32
            int crc32 = (int) (session.account + ((session.session >> 8) & 0xFF));
            byte[] data = new byte[92];
            System.arraycopy(orderRequest, 1, data, 0, 92);
            crc32 = vCRC32Java.calculate(data, crc32);
            ByteArrayUtil.copyIntToByteArray(crc32, orderRequest, 93);
            byte[] req = new byte[orderRequest.length + 4];
            System.arraycopy(orderRequest, 0, req, 0, orderRequest.length);
            if (session.serverBuild > 1101) {
                req[0] = (byte) 0xD1;
            }
            ByteArrayUtil.copyIntToByteArray(requestId, req, 97);
            if (session.session == 0)
                return req;
            seed = (int) (session.account + ((session.session >> 8) & 0xFF) + ConnectionData.CURRENT_BUILD + session.serverBuild);
            seed = seed * 214013 + 2531011;
            int szRandOpen = (int) ((seed >> 16) & 0x3) + 16;
            seed = seed * 214013 + 2531011;
            int szRandClose = (int) ((seed >> 16) & 0x7) + 8;
            int newSize = req.length + szRandOpen + 20 + 4 + szRandClose;
            data = new byte[newSize];
            System.arraycopy(req, 0, data, 0, req.length);
            System.arraycopy(req, 0, data, 0, req.length);
            for (i = 0; i < szRandOpen; i++) {
                seed = seed * 214013 + 2531011;
                data[i + req.length] = (byte) ((seed >> 16) & 0xFF);
            }
            int szPack = req.length + szRandOpen;
            byte[] bufHash = new byte[szPack - 1];
            System.arraycopy(data, 1, bufHash, 0, szPack - 1);
            vSHA1Java sha = new vSHA1Java();
            sha.hashData(bufHash);
            sha.hashData(ByteArrayUtil.intToByteArray(session.account));
            sha.hashData(MT4Crypt.decode(session.transactionKey1, session.getHardIdByte()));
            sha.hashData(MT4Crypt.decode(session.transactionKey2, session.getHardIdByte()));
            sha.hashData(session.getHardIdByte());
            System.arraycopy(sha.finalizeHash(), 0, data, szPack, sha.finalizeHash().length);
            szPack += 20;
            ByteArrayUtil.copyIntToByteArray(session.session ^ ConnectionData.CLIENT_EXE_SIZE, data, szPack);
            szPack += 4;
            for (i = 0; i < szRandClose; i++) {
                seed = seed * 214013 + 2531011;
                data[i + szPack] = (byte) ((seed >> 16) & 0xFF);
            }
            ByteArrayUtil.copyIntToByteArray((int) 0, orderRequest, 93);

            return data;
        }


        private void orderNotify(Integer requestId, byte[] buf) throws Exception {

            byte code = buf[5];
            byte tradeType = buf[4];
            var request = requestCache.getIfPresent(getRequestKey(requestId));
            switch (code) {
                case (byte) 0x8E:
//                    sendOrderEvent(EventTopic.ORDER_REQUEST, request, OrderStateData.Initial
//                            , request != null ? request.getType() : null
//                            , null, null);
                    break;
                case (byte) 0x8F:
//                    sendOrderEvent(EventTopic.ORDER_REQUEST, request, OrderStateData.Processing
//                            , request != null ? request.getType() : null
//                            , null, null);
                    break;
                case (byte) 0x8A:
                    buf = cw.getConnection().receiveDecode(16);
                    double bid = ByteUtil.getDouble(buf, 0);
                    double ask = ByteUtil.getDouble(buf, 8);
                    sendOrderEvent(EventTopic.ORDER_REQUEST, request, OrderStateData.Rejected
                            , request != null ? request.getType() : null
                            , null, new RequoteException(bid, ask));
                    break;
                case 0:
                    processRequest(request, tradeType);
                    break;
                case 1:
                    break;
                default:
                    String message;
                    if (request == null) {
                        message = String.format("Order was rejected. Request: %s. Account: %s", requestId, getAccountId());
                    } else {
                        message = String.format(
                                "Order was rejected on %s. Ticket: %s. Symbol: %s. Lot: %s. Price: %s. Account: %s. ",
                                request.getRequestType(),
                                request.getTicket(), request.getSymbol(),
                                request.getLot(), request.getPrice(), getAccountId()
                        );
                    }
                    sendErrorOrderEvent(EventTopic.ORDER_REQUEST, request, OrderStateData.Rejected
                            , request != null ? request.getType() : null
                            , null, new CodeException(message, StatusCode.codeByNumber(Byte.toUnsignedInt(code))));

            }
        }

        public void receiveOrderNotify(byte[] buf) throws Exception {
            try {
                int id = ByteUtil.getInt(buf, 0);
                orderNotify(id, buf);
            } catch (Exception e) {
                log.error("Error on order notify. broker {} account {}", getBrokerId(), getAccountId(), e);
            }

        }

        private void processRequest(OrderRequest request, byte tradeType) throws Exception {
            byte[] buf;
            final Connection connection = cw.getConnection();
            switch (tradeType) {
                case 0:
                    buf = connection.receiveDecode(16);
                    processing(request, new byte[0], OrderStateData.Price);
                    break;
                case 0x40: //instant order
                case 0x41: //order by request
                case 0x42: //order %s market
                    buf = connection.readCompressed();
                    processing(request, buf, OrderStateData.Opened);
                    break;
                case 0x43: //pending order
                    buf = connection.readCompressed();
                    processing(request, buf, OrderStateData.Placed);
                    break;
                case 0x44:
                case 0x45:
                case 0x46:
                    connection.receiveDecode(8);
                    connection.receiveDecode(8);
                    buf = connection.readCompressed();
                    processing(request, buf, OrderStateData.Closed);
                    break;
                case 0x47:
                    buf = connection.readCompressed();
                    processing(request, buf, OrderStateData.Modified);
                    break;
                case 0x48:
                    processing(request, new byte[0], OrderStateData.Cancelled);
                    break;
                case 0x49:
                    connection.receiveDecode(8);
                    connection.receiveDecode(8);
                    connection.readCompressed();
                    processing(request, new byte[0], OrderStateData.ClosedBy);
                    break;
                default:
                    sendErrorOrderEvent(EventTopic.ORDER_REQUEST, request, OrderStateData.Rejected
                            , request != null ? request.getType() : null
                            , null, new CodeException(UNKNOWN_ORDER_TYPE));

            }
        }

        private void processing(OrderRequest request, byte[] buf, OrderStateData orderStateData) {
            Mt4Order mt4Order = null;
            if (buf.length > 1) {
                try {
                    mt4Order = OrderBufParser.parse(buf, 0, timeComponent);
                } catch (Exception e) {
                    log.error("Error parsing order {}", orderStateData, e);
                }
            }
            sendOrderEvent(EventTopic.ORDER_REQUEST, request, orderStateData
                    , request != null ? request.getType() : null
                    , mt4Order, null);
        }


        public void executeRequest(byte[] buf, final int requestId) throws Exception {
            long time = System.currentTimeMillis();
            try {
                byte[] pack = createTransactionPacket(buf, requestId);
                if (pack.length > 1) {
                    try {
                        cw.getConnection().send(pack);
                    } catch (Exception e) {
                        throw new NotConnectedException();
                    }
                }
            } finally {
                if (metricService != null) {
                    metricService.record("transport_send", time - System.currentTimeMillis());
                }
            }
        }
    }

    interface ProtocolCommand {
        void execute();

        long getTimestamp();
    }

    @Data
    final static class OrderMarket implements ProtocolCommand {
        final DataRef dataRef;
        final OrderRequest request;
        double sl;
        double tp;
        final long timestamp = System.currentTimeMillis();

        public OrderMarket(DataRef dataRef, OrderRequest request) {
            this.dataRef = dataRef;
            this.request = request;
            this.sl = request.getSl() != null ? request.getSl().doubleValue() : 0;
            this.tp = request.getTp() != null ? request.getTp().doubleValue() : 0;
        }

        @Override
        public void execute() {
            try {
                request.setSendTime(System.currentTimeMillis());
                Long tid = toLong(request.getTID());
                dataRef.commandComponent().tryMarketOrder(request
                        , request.getSymbol()
                        , orderType(request.getType())
                        , request.getLot().doubleValue()
                        , request.getPrice() != null ? request.getPrice().doubleValue() : 0
                        , 100
                        , sl
                        , tp
                        , request.getComment()
                        , tid.intValue());

            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                dataRef.errorNotifier().notifyOrderError(EventTopic.ORDER_REQUEST
                        , request
                        , OrderStateData.Rejected
                        , request.getType()
                        , null, codeException);
            }
        }

    }

    @Data
    final static class OrderModify implements ProtocolCommand {
        final DataRef dataRef;
        OrderRequest request;
        OrderData order;
        final long timestamp = System.currentTimeMillis();

        public OrderModify(DataRef dataRef
                , OrderRequest request, OrderData order) {
            this.dataRef = dataRef;
            this.request = request;
            this.order = order;
        }

        @Override
        public void execute() {
            try {
                request.setSendTime(System.currentTimeMillis());
                dataRef.commandComponent().orderModify(request, order);

            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                dataRef.errorNotifier().notifyOrderError(EventTopic.ORDER_REQUEST
                        , request
                        , OrderStateData.Rejected
                        , request.getType()
                        , order, codeException);
            }
        }

    }


    @Data
    final static class OrderClose implements ProtocolCommand {
        final DataRef dataRef;
        OrderRequest request;
        OrderData order;
        final long timestamp = System.currentTimeMillis();


        public OrderClose(DataRef dataRef
                , OrderData order
                , OrderRequest request) {
            this.dataRef = dataRef;
            this.order = order;
            this.request = request;
        }

        @Override
        public void execute() {

            try {
                request.setSendTime(System.currentTimeMillis());
                dataRef.commandComponent().orderClose(request, order);

            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                dataRef.errorNotifier().notifyOrderError(EventTopic.ORDER_REQUEST
                        , request
                        , OrderStateData.Rejected
                        , request.getType()
                        , order, codeException);
            }


        }

    }

    @Data
    final static class OrderDelete implements ProtocolCommand {
        final DataRef dataRef;
        OrderRequest request;
        OrderData orderData;
        final long timestamp = System.currentTimeMillis();

        public OrderDelete(DataRef dataRef
                , OrderRequest request
                , OrderData orderData
        ) {
            this.dataRef = dataRef;
            this.request = request;
            this.orderData = orderData;
        }

        @Override
        public void execute() {
            try {
                request.setSendTime(System.currentTimeMillis());
                dataRef.commandComponent().orderDelete(request, orderData);
            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                dataRef.errorNotifier().notifyOrderError(EventTopic.ORDER_REQUEST
                        , request
                        , OrderStateData.Rejected
                        , request.getType()
                        , orderData, codeException);
            }
        }

    }

    @Data
    final static class OrderPending implements ProtocolCommand {
        final DataRef dataRef;
        OrderRequest request;
        final long timestamp = System.currentTimeMillis();

        public OrderPending(DataRef dataRef
                , OrderRequest request) {
            this.dataRef = dataRef;
            this.request = request;
        }

        @Override
        public void execute() {
            try {
                request.setSendTime(System.currentTimeMillis());
                dataRef.commandComponent().newPendingOrder(request);

            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                dataRef.errorNotifier().notifyOrderError(EventTopic.ORDER_REQUEST
                        , request
                        , OrderStateData.Rejected
                        , request.getType()
                        , null, codeException);
            }
        }

    }

    final class CommandComponent {
        public void orderClose(OrderRequest request, OrderData order) throws Exception {
            String comment = request.getComment();
            int slippage = 100;
            var reqAmount = request.getLot();
            if (reqAmount == null || reqAmount.compareTo(BigDecimal.ZERO) <= 0) {
                reqAmount = order.getLot();
            }
            double volume = reqAmount.doubleValue();
            double price = order.getPrice().doubleValue();
            int magic = request.getTID() != null ? toInt(request.getTID()) : 0;
            String symbol = order.getSymbol();
            request.setSymbol(symbol);
            Cmd cmd = Cmd.CLOSE_INSTANT;
            final Instrument instrument = instrumentComponent.getInstrument(symbol);
            boolean market = instrument.getExeMode() == ExecutionType.MARKET.value();
            if (market) {
                cmd = Cmd.CLOSE_MARKET;
                price = 0;
                slippage = 0;
            } else {
                int operation = order.getOrderType() == OrderTypeData.Buy
                        ? OrderType.SELL.getValue()
                        : OrderType.BUY.getValue();
                price = getPrice(symbol, operation, price);
                if (slippage != 0) {
                    final ConGroupSecDto conGroupSecDto = getSymbolGroupParams(instrument.getSymbolGroup());
                    int ie = conGroupSecDto.getMaxPriceDeviation();
                    if (ie != 0 && slippage > ie)
                        slippage = ie;
                }
            }
            if (magic > 0) {
                magic = -1 * magic;
            }
            int lots = (int) Math.round(volume / 0.01);
            byte[] buf = OrderSerializer.get(cw.getConnectionData().user, order.getTicket().intValue(), cmd.value(), OrderType.BUY.getValue(), symbol, lots,
                    price, 0, 0, slippage, comment, magic, (int) System.currentTimeMillis());
            sendToBroker(request, buf);
        }


        public void orderModify(OrderRequest request,
                                OrderData order) throws Exception {

            BigDecimal price = request.getPrice() != null ? request.getPrice() : order.getPrice();

            var sl = request.getSl() == null
                    ? order.getSl() == null ? 0 : order.getSl().doubleValue()
                    : request.getSl().doubleValue();
            var tp = request.getTp() == null
                    ? order.getTp() == null ? 0 : order.getTp().doubleValue()
                    : request.getTp().doubleValue();
            var type = orderType(order.getOrderType());
            final byte[] buf = OrderSerializer.get(cw.getConnectionData().user
                    , request.getTicket().intValue(), Cmd.MODIFY.value()
                    , type, order.getSymbol()
                    , order.getLot().intValue(), price.doubleValue()
                    , sl, tp
                    , 0, null
                    , 0, 0);
            sendToBroker(request, buf);
        }

        public void newPendingOrder(OrderRequest request) throws Exception {
            String symbol = request.getSymbol();
            BigDecimal lot = request.getLot();
            BigDecimal orderRate = request.getPrice();
            BigDecimal slRate = request.getSl();
            BigDecimal tpRate = request.getTp();
            String comment = request.getComment();
            orderSend(request, symbol
                    , orderType(request.getType())
                    , lot.doubleValue()
                    , orderRate.doubleValue()
                    , 0
                    , slRate != null ? slRate.doubleValue() : 0
                    , tpRate != null ? tpRate.doubleValue() : 0
                    , comment
                    , request.getTID() == null ? 0 : toInt(request.getTID())
                    , 0);
        }

        public void orderSend(OrderRequest request, String symbol, int operation, double volume, double price,
                              int slippage, double stopLoss, double takeProfit, String comment,
                              int magic, int expiration
        ) throws Exception {
            Cmd cmd = Cmd.INSTANT;
            if (operation == OrderType.BUY_LIMIT.getValue() || operation == OrderType.BUY_STOP.getValue()
                || operation == OrderType.SELL_LIMIT.getValue() || operation == OrderType.SELL_STOP.getValue()) {
                cmd = Cmd.PENDING;
            } else {
                final Instrument instrument = instrumentComponent.getInstrument(symbol);
                boolean market = instrument.getExeMode() == ExecutionType.MARKET.value();
                if (market) {
                    cmd = Cmd.MARKET;
                    price = 0;
                    slippage = 0;
                } else {
                    if (slippage != 0) {
                        final ConGroupSecDto conGroupSecDto = getSymbolGroupParams(instrument.getSymbolGroup());
                        int ie = conGroupSecDto.getMaxPriceDeviation();
                        if (ie != 0 && slippage > ie)
                            slippage = ie;
                    }
                }
            }
            int lots = (int) Math.round(volume / 0.01);

            if (magic > 0) {
                magic = -1 * magic;
            }
            byte[] buf = OrderSerializer.get(cw.getConnectionData().user, 0, cmd.value(), operation, symbol, lots, price,
                    stopLoss, takeProfit, slippage, comment, magic, expiration);
            sendToBroker(request, buf);

        }


        public void orderDelete(OrderRequest request, OrderData order) throws Exception {
            request.setSymbol(order.getSymbol());
            request.setLot(order.getLot());
            request.setPrice(order.getPrice());
            request.setType(order.getOrderType());
            var ticket = order.getTicket().intValue();
            var type = orderType(order.getOrderType());
            var symbol = order.getSymbol();
            var volume = order.getLot().doubleValue();
            var price = order.getPrice().doubleValue();

            int lots = (int) Math.round(volume / 0.01);
            byte[] buf = OrderSerializer.get(cw.getConnectionData().user, ticket, Cmd.DELETE_PENDING.value(),
                    type, symbol, lots, price, 0, 0, 0, null, 0, (int) System.currentTimeMillis());
            sendToBroker(request, buf);

        }

        private void tryMarketOrder(OrderRequest request, String symbol, int operation, double volume, double price
                , int slippage, double stopLoss, double takeProfit
                , String comment, int magic) throws Exception {
            Cmd cmd = Cmd.INSTANT;
            Instrument instrument = instrumentComponent.getInstrument(symbol);
            boolean market = instrument.getExeMode() == ExecutionType.MARKET.value();
            if (market) {
                cmd = Cmd.MARKET;
                price = 0;
                slippage = 0;
            } else {
                price = getPrice(symbol, operation, price);
            }
            int lots = (int) Math.round(volume / 0.01);
            if (magic > 0) {
                magic = -1 * magic;
            }
            byte[] buf = OrderSerializer.get(cw.getConnectionData().user
                    , 0, cmd.value(), operation, symbol, lots, price,
                    stopLoss, takeProfit, slippage, comment, magic, 0);
            sendToBroker(request, buf);

        }


    }

    class OrderHistoryClient {
        public List<Mt4Order> load(OrderHistoryTask orderHistoryTask) {
            final ConnectionWrapper cw = TradingMt4Api.this.cw.copyAndReset();
            try {
                HostPort hostPort = hostAndPortService.current();
                AuthUtil.connectAndLogin(hostPort, cw);
                List<Mt4Order> result = new ArrayList<>();
                OrderHistoryUtil.read(cw.getConnection()
                        , orderHistoryTask.from, orderHistoryTask.to
                        , result::add
                        , timeComponent);
                return result;
            } catch (Exception ignored) {
            } finally {
                try {
                    cw.getConnection().disconnect();
                } catch (Exception ignored) {
                }
            }
            return List.of();
        }


    }

    @Getter
    class OrderHistoryComponent {
        public List<Mt4Order> loadOrderHistory(LocalDateTime from, LocalDateTime to) {
            return orderHistoryClient.load(new OrderHistoryTask(from, to));
        }
    }

    @Getter
    class OrderComponent {
        final Map<Integer, OrderData> openedOrders = new ConcurrentHashMap<>();
        final Map<Integer, OrderData> pendingOrders = new ConcurrentHashMap<>();
        final Map<Integer, CloseOrderData> closedOrders = new ConcurrentHashMap<>();
        private final boolean autoSubscribe;

        public OrderComponent(boolean autoSubscribe) {
            this.autoSubscribe = autoSubscribe;
        }

        public void setClosedOrders(List<CloseOrderData> closedOrders) {
            closedOrders.forEach(newOrder -> orderComponent.closedOrders.put(newOrder.getTicket().intValue(), newOrder));
        }

        public void onOrderUpdate(Mt4Account mt4Account, OrderUpdateEvent orderUpdateEvent) {


            try {
                Mt4Order newMt4Order = orderUpdateEvent.getMt4Order();
                var clOrdId = newMt4Order.getClOrdId();
                OrderRequest request = null;
                if (clOrdId > 0) {
                    request = requestCache.getIfPresent(getRequestKey(clOrdId));
                }
                final int ticket = newMt4Order.getTicket();
                OrderData oldOpenedOrder = openedOrders.get(ticket);
                OrderData oldPendingOrder = pendingOrders.get(ticket);
                if (orderUpdateEvent.getAction() == TradingEvent.POSITION_OPEN) {
                    addOpenedOrder(newMt4Order);
                    if (oldOpenedOrder == null) {
                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Opened
                                , HelpUtil.toOrderTypeData(newMt4Order.getOrderType())
                                , newMt4Order, null);
                    }
                } else if (orderUpdateEvent.getAction().equals(TradingEvent.POSITION_CLOSE)) {
                    if (oldOpenedOrder != null) {
                        openedOrders.remove(ticket);
                        addClosedOrder(newMt4Order, closedOrders);
                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Closed
                                , HelpUtil.toOrderTypeData(newMt4Order.getOrderType())
                                , newMt4Order, null);
                    }
                } else if (orderUpdateEvent.getAction() == TradingEvent.PENDING_OPEN) {
                    addOrder(newMt4Order, pendingOrders);
                    sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Placed
                            , HelpUtil.toOrderTypeData(newMt4Order.getOrderType())
                            , newMt4Order, null);
                } else if (orderUpdateEvent.getAction().equals(TradingEvent.POSITION_MODIFY)) {
                    if (oldPendingOrder != null) {
                        addOpenedOrder(newMt4Order);
                        fillPending(ticket, oldPendingOrder, newMt4Order);
                    } else if (oldOpenedOrder != null) {
                        addOpenedOrder(newMt4Order);
                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Modified
                                , HelpUtil.toOrderTypeData(newMt4Order.getOrderType())
                                , newMt4Order, null);
                    }
                } else if (orderUpdateEvent.getAction().equals(TradingEvent.PENDING_CLOSE)) {
                    pendingOrderClose(ticket, newMt4Order);
                } else if (orderUpdateEvent.getAction().equals(TradingEvent.PENDING_MODIFY)) {
                    addOrder(newMt4Order, pendingOrders);
                    sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Modified
                            , HelpUtil.toOrderTypeData(newMt4Order.getOrderType())
                            , newMt4Order, null);
                }
            } catch (Exception e) {
                LogUtil.log("onOrderUpdate", "api", mt4Account.accountId, mt4Account.brokerId,
                        msg -> log.error(msg, e)
                );
            }
        }

        private void fillPending(int ticket, OrderData oldOrder, Mt4Order newOrder) {
            OrderType currType = newOrder.getOrderType();
            if (oldOrder != null && (((oldOrder.getOrderType() == OrderTypeData.BuyLimit
                                       || oldOrder.getOrderType() == OrderTypeData.BuyStop)
                                      && currType == OrderType.BUY)
                                     ||
                                     ((oldOrder.getOrderType() == OrderTypeData.SellLimit
                                       || oldOrder.getOrderType() == OrderTypeData.SellStop)
                                      && currType == OrderType.SELL))) {
//                sendOrderEvent(EventTopic.ORDER, null, OrderStateData.Filled
//                        , HelpUtil.toOrderTypeData(newOrder.getOrderType())
//                        , newOrder, null);
                pendingOrderClose(ticket, newOrder);
                sendOrderEvent(EventTopic.ORDER, null, OrderStateData.Opened
                        , HelpUtil.toOrderTypeData(newOrder.getOrderType())
                        , newOrder, null);
            }
        }

        private void pendingOrderClose(int ticket, Mt4Order newMt4Order) {
            pendingOrders.remove(ticket);
            sendOrderEvent(EventTopic.ORDER, null, OrderStateData.Cancelled
                    , HelpUtil.toOrderTypeData(newMt4Order.getOrderType())
                    , newMt4Order, null);
        }

        boolean compare(OrderData oldOrder, OrderData newOrder) {
            return Objects.equals(oldOrder.getTicket(), newOrder.getTicket())
                   && Objects.equals(oldOrder.getSymbol(), newOrder.getSymbol())
                   && Objects.equals(oldOrder.getPrice(), newOrder.getPrice())
                   && Objects.equals(oldOrder.getProfit(), newOrder.getProfit())
                   && Objects.equals(oldOrder.getCommission(), newOrder.getCommission())
                   && Objects.equals(oldOrder.getTaxes(), newOrder.getTaxes())
                   && Objects.equals(oldOrder.getLot(), newOrder.getLot())
                   && Objects.equals(oldOrder.getComment(), newOrder.getComment())
                   && Objects.equals(oldOrder.getOrderType(), newOrder.getOrderType())
                   && Objects.equals(oldOrder.getSl(), newOrder.getSl())
                   && Objects.equals(oldOrder.getTp(), newOrder.getTp());
        }

        private void addOpenedOrder(Mt4Order mt4Order) {
            addOrder(mt4Order, openedOrders);
            try {
                if (autoSubscribe)
                    quoteComponent.tradingSubscribe(mt4Order.getSymbol());
            } catch (Exception ignored) {
            }
        }


        private void addOrder(Mt4Order mt4Order
                , Map<Integer, OrderData> orders
        ) {
            if (mt4Order.getOrderType() == OrderType.BALANCE || mt4Order.getOrderType() == OrderType.CREDIT)
                return;
            OrderData order = toOrderData(mt4Order);
            orders.put(mt4Order.getTicket(), order);
        }

        private void addClosedOrder(Mt4Order mt4Order
                , Map<Integer, CloseOrderData> orders
        ) {
            CloseOrderData order = toCloseOrderData(mt4Order);
            orders.put(mt4Order.getTicket(), order);
        }

        public void update(List<Mt4Order> incomeMt4Orders) {
            incomeMt4Orders.forEach(mt4Order -> {
                if (mt4Order.getOrderType() == OrderType.BUY || mt4Order.getOrderType() == OrderType.SELL) {
                    addOpenedOrder(mt4Order);
                } else {
                    addOrder(mt4Order, pendingOrders);
                }
            });
        }

        public void clear() {
            openedOrders.clear();
            pendingOrders.clear();
            closedOrders.clear();
        }


    }

    public class QuoteComponent {
        static final float MINIMAL_RATE = 0.0001f;

        final Set<Short> subscriptions = Sets.newConcurrentHashSet();
        final Set<Short> tradingSubscriptions = Sets.newConcurrentHashSet();
        final AtomicReference<Short> criticalSubscription = new AtomicReference<>();
        final Map<String, QuoteData> quotes = new ConcurrentHashMap<>();
        final AtomicBoolean request = new AtomicBoolean();

        static BigDecimal round(double num, Integer digit) {
            return BigDecimal.valueOf(num).setScale(digit, RoundingMode.HALF_UP);
        }

        public void onQuote(byte[] buf) {
            eventProducer.execute(EventTopic.PRICE, () -> {
                int size = buf.length / 14;
                final byte[] bytes = new byte[14];
                for (int i = 0; i < size; i++) {
                    try {
                        System.arraycopy(buf, i * 14, bytes, 0, 14);
                        final short code = ByteUtil.getShort(bytes, 0);
                        long t = ByteUtil.getInt(bytes, 2);
                        float ask = ByteUtil.getFloat(bytes, bytes.length - 4);
                        if (ask < MINIMAL_RATE) {
                            continue;
                        }
                        float bid = ByteUtil.getFloat(bytes, bytes.length - 8);
                        if (bid < MINIMAL_RATE) {
                            continue;
                        }
                        if (subscriptions.contains(code)) {
                            produce(EventTopic.PRICE, of(code, bid, ask, timeComponent.timeNow(t)));
                        }
                    } catch (Exception e) {
                        log.error("Failed to process quote", e);
                    }
                }
            });

        }


        private QuoteData of(short code, float bid, float ask, long t) {
            try {
                Instrument instrument = instrumentComponent.getInstrumentsById().get(code);
                if (instrument != null) {
                    QuoteData quote = new QuoteData();
                    quote.setCode(code);
                    quote.setBrokerId(TradingMt4Api.this.getBrokerId());
                    quote.setAccountId(TradingMt4Api.this.getAccountId());
                    final ConGroupSecDto conGroupSecDto = getSymbolGroupParams(instrument.getSymbolGroup());
                    final int dif = conGroupSecDto == null ? 0 : conGroupSecDto.getSpreadDiff();
                    final double halfPoint = instrument.getPointHalf();
                    if (dif > 1) {
                        quote.setBid(round(bid - (dif * halfPoint), instrument.getDigits()));
                        quote.setAsk(round(ask + (dif * halfPoint), instrument.getDigits()));
                    } else {
                        quote.setBid(toBigDecimal(bid));
                        quote.setAsk(toBigDecimal(ask));
                    }
                    quote.setTime(t);
                    quote.setTradable(instrument.getTradeMode() == 2);
                    quote.setSymbol(instrument.getSymbol());
                    quotes.put(quote.getSymbol(), quote);
                    return quote;
                }
            } catch (Exception e) {
                log.error("Failed to process quote", e);
            }
            return null;
        }

        private BigDecimal toBigDecimal(float value) {
            return new BigDecimal(Float.toString(value));
        }

        public QuoteData getQuote(String symbol) {
            return quotes.get(symbol);
        }

        public void clear() {
            quotes.clear();
        }

        public void onSubscribe() throws IOException {
            if (request.getAndSet(false)) {
                request(cw.getConnection(), concatSubs());
            }
        }

        public void unsubscribe(String... symbols) throws Exception {
            generalUnsubscribe(subscriptions, symbols);
        }

        void tradingSubscribe(String... symbols) throws Exception {
            generalSubscribe(tradingSubscriptions, symbols);
        }

        void tradingUnsubscribe(String... symbols) throws Exception {
            generalUnsubscribe(tradingSubscriptions, symbols);
        }

        public void subscribe(String... symbols) throws Exception {
            generalSubscribe(subscriptions, symbols);
        }

        private void generalSubscribe(Set<Short> subs, String... symbols) throws Exception {
            manageSubscriptions(code -> {
                criticalSubscription.getAndSet(code);
                return subs.add(code);
            }, symbols);
        }

        private void generalUnsubscribe(Set<Short> subs, String... symbols) throws Exception {
            manageSubscriptions(subs::remove, symbols);
        }

        private boolean isNeedUpdate(String symbol, Function<Short, Boolean> logic) throws Exception {
            final Instrument instrument = instrumentComponent.getInstrument(symbol);
            if (instrument != null) {
                return logic.apply(instrument.getSymbolIndex());
            }
            return false;
        }

        private void manageSubscriptions(Function<Short, Boolean> logic, String... symbols) throws Exception {
            boolean process = false;
            for (String symbol : symbols) {
                if (isNeedUpdate(symbol, logic)) {
                    process = true;
                }
            }
            if (process)
                request.getAndSet(true);
        }


        public void clearAndSubscribe(String... symbols) throws Exception {
            subscriptions.clear();
            if (symbols.length > 0)
                subscribe(symbols);
        }


        private Set<Short> concatSubs() {
            if (subscriptions.isEmpty()
                && tradingSubscriptions.isEmpty()
                && criticalSubscription.get() != null
            ) {
                return Set.of(criticalSubscription.get());
            } else {
                return new HashSet<>() {
                    {
                        addAll(subscriptions);
                        addAll(tradingSubscriptions);
                    }
                };
            }
        }

        private void request(Connection connection, Set<Short> allSubscriptions) throws IOException {
            byte[] buf = new byte[3 + allSubscriptions.size() * 2];
            buf[0] = (byte) 0x96;                          //request quotes
            ByteArrayUtil.copyShortToByteArray((short) allSubscriptions.size(), buf, 1);
            int ind = 3;
            for (short code : allSubscriptions) {
                try {
                    ByteArrayUtil.copyShortToByteArray(code, buf, ind);
                    ind += 2;
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to subscribe");
                }
            }
            connection.send(buf);
        }

        public boolean isSubscribed(String symbol) throws Exception {
            final short code = instrumentComponent.getCode(symbol);
            return subscriptions.contains(code) || tradingSubscriptions.contains(code);
        }


        public String[] getAllSubscriptions() {
            return concatSubs().stream()
                    .map(instrumentComponent::getSymbol)
                    .toArray(String[]::new);
        }


        public void resubscribe() {
            request.getAndSet(true);
        }
    }

    @Data
    class MessageHandlerService {
        private final AtomicLong lastMessageTime = new AtomicLong();
        private final AtomicLong lastServerPingTime = new AtomicLong();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final MpscArrayQueue<ProtocolCommand> queue = new MpscArrayQueue<>(256);
        private static final long TIMEOUT_MS = 50;

        private void runLoop() {
            ProtocolCommand command;
            final long startTime = System.currentTimeMillis();
            while (running.get()
                   && !Thread.currentThread().isInterrupted()
                   && (command = queue.poll()) != null
                   && (System.currentTimeMillis() - startTime) < TIMEOUT_MS) {
                command.execute();
                if (metricService != null)
                    metricService.record("api_queue", System.currentTimeMillis() - command.getTimestamp());
            }
        }


        public void start() {
            running.set(true);
            setStatus(ConnectionStatus.ONLINE);

            produce(EventTopic.CONNECT, new ConnectEvent(getBrokerId(), getAccountId()));
            produce(EventTopic.LOAD, new SnapshotEvent<>(EventType.LOAD_INSTRUMENT, getBrokerId(), getAccountId(), getAllContractData()));
            produce(EventTopic.LOAD, new SnapshotEvent<>(EventType.LOAD_CLOSE, getBrokerId(), getAccountId(), closed()));
            produce(EventTopic.LOAD, new SnapshotEvent<>(EventType.LOAD_OPEN, getBrokerId(), getAccountId(), opened()));
            produce(EventTopic.LOAD, new SnapshotEvent<>(EventType.LOAD_PENDING, getBrokerId(), getAccountId(), pending()));
            produce(EventTopic.LOAD, toAccountSnapshot());

            messageHandlerExecutor.execute(() -> {
                int count = 0;

                long pingTime1 = ping1();
                long pingTime2 = ping2();
                try {
                    while (cw.getConnection().isConnected() && running.get() && !Thread.currentThread().isInterrupted()) {
                        final long currentTime = System.currentTimeMillis();

                        if (currentTime > pingTime2) {
                            cw.getConnection().ping();
                            pingTime2 = ping2();
                        }

                        messageHandlerListener.onSubscribe();
                        runLoop();

                        if (currentTime > pingTime1) {
                            cw.getConnection().ping();
                        }

                        if (cw.getConnection().available() > 0) {
                            byte[] buf = cw.getConnection().receiveDecode(1);
                            count = 0;
                            lastMessageTime.set(System.currentTimeMillis());
                            pingTime1 = ping1();
                            byte cmd = buf[0];

                            switch (cmd) {
                                case (byte) 0x97:
                                    buf = cw.getConnection().receiveDecode(1);
                                    buf = cw.getConnection().receiveDecode(Byte.toUnsignedInt(buf[0]) * 14);
                                    messageHandlerListener.onQuote(buf);
                                    break;
                                case (byte) 0x98:
                                    receiveNews(cw.getConnection());
                                    break;
                                case (byte) 0x99:
                                    cw.getConnection().readCompressed();
                                    break;
                                case (byte) 0x9A:
                                    buf = cw.getConnection().readCompressed();
                                    messageHandlerListener.onInstrument(buf);
                                    break;
                                case (byte) 0x9B:
                                    buf = cw.getConnection().readCompressed();
                                    messageHandlerListener.onOrders(buf);
                                    break;
                                case (byte) 0x9C:
                                    buf = cw.getConnection().receiveDecode(24);
                                    cw.getConnection().receiveDecode(ByteUtil.getInt(buf, buf.length - 4) * 28);
                                    break;
                                case (byte) 0x9D:
                                    buf = cw.getConnection().receiveDecode(1);
                                    if (buf[0] == 0) receiveNews(cw.getConnection());
                                    break;
                                case (byte) 0xAB:
                                    buf = cw.getConnection().readCompressed();
                                    messageHandlerListener.onConGroup(buf);
                                    break;
                                case (byte) 0xBE:
                                case (byte) 0xD1:
                                    buf = cw.getConnection().receiveDecode(6);
                                    int id = ByteUtil.getInt(buf, 0);
                                    if (id == 0) throw new CodeException(StatusCode.codeByNumber(buf[5]));
                                    messageHandlerListener.onTransactionResponse(buf);
                                    break;
                                case 0x0D:
                                    throw new CodeException(Code.SERVER_DECIDED_TO_DISCONNECT);
                                case 0x02:
                                    lastServerPingTime.set(System.currentTimeMillis());
                                    break;
                                default:
                                    try {
                                        cw.getConnection().receiveDecode(cw.getConnection().available());
                                    } catch (Exception e) {
                                        throw new DecompressorException("Can't decode unknown message.");
                                    }
                            }
                        } else {
                            if (cw.getConnection().available() <= 0) {
                                Thread.sleep(2);
                                count++;
                                if (count > maxSpinBeforeExtraLoopDelay) {
                                    count = 0;
                                    Thread.sleep(extraLoopDelayInMills);
                                }
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    notifyConnectError(e, EventType.CONNECT);
                }
                running.set(false);
            });
        }

        private long ping1() {
            return System.currentTimeMillis() + 4000;
        }

        private long ping2() {
            return System.currentTimeMillis() + 8000;
        }

    }

    private void receiveNews(Connection con) throws Exception {
        byte[] buf;
        buf = con.receiveDecode(6);
        int len = ByteUtil.getInt(buf, 0);
        if (len > 0x800000)
            return;
        con.receiveDecode(len);
    }

    class DataLoaderService {
        final ConSymbolGroupDto[] groups = new ConSymbolGroupDto[32];

        public void loadAccount(HostPort hostPort) throws Exception {
            Connection connection = cw.getConnection();
            Session session = cw.getSession();
            AuthUtil.connectAndLogin(hostPort, cw);
            byte[] buf = connection.receiveServersList();
            try {
                session.serverDetails = ServerDetails.parse(buf);
            } catch (Exception e) {
                log.error("Error parsing server details", e);
            }
            var event = new LogonEvent(getBrokerId(), getAccountId());
            produce(EventTopic.CONNECT, event);

            session.serverVersion = ByteUtil.getShort(buf, 0);
            session.serverBuild = ByteUtil.getShort(buf, 2);

            loginListener.onLoadInstrument(connection.receiveSymbols());
            byte[] groupResponse = connection.receiveGroups();
            try {
                for (int i = 0; i < 32; i++) {
                    groups[i] = parseConSymbolGroupDto(groupResponse, i * 80);
                }
            } catch (Exception e) {
            }
            connection.receiveMailHistory();
            loginListener.onLoadOrderHistory();
            ConnectionUtil.createAndFillTransactionKey2(session);
            buf = connection.receiveAccount(session);
            loginListener.onLoadAccount(buf);
            loginListener.onLoadOrder(buf, session.serverBuild);
            loginListener.onServerDetails(session.serverDetails);
        }

    }


}
