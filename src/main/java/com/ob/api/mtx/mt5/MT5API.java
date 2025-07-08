package com.ob.api.mtx.mt5;


import com.google.common.cache.Cache;
import com.ob.api.mtx.util.HostAndPortService;
import com.ob.broker.common.*;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.*;
import com.ob.broker.common.model.*;
import com.ob.broker.common.request.IRequest;
import com.ob.broker.common.request.OrderRequest;
import com.ob.broker.service.LocalEntryService;
import com.ob.broker.service.ReconnectService;
import com.ob.broker.util.ErrorUtil;
import com.ob.broker.util.LogUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ob.api.mtx.util.BrokerUtil.eventType;
import static com.ob.broker.common.error.Code.*;
import static com.ob.broker.util.Util.toLong;


@Slf4j
public class MT5API implements ApiConnectAction, ITradeBaseApi {

    final static Quote QUOTE = new Quote();
    public final Symbols symbols = new Symbols();
    final HostAndPortService hostAndPortService = new HostAndPortService();
    final AtomicReference<ApiCredentials> apiCredentials = new AtomicReference<>(
            new Mt5ApiCredentials(0L, 0L, "", List.of(HostPort.DEFAULT), false, List.of()));
    final AtomicReference<ConnectionStatus> status = new AtomicReference<>(ConnectionStatus.OFFLINE);
    final AtomicBoolean criticalError = new AtomicBoolean();
    final QuoteHistory quoteHistory;
    final AccountLoader accountLoader;
    final Lock connectLock = new ReentrantLock();
    final Key key = new Key();
    private final Lock statusLock = new ReentrantLock();
    private final ScheduledExecutorService scheduler;
    private final ExecutorService outcome;
    private final ExecutorService income;
    private final ExecutorService messageHandlerExecutor;
    private final AtomicInteger RequestId = new AtomicInteger(1);
    public PlacedType placedType = PlacedType.Manually;
    public LocalDateTime Time = LocalDateTime.MIN;
    public AccountRec Account;
    public OpenedClosedOrders orders;
    public OrderHistory orderHistory;
    public String Server;
    public byte[] PfxFile;
    public String PfxFilePassword;
    //public Workaround Workaround;
    public Map.Entry<ServerRec, ArrayList<Map.Entry<AccessInfo, List<AddressRec>>>> ServerDetails;
    public ZoneId zoneId;
    public ZoneOffset zoneOffset;
    public String guid = "1288942f-aadb-4d98-8cc1-c06f33730d76";
    public Boolean onCalcProfit = false;
    Cache<String, OrderRequest> orderRequests;
    LocalEntryService localEntryService;
    MessageHandler messageHandler;
    Connection Connection;
    Subscriber subscriber;
    OrderProfit orderProfit;
    List<String> n = new ArrayList<>();
    private EventProducer eventProducer;
    private ReconnectService reconnectService;

    public MT5API(ScheduledExecutorService scheduler
            , ExecutorService income
            , ExecutorService outcome
            , ExecutorService messageHandlerExecutor
            , Cache<String, OrderRequest> orderRequests) {
        this.scheduler = scheduler;
        this.income = income;
        this.outcome = outcome;
        this.messageHandlerExecutor = messageHandlerExecutor;
        this.orderRequests = orderRequests;
        subscriber = new Subscriber(this);
        orders = new OpenedClosedOrders();
        orderHistory = new OrderHistory();
        orderProfit = new OrderProfit(this);
        quoteHistory = new QuoteHistory(this);
        accountLoader = new AccountLoader();
        messageHandler = new MessageHandler();
        init(income);
    }

    public static Server[] LoadServersDat(String path) throws IOException {
        return (new ServersDatLoader()).Load(path);
    }

    public static int PingHost(String host, int port, int timeoutMs) throws UnknownHostException {
        LocalDateTime start = LocalDateTime.now();
        int res = -1;
        try {
            try (Socket client = new Socket()) {
                client.connect(new InetSocketAddress(host, port), timeoutMs);
                res = (int) Duration.between(start, LocalDateTime.now()).toMillis();
            }
        } catch (UnknownHostException ex) {
            throw ex;
        } catch (Exception ignored) {
        }
        return res;
    }

    private static boolean isNotAccepted(OrderStateData orderStateData) {
        return OrderStateData.Cancelled.equals(orderStateData)
               || OrderStateData.Rejected.equals(orderStateData);
    }

    static OrderTypeData toOrderTypeData(OrderType orderType) {
        return switch (orderType) {
            case Buy -> OrderTypeData.Buy;
            case Sell -> OrderTypeData.Sell;
            case BuyLimit -> OrderTypeData.BuyLimit;
            case SellLimit -> OrderTypeData.SellLimit;
            case BuyStop -> OrderTypeData.BuyStop;
            case SellStop -> OrderTypeData.SellStop;
            default -> throw new CodeException("Unknown OperationType: " + orderType, Code.INVALID_PARAM);
        };
    }

    private void init(ExecutorService income) {
        eventProducer = new EventProducer(new TaskExecutor() {
            @Override
            public void submit(String connectorId, Task task) {
                income.execute(task);
            }
        }, key);
        reconnectService = new ReconnectService(Set.of(ACCOUNT_DISABLED.getValue()
                , OLD_VERSION.getValue()
                , UNALLOWED_COMPANY.getValue()
                , INVALID_ACCOUNT.getValue()
                , INVESTOR_PASSWORD.getValue()), Set.of(), this);
        eventProducer.listener(EventTopic.CONNECT, reconnectService);
        this.localEntryService = new LocalEntryService(this
                , eventProducer
                , (connectorId, task) -> outcome.execute(task));
        eventProducer.listener(EventTopic.CONNECT, new EventConsumer() {
            @Override
            public void onNext(EventTopic topic, IBaseApi api, Event event) {
                if (event instanceof ConnectEvent) {

                    try {
                        subscriber.resubscribe();
                    } catch (Exception e) {
                        if (!subscriber.subscribe.isEmpty()) {
                            for (String symbol : subscriber.subscribe) {
                                CodeException codeException = new CodeException("Failed to subscribe due reconnect to symbol " + symbol, SUBSCRIBE_ERROR);
                                sendSubscribeError(symbol, codeException, true);
                            }
                        }
                    }
                    if (System.currentTimeMillis() - hostAndPortService.brokerDataTime.get() > TimeUnit.MINUTES.toMillis(30)) {
                        try {
                            final String server = ServerDetails.getKey().ServerName;
                            final String company = ServerDetails.getKey().CompanyName;
                            hostAndPortService.update(server, company, "mt5");
                        } catch (Exception ignored) {
                        }
                    }
                }

            }

            @Override
            public String getId() {
                return EventTopic.CONNECT + "_" + getBrokerId() + "_" + getAccountId();
            }
        });

        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        if (isDead()) {
                            scheduler.shutdown();
                        } else {
                            scheduleJob();
                        }
                    } catch (Exception e) {
                        LogUtil.log("SCHEDULER", "api", getAccountId()
                                , getBrokerId(),
                                text ->
                                        log.error("{}, \"text\":\"fatal\"", text, e));
                        criticalError.getAndSet(true);
                    }
                }, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void scheduleJob() {
        if (!isConnected()) {
            reconnectService.reconnect();
        }
    }

    @Override
    public void connect() {
        if (isConnected())
            throw new CodeException("Already connected", Code.CONNECT_ALREADY_EXIST);
        var credentials = (Mt5ApiCredentials) apiCredentials.get();
        key.setAccountId(credentials.getAccountId());
        key.setBrokerId(credentials.getBrokerId());
        hostAndPortService.init(getBrokerId(), credentials.getBrokerName(), "mt5", credentials.getHostPorts());
        tryConnect();
    }

    @Override
    public void simulateConnect() {

    }

    void tryConnect() {
        if (isConnected())
            throw new CodeException("Already connected", Code.CONNECT_ALREADY_EXIST);
        connectLock.lock();
        try {
            setStatus(ConnectionStatus.CONNECTING);
            try {
                clearAndStop();
                final HostPort hostPort = hostAndPortService.next();
                LogUtil.log("connect-begin", "api", getAccountId(), getBrokerId(),
                        msg ->
                                log.info("{},\"host\":\"{}\"",
                                        msg
                                        , hostPort.getHost()));
                var credentials = apiCredentials.get();
                Connection = new Connection((int) (toLong(credentials.getAccountId()) & 0xFFFFFFFFL)
                        , credentials.getPassword(), hostPort.getHost(), hostPort.getPort(), this);
                Connection.logon();
                messageHandler.start();
            } catch (Exception e) {
                LogUtil.log("FAILED-CONNECT", "api", getAccountId()
                        , getBrokerId(),
                        text ->
                                log.error(text, e));
                notifyConnectError(e, EventType.CONNECT);
            }
        } finally {
            connectLock.unlock();
        }
    }

    private void notifyConnectError(Throwable throwable, EventType eventType) {
        if (!isDead()) {
            criticalError.getAndSet(true);
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
                    .accept(MT5API.this, event);
        } catch (Exception e) {
            log.warn("Event {} topic {} issue ", event, topic, e);
        }
    }

    public final void OnQuoteHistory(String symbol, Bar[] bars) {

    }

    /**
     * Force update order profits
     */
    public final void UpdateProfits() throws IOException {
        for (OrderData item : opened()) {
            try {
                Quote quote = quote(item.getSymbol());
                if (quote == null)
                    continue;
                orderProfit.Update(item, quote.Bid, quote.Ask);
                Order order = orders.Opened.get(item.getTicket());
                if (order != null) {
                    order.Profit = item.getProfit().doubleValue();
                }
            } catch (Exception e) {
            }
        }

    }

    /**
     * Account used margin
     */
    public final double AccountMargin() throws IOException {
        HashMap<String, SymbolMargin> syms = new HashMap<String, SymbolMargin>();
        for (Order order : orders.Opened.values()) {
            if (!syms.containsKey(order.Symbol)) {
                syms.put(order.Symbol, new SymbolMargin(this, order.Symbol));
            }
            if (order.OrderType == OrderType.Buy || order.OrderType == OrderType.Sell) {
                syms.get(order.Symbol).AcceptDeal(order.DealInternalIn);
            } else {
                syms.get(order.Symbol).AcceptOrder(order.OrderInternal, false);
            }
        }
        double sum = 0;
        for (SymbolMargin item : syms.values()) {
            sum += item.GetTradeMargin();
        }
        return sum;
    }

    /**
     * Account free margin
     */
    public final double AccountFreeMargin() throws IOException {
        return AccountEquity() - AccountMargin();
    }

    public LocalDateTime ServerTime() {
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(ServerDetails.getKey().TimeZone).plusHours(ServerDetails.getKey().DST);
    }

    public int ServrTimeZoneInMinutes() {
        return ServerDetails.getKey().TimeZone + 60 * ServerDetails.getKey().DST;
    }

    /**
     * Is imvestor mode
     *
     * @return
     */
    public boolean IsInvestor() {
        int InvestorFlag = 8;
        if ((Account.TradeFlags & InvestorFlag) != 0)
            return true;
        return false;
    }

    public void setServerDetails(Map.Entry<ServerRec, ArrayList<Map.Entry<AccessInfo, List<AddressRec>>>> serverDetails) {
        this.ServerDetails = serverDetails;
        ServerRec serverRec = ServerDetails.getKey();
        this.zoneOffset = ZoneOffset.ofHours((serverRec.TimeZone / 60) + serverRec.DST);
        this.zoneId = ZoneId.ofOffset("GMT", zoneOffset);

    }

    /**
     * Account profit
     */
    public final double AccountProfit() {
        return opened().stream()
                .map(orderData -> orderData.getProfit().add(orderData.getCommission()).add(orderData.getSwap()).add(orderData.getTaxes()))
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO).doubleValue();
    }

    /**
     * Account equity.
     */
    public final double AccountEquity() {
        return Account.Balance + AccountProfit();
    }

    public final void OrderSendAsync(
            OrderRequest request
            , Long expertId
            , String symbol
            , double lots
            , double price
            , OrderType type
            , double sl
            , double tp
            , long deviation
            , String comment
            , FillPolicy fillPolicy
            , double stoplimit
            , LocalDateTime expiration) throws IOException {
        if (fillPolicy == null) {
            fillPolicy = GetFillPolicy(symbol, type);
        }
        TradeRequest req = new TradeRequest();
        req.Flags &= ~0x100;
        req.Flags &= ~0x200;
        req.PlacedType = placedType;
        req.Login = (Long) apiCredentials.get().getAccountId();
        req.Digits = symbols.GetInfo(symbol).Digits;

        req.OrderPrice = stoplimit;
        req.Price = price;
        req.Lots = (long) (lots * 100000000);
        req.Currency = symbol;
        if (type == OrderType.Buy || type == OrderType.Sell)
            req.TradeType = TradeType.forValue(symbols.GetGroup(symbol).TradeType.getValue() + 1);
        else
            req.TradeType = TradeType.SetOrder;
        req.OrderType = type;
        req.StopLoss = sl;
        req.TakeProfit = tp;
        req.Deviation = deviation;
        req.Comment = comment;
        req.FillPolicy = fillPolicy;
        if (expiration != null) {
            req.expirationType = ExpirationType.Specified;
            req.ExpirationTime = ConvertTo.Long(expiration);
        }
        req.ExpertId = (expertId > 0) ? expertId * -1 : expertId;
        req.RequestId = setRequestListener(request, "OrderSend");
        if (req.ExpertId == 0) {
            req.ExpertId = -req.RequestId;
        }
        (new OrderSender(Connection)).Send(req);
    }

    private Quote quote(String symbol) throws Exception {
        try {
            subscribe(symbol);
        } catch (Exception e) {
        }
        Quote quote;
        long time = System.currentTimeMillis();
        while ((quote = GetQuote(symbol)) == null
               && (System.currentTimeMillis() - time) < 15000) {
            Thread.sleep(10);
        }
        return quote;
    }

    private double price(double price, String symbol, OrderType type) throws Exception {
        final SymGroup symGroup = symbols.GetGroup(symbol);
        if (symGroup == null) {
            throw new CodeException("Group for symbol " + symbol + " not found.", Code.NOT_FOUND);
        }
        if (symGroup.TradeType != ExecutionType.Market) {
            Quote quote = quote(symbol);
            if (quote == null) {
                throw new CodeException("Invalid price for symbol = " + symbol, Code.NO_PRICES);
            }
            if (type == OrderType.Buy) {
                price = quote.Bid;
            } else if (type == OrderType.Sell) {
                price = quote.Ask;
            }
        }
        return price;
    }

    public final void OrderCloseAsync(
            OrderRequest request
            , Long expertId
            , long ticket
            , String symbol
            , double price
            , double lots
            , OrderType type
            , long deviation
            , String comment
            , FillPolicy fillPolicy) throws IOException {
        if (fillPolicy == null) {
            fillPolicy = GetFillPolicy(symbol, type);
        }
        TradeRequest req = new TradeRequest();
        req.Flags &= ~0x100;
        req.Flags &= ~0x200;
        req.PlacedType = placedType;
        req.Login = (Long) apiCredentials.get().getAccountId();
        req.Digits = symbols.GetInfo(symbol).Digits;

        req.Lots = (long) (lots * 100000000);
        if (req.Lots == 0) {
            throw new CodeException("Request Lots = 0 ", Code.INVALID_PARAM);
        }
        req.Currency = symbol;
        if (type == OrderType.Buy || type == OrderType.Sell) {
            req.Price = price;
            req.DealTicket = ticket;
            req.FillPolicy = fillPolicy;
            req.TradeType = TradeType.forValue(symbols.GetGroup(symbol).TradeType.getValue() + 1);
            if (type == OrderType.Buy) {
                req.OrderType = OrderType.Sell;
            } else {
                req.OrderType = OrderType.Buy;
            }
            req.Deviation = deviation;
        } else {
            req.OrderTicket = ticket;
            req.TradeType = TradeType.CancelOrder;
            req.OrderType = type;
        }
        if (req.FillPolicy == null)
            req.FillPolicy = FillPolicy.FillOrKill;
        req.Comment = comment;
        req.ExpertId = expertId > 0 ? expertId * -1 : expertId;
        req.RequestId = setRequestListener(request, "OrderClose");
        if (req.ExpertId == 0) {
            req.ExpertId = -req.RequestId;
        }
        (new OrderSender(Connection)).Send(req);
    }

    public final void OrderModifyAsync(OrderRequest request
            , Long expertId
            , long ticket, String symbol
            , double volume, double price, OrderType type, double sl, double tp
            , long deviation, String comment
            , FillPolicy fillPolicy
    ) throws IOException {
        if (fillPolicy == null) {
            fillPolicy = GetFillPolicy(symbol, type);
        }
        TradeRequest req = new TradeRequest();
        req.PlacedType = placedType;
        req.Lots = (long) (volume * 100000000);
        req.Currency = symbol;
        req.OrderPrice = price;
        if (type == OrderType.Buy || type == OrderType.Sell) {
            req.DealTicket = ticket;
            req.TradeType = TradeType.ModifyDeal;
        } else {
            req.OrderTicket = ticket;
            req.TradeType = TradeType.ModifyOrder;
            req.Price = price;
        }
        req.OrderType = type;
        req.StopLoss = sl;
        req.TakeProfit = tp;
        req.Deviation = deviation;
        req.Comment = comment;
        req.FillPolicy = fillPolicy;
        req.ExpertId = expertId > 0 ? expertId * -1 : expertId;
        req.RequestId = setRequestListener(request, "OrderModify");
        if (req.ExpertId == 0) {
            req.ExpertId = -req.RequestId;
        }
        (new OrderSender(Connection)).Send(req);
    }

    final double GetContaractSize(String symbol) {
        return symbols.GetInfo(symbol).ContractSize;
    }

    final void RequestDealHistory(LocalDateTime from, LocalDateTime to) throws IOException {

        if (!isConnected()) {
            throw new CodeException("Not connected", Code.NO_CONNECTION);
        }
        orderHistory.Request(from, to, 0, 0, true);
    }

    void RequestDealHistory(int year, int month, List<DealInternal> exist) throws TimeoutException, IOException {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).plusSeconds(-1);
        if (exist == null)
            orderHistory.Request(start, end, 0, 0, true);
        else {
            long max = 0;
            for (DealInternal item : exist)
                if (item.HistoryTime > max)
                    max = item.HistoryTime;
            orderHistory.Request(start, end, exist.size(), max, true);
        }
    }

    void RequestDealHistory(int year, int month) throws TimeoutException, IOException {
        RequestDealHistory(year, month, null);
    }

    final void RequestOrderHistory(LocalDateTime from, LocalDateTime to) throws IOException {

        if (!isConnected()) {
            throw new CodeException("Not connected", Code.NO_CONNECTION);
        }
        orderHistory.Request(from, to, 0, 0, false);
    }

    void RequestOrderHistory(int year, int month, List<OrderInternal> exist) throws TimeoutException, IOException {

        if (!isConnected())
            throw new CodeException("Not connected", Code.NO_CONNECTION);
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).plusSeconds(-1);
        if (exist == null)
            orderHistory.Request(start, end, 0, 0, false);
        else {
            long max = 0;
            for (OrderInternal item : exist)
                if (item.HistoryTime > max)
                    max = item.HistoryTime;
            orderHistory.Request(start, end, exist.size(), max, false);
        }
    }

    public void RequestOrderHistory(int year, int month) throws TimeoutException, IOException {
        RequestOrderHistory(year, month, null);
    }

    public final void RequestHistory(String symbol) throws IOException {
        quoteHistory.ReqStart(Connection, symbol);
    }

    public final Quote GetQuote(String symbol) throws IOException {
        Quote q = subscriber.getQuote(symbol);
        if (q != null) {
            return q;
        }
        return QUOTE;
    }

    public final void OnOrderHisotyCall(OrderHistoryEventArgs args) {
        args.Orders.stream().filter(order -> order.DealType == DealType.DealSell || order.DealType == DealType.DealBuy)
                .forEach(order -> orders.Closed.put(order.Ticket, order));
        produce(EventTopic.LOAD, new SnapshotEvent<>(EventType.LOAD_CLOSE, getBrokerId(), getAccountId(), closed()));
    }

    public final void OnQuoteCall(Quote quote) {
        if (onCalcProfit)
            onCalcProfit(quote);
        produce(EventTopic.PRICE, of(quote));
    }

    private QuoteData of(Quote quote) {
        try {
            SymbolInfo symbolInfo = symbols.symbolInfo(quote.Symbol);
            if (symbolInfo != null) {
                QuoteData quoteData = new QuoteData();
                quoteData.setCode((short) quote.id);
                quoteData.setBrokerId(getBrokerId());
                quoteData.setAccountId(getAccountId());
                quoteData.setAsk(BigDecimal.valueOf(quote.Ask));
                quoteData.setBid(BigDecimal.valueOf(quote.Bid));
                quoteData.setTime(quote.Time.toEpochSecond(zoneOffset));
                quoteData.setTradable(true);
                quoteData.setSymbol(quote.Symbol);
                return quoteData;
            }
        } catch (Exception e) {
            log.error("Failed to process quote", e);
        }
        return null;
    }

    private void onCalcProfit(Quote quote) {
        try {
            for (OrderData item : opened()) {
                if (Objects.equals(item.getSymbol(), quote.Symbol)) {
                    try {
                        orderProfit.Update(item, quote.Bid, quote.Ask);
                        Order order = orders.Opened.get(item.getTicket());
                        if (order != null) {
                            order.Profit = item.getProfit().doubleValue();
                        }
                    } catch (Exception ignored) {

                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    public FillPolicy GetFillPolicy(String symbol, OrderType type) {
        boolean pendingOrder = true;
        if (type == OrderType.Buy || type == OrderType.Sell || type == OrderType.CloseBy)
            pendingOrder = false;
        FillPolicy fp;
        SymGroup group = symbols.GetGroup(symbol);
        if (group.TradeType == ExecutionType.Request || group.TradeType == ExecutionType.Instant) {
            if (pendingOrder)
                fp = FillPolicy.FlashFill;
            else
                fp = FillPolicy.FillOrKill;
        } else {
            if (pendingOrder)
                fp = FillPolicy.FlashFill;
            else if (group.FillPolicy == 2)
                fp = FillPolicy.ImmediateOrCancel;
            else
                fp = FillPolicy.FillOrKill; // FOK or ANY
        }
        return fp;
    }

    private int setRequestListener(OrderRequest orderRequest, String type) {
        final int requestId = RequestId.getAndIncrement();
        try {
            final String key = getRequestKey((orderRequest.getTID() != null
                    ? orderRequest.getTID()
                    : 0)
                                             + "_" + type);
            final String key2 = getRequestKey(String.valueOf(requestId));
            orderRequests.put(key, orderRequest);
            orderRequests.put(key2, orderRequest);
        } catch (Exception e) {
            log.error("Error setting request listener for request {}", orderRequest, e);
        }
        return requestId;
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return status.get();
    }

    private void sendSubscribeError(String symbol, Exception e, boolean active) {
        CodeException codeException = ErrorUtil.toError(e);
        produce(EventTopic.SUBSCRIBE, new SubscribeErrorEvent(getBrokerId()
                        , getAccountId()
                        , symbol
                        , active
                        , codeException
                        , EventType.SUBSCRIBE
                )
        );
    }

    @Override
    public void subscribe(String symbol, boolean critical) {
        if (isConnected()) {
            try {
                this.subscriber.subscribe(symbol);
            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                if (critical) {
                    throw codeException;
                }
                sendSubscribeError(symbol, codeException, true);
            }
        } else {
            CodeException codeException = new CodeException("Not connected", Code.NO_CONNECTION);
            if (critical) {
                throw codeException;
            }
            sendSubscribeError(symbol, codeException, true);
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
                this.subscriber.unsubscribe(symbol);
            } catch (Exception e) {
                CodeException codeException = ErrorUtil.toError(e);
                sendSubscribeError(symbol, codeException, false);
            }
        } else {
            CodeException codeException = new CodeException("Not connected", Code.NO_CONNECTION);
            sendSubscribeError(symbol, codeException, false);
        }
    }

    @Override
    public void addListener(EventTopic topic, EventConsumer eventConsumer) {
        eventProducer.listener(topic, eventConsumer);
    }

    @Override
    public void removeListener(EventTopic topic, EventConsumer eventConsumer) {

    }

    @Override
    public List<ContractData> getAllContractData() {
        return symbols.Infos.values().stream().map(this::toContractData).collect(Collectors.toList());
    }

    private ContractData toContractData(SymbolInfo symbolInfo) {
        ContractData contractData = new ContractData();
        contractData.setSymbol(symbolInfo.Name);
        contractData.setContractSize(BigDecimal.valueOf(symbolInfo.ContractSize));
        contractData.setContractType(ContractType.fromValue(symbolInfo.CalcMode.getValue()));
        contractData.setCurrency(symbolInfo.ProfitCurrency);
        contractData.setDigits(symbolInfo.Digits);
        contractData.setPointSize(BigDecimal.valueOf(symbolInfo.Points));
        try {
            SymGroup symGroup = symbols.GetGroup((symbolInfo.Name));
            if (symGroup != null) {
                double marginRate = symGroup.InitMarginRate[0];
                if (marginRate == 0) {
                    marginRate = 1;
                }
                double marginDivider = 1 / marginRate;
                contractData.setMarginDivider(BigDecimal.valueOf(marginDivider));
            }
        } catch (Exception e) {
            log.error("Error getting marginDivider in contract data for symbol {}", symbolInfo.Name, e);
        }
        contractData.setMarginCurrency(symbolInfo.MarginCurrency);
        contractData.setTickSize(BigDecimal.valueOf(symbolInfo.TickSize));
        contractData.setTickPrice(BigDecimal.valueOf(symbolInfo.TickValue));
        return contractData;
    }

    @Override
    public ContractData getContractData(String symbol) {
        var symbolInfo = symbols.GetInfo(symbol);
        return toContractData(symbolInfo);
    }

    @Override
    public void shutdown() {
        try {
            disconnect();
        } catch (Exception ignored) {
        }
        try {
            scheduler.shutdownNow();
        } catch (Exception ignored) {
        }
        try {
            outcome.shutdownNow();
        } catch (Exception ignored) {
        }
        try {
            income.shutdown();
        } catch (Exception ignored) {
        }
        try {
            messageHandlerExecutor.shutdownNow();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void update(ConnectionStatus connectionStatus) {
        setStatus(connectionStatus);
    }

    @Override
    public boolean isConnected() {
        return status.get() == ConnectionStatus.ONLINE
               && !criticalError.get()
               && messageHandler.getRunning();
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
        var error = ErrorUtil.toError(throwable);
        var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, EventType.RECONNECT);
        produce(EventTopic.CONNECT, event);
        disconnect();
    }

    @Override
    public void fireConnect() throws Exception {
        connect();
    }

    @Override
    public IBaseApi setApiCredentials(ApiCredentials apiCredentials) {
        this.apiCredentials.set(apiCredentials);
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

    @Override
    public void connect(ApiCredentials apiCredentials) {
        setApiCredentials(apiCredentials);
        connect();
    }

    @Override
    public void disconnect() {
        connectLock.lock();
        try {
            setStatus(ConnectionStatus.DEAD);
            clearAndStop();
        } finally {
            connectLock.unlock();
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
                this.messageHandler.running.getAndSet(false);
            }
        } finally {
            statusLock.unlock();
        }
    }

    @Override
    public Long getBrokerId() {
        return apiCredentials.get().getBrokerId();
    }

    @Override
    public Object getAccountId() {
        return apiCredentials.get().getAccountId();
    }

    @Override
    public List<OrderData> opened() {
        return orders.Opened.values().stream()
                .filter(order -> order.OrderType == OrderType.Buy || order.OrderType == OrderType.Sell)
                .map(order -> order.toOrderData(getBrokerId(), getAccountId(), zoneOffset))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderData> pending() {
        return orders.Opened.values().stream()
                .filter(order -> order.OrderType == OrderType.BuyStop || order.OrderType == OrderType.SellStop
                                 || order.OrderType == OrderType.BuyLimit || order.OrderType == OrderType.SellLimit)
                .map(order -> order.toOrderData(getBrokerId(), getAccountId(), zoneOffset))
                .collect(Collectors.toList());
    }

    @Override
    public List<CloseOrderData> closed() {
        return orders.Closed.values().stream()
                .filter(order -> order.OrderType == OrderType.Buy || order.OrderType == OrderType.Sell)
                .map(order -> order.toCloseOrderData(getBrokerId(), getAccountId(), zoneOffset))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, OrderData> trades() {
        return Map.of();
    }

    @Override
    public Map<Integer, OrderData> orders() {
        return Map.of();
    }

    @Override
    public Map<Integer, CloseOrderData> history() {
        return Map.of();
    }

    @Override
    public OrderData findOrder(Long ticket) {
        var order = orders.Opened.get(ticket);
        if (order == null)
            throw new CodeException("Order " + ticket + " not found", NOT_FOUND);
        return order.toOrderData(getBrokerId(), getAccountId(), zoneOffset);
    }

    @Override
    public List<OrderData> findOrders(List<Long> tickets) {
        return orders.Opened.values().stream()
                .filter(order -> tickets.contains(order.Ticket))
                .map(order -> order.toOrderData(getBrokerId(), getAccountId(), zoneOffset))
                .collect(Collectors.toList());
    }

    @Override
    public AccountData getAccountData() {
        return toAccountData();
    }

    private void clearAndStop() {
        LogUtil.log("stop message handler", "api", getAccountId(), getBrokerId()
                , log::debug);
        //Stop
        messageHandler.running.set(false);
        symbols.Groups.clear();
        symbols.Sessions.clear();
        orders.Opened.clear();
        orders.Closed.clear();
    }

    AccountData toAccountData() {
        return new AccountData(getBrokerId()
                , getAccountId()
                , BigDecimal.valueOf(Account.Balance)
                , BigDecimal.valueOf(Account.Credit)
                , 2
                , symbols.Base.Currency
                , Account.Leverage
                , null
                , 0);
    }

    @Override
    public void execute(List<IRequest> requests) {
        requests.forEach(this::execute);
    }

    private OrderData getOrder(Long ticket, Map<Integer, OrderData> orders) {
        OrderData order = orders.get(ticket.intValue());
        if (order == null)
            throw new CodeException(String.format("Can't find order %s", ticket), Code.NOT_FOUND);
        return order;
    }

    @Override
    public void execute(IRequest request) {
        if (!(request instanceof final OrderRequest orderRequest))
            throw new CodeException(Code.INVALID_PARAM);
        if (!isConnected())
            throw new CodeException("Can't execute request " + request.getTID() + ", Please reconnect.", Code.CONNECT_TIMEOUT_ERROR);
        outcome.execute(() -> {
            orderRequest.setAccountId(getAccountId());
            orderRequest.setBrokerId(getBrokerId());
            final AtomicReference<OrderData> orderRef = new AtomicReference<>();
            if (orderRequest.getTime() == null) {
                orderRequest.setTime(System.currentTimeMillis());
            }
            try {
                switch (orderRequest.getRequestType()) {
                    case CLOSE -> {
                        if (orderRequest.getTicket() == null)
                            throw new CodeException("TicketId was not provided", Code.INVALID_PARAM);
                        orderRef.set(findOrder(orderRequest.getTicket()));
                        doClose(orderRequest, orderRef.get());
                    }
                    case CLOSE_SYMBOL ->
                            closeWithFilter(orderRequest, order -> order.getSymbol().equals(orderRequest.getSymbol()));

                    case CLOSE_ALL -> {
                        closeWithFilter(orderRequest, order -> true);
                    }
                    case CLOSE_TYPE -> closeWithFilter(orderRequest, order ->
                            (OrderTypeData.Buy.equals(order.getOrderType())
                             || OrderTypeData.Sell.equals(order.getOrderType()))
                            && order.getOrderType().getValue() == (orderRequest.getType().getValue()));
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
                        orderRef.set(findOrder(orderRequest.getTicket()));
                        updateOrder(orderRequest
                                , orderRef.get().getPrice(), orderRef.get());
                    }
                    case CANCEL_PENDING -> {
                        if (orderRequest.getTicket() == null)
                            throw new CodeException("TicketId was not provided", Code.INVALID_PARAM);
                        orderRef.set(findOrder(orderRequest.getTicket()));
                        cancelOrder(orderRequest, orderRef.get());
                    }
                    case PENDING -> pendingOrder(orderRequest);
                    case UPDATE_PENDING -> {
                        if (orderRequest.getTicket() == null)
                            throw new CodeException("TicketId was not provided", Code.INVALID_PARAM);
                        orderRef.set(findOrder(orderRequest.getTicket()));
                        updateOrder(orderRequest
                                , orderRequest.getPrice() != null ? orderRequest.getPrice() : orderRef.get().getPrice()
                                , orderRef.get());
                    }

                    case CANCEL_LOCAL_PENDING -> localEntryService.delete(orderRequest.getTicket());
                    case LOCAL_PENDING -> localEntryService.create(orderRequest);
                    case UPDATE_LOCAL_PENDING -> localEntryService.update(orderRequest);
                    case LOAD_LOCAL_PENDING -> localEntryService.addOrder(orderRequest);
                    default -> throw new CodeException(Code.INVALID_PARAM);
                }
            } catch (Exception e) {
                errorHandler(orderRequest, orderRef.get(), e);
            }
        });
    }

    void closeWithFilter(OrderRequest orderRequest, Predicate<OrderData> filter) {
        var opened = opened().stream().filter(filter)
                .sorted(Comparator.comparing(OrderData::getTicket))
                .toList();
        if (opened.isEmpty()) {
            throw new CodeException("No orders", Code.NOT_FOUND);
        }
        opened.forEach(order -> {
            try {
                doClose(orderRequest, order);
            } catch (Exception e) {
                errorHandler(orderRequest, order, e);
            }
        });
    }

    private void errorHandler(OrderRequest request, OrderData order, Exception e) {
        var error = ErrorUtil.toError(e);
        var event = new OrderErrorEvent(getBrokerId()
                , getAccountId()
                , request.getTID(), error
                , EventType.REJECT_ORDER
                , request
                , order);
        produce(EventTopic.ORDER_REQUEST, event);

    }

    public void cancelOrder(OrderRequest request
            , OrderData order) {
        try {
            OrderCloseAsync(request
                    , request.getTID() == null ? 0 : toLong(request.getTID())
                    , order.getTicket().intValue()
                    , order.getSymbol()
                    , order.getPrice().doubleValue()
                    , order.getLot().doubleValue()
                    , orderType(order.getOrderType())
                    , 0
                    , request.getComment()
                    , null);
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    private OrderType orderType(OrderTypeData type) {
        switch (type) {
            case Buy -> {
                return OrderType.Buy;
            }
            case Sell -> {
                return OrderType.Sell;
            }
            case BuyStop -> {
                return OrderType.BuyStop;
            }
            case SellStop -> {
                return OrderType.SellStop;
            }
            case BuyLimit -> {
                return OrderType.BuyLimit;
            }
            case SellLimit -> {
                return OrderType.SellLimit;
            }
            default -> throw new CodeException("order type :" + type, UNSUPPORTED_OPERATION);
        }
    }

    public void pendingOrder(OrderRequest request) {

        try {
            OrderSendAsync(request
                    , request.getTID() == null ? 0 : toLong(request.getTID())
                    , request.getSymbol()
                    , request.getLot().doubleValue()
                    , request.getPrice() != null ? request.getPrice().doubleValue() : 0
                    , orderType(request.getType())
                    , request.getSl() != null ? request.getSl().doubleValue() : 0
                    , request.getTp() != null ? request.getTp().doubleValue() : 0
                    , 100
                    , request.getComment()
                    , null
                    , 0
                    , null);
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }

    }

    public void updateOrder(
            OrderRequest orderRequest
            , BigDecimal price
            , OrderData order) {
        try {
            OrderModifyAsync(orderRequest
                    , orderRequest.getTID() == null ? 0 : toLong(orderRequest.getTID())
                    , order.getTicket()
                    , order.getSymbol()
                    , order.getLot().intValue()
                    , price.doubleValue()
                    , orderType(orderRequest.getType())
                    , orderRequest.getSl() == null
                            ? order.getSl() == null ? 0.0 : order.getSl().doubleValue()
                            : orderRequest.getSl().doubleValue()
                    , orderRequest.getTp() == null
                            ? order.getTp() == null ? 0.0 : order.getTp().doubleValue()
                            : orderRequest.getTp().doubleValue()
                    , 0
                    , order.getComment()
                    , null
            );
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    public void marketOrder(OrderRequest request) {
        try {
            OrderSendAsync(request
                    , request.getTID() == null ? 0 : toLong(request.getTID())
                    , request.getSymbol()
                    , request.getLot().doubleValue()
                    , request.getPrice() != null ? request.getPrice().doubleValue() : 0
                    , orderType(request.getType())
                    , request.getSl() != null ? request.getSl().doubleValue() : 0
                    , request.getTp() != null ? request.getTp().doubleValue() : 0
                    , 100
                    , request.getComment()
                    , null
                    , 0
                    , null);
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }

    }

    public void doClose(OrderRequest request
            , OrderData order) {
        var orderLot = order.getLot();
        var reqAmount = request.getLot();
        if (request.getLot() == null || orderLot.compareTo(order.getLot()) < 0 || order.getLot().compareTo(BigDecimal.ZERO) <= 0) {
            reqAmount = orderLot;
        }
        try {
            OrderCloseAsync(request
                    , request.getTID() == null ? 0 : toLong(request.getTID())
                    , order.getTicket().intValue()
                    , order.getSymbol()
                    , order.getPrice().doubleValue()
                    , reqAmount.doubleValue()
                    , orderType(order.getOrderType())
                    , 100
                    , request.getComment()
                    , null);
        } catch (Exception e) {
            throw ErrorUtil.toError(e);
        }
    }

    OrderEvent toOrderEvent(OrderStateData orderStateData
            , OrderTypeData orderTypeData, OrderInternal order) {
        IOrder iorder = toOrderData(order);
        return OrderEvent.builder()
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .order(iorder)
                .orderStateData(orderStateData)
                .eventType(eventType(orderStateData, orderTypeData))
                .symbol(order.Symbol)
                .TID(order.ExpertId)
                .ticket(order.TicketNumber)
                .build();
    }

    OrderEvent toOrderEvent(OrderRequest request, OrderStateData orderStateData
            , OrderTypeData orderTypeData, Order order, CodeException codeException) {
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
                .TID(order != null ? order.ExpertId : request.getTID())
                .ticket(order != null ? order.Ticket : null)
                .build();
    }

    OrderErrorEvent toErrorOrderEvent(OrderRequest request, OrderStateData orderStateData
            , OrderTypeData orderTypeData, Order order, CodeException codeException) {

        boolean notAccepted = isNotAccepted(orderStateData);
        IOrder iorder = getIOrder(orderStateData, order, notAccepted);
        return new OrderErrorEvent(getBrokerId()
                , getAccountId()
                , order != null ? order.ExpertId : request.getTID()
                , codeException
                , eventType(orderStateData, orderTypeData)
                , request
                , iorder);


    }

    private IOrder getIOrder(OrderStateData orderStateData, Order order, boolean notAccepted) {
        if (order == null)
            return null;
        IOrder iorder = null;
        if (OrderStateData.Closed.equals(orderStateData)
            || OrderStateData.PartialClosed.equals(orderStateData)) {
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
            , OrderStateData orderStateData
            , OrderTypeData orderTypeData
            , OrderInternal order) {
        OrderEvent orderEvent = toOrderEvent(orderStateData, orderTypeData, order);
        eventProducer.eventConsumer(eventTopic)
                .accept(MT5API.this, orderEvent);
    }

    void sendOrderEvent(EventTopic eventTopic
            , OrderRequest request
            , OrderStateData orderStateData, OrderTypeData orderTypeData, Order order, CodeException exception) {
        OrderEvent orderEvent = toOrderEvent(request, orderStateData, orderTypeData, order, exception);
        eventProducer.eventConsumer(eventTopic)
                .accept(MT5API.this, orderEvent);
    }

    void sendErrorOrderEvent(EventTopic eventTopic
            , OrderRequest request
            , OrderStateData orderStateData, OrderTypeData orderTypeData, Order order, CodeException exception) {
        OrderErrorEvent orderErrorEvent = toErrorOrderEvent(request, orderStateData, orderTypeData, order, exception);
        eventProducer.eventConsumer(eventTopic)
                .accept(MT5API.this, orderErrorEvent);
    }

    private String getRequestKey(String preKey) {
        return getBrokerId() + "_" + getAccountId() + "_" + preKey;
    }

    OrderData toOrderData(Order order) {
        return OrderData.builder()
                .TID(order.ExpertId)
                .ticket(order.getTicket())
                .comment(order.getComment())
                .lot(BigDecimal.valueOf(order.getLots()))
                .price(BigDecimal.valueOf(order.getOpenPrice()))
                .sl(BigDecimal.valueOf(order.getStopLoss()))
                .tp(BigDecimal.valueOf(order.getTakeProfit()))
                .symbol(order.getSymbol())
                .orderType(toOrderTypeData(order.getOrderType()))
                .swap(BigDecimal.valueOf(order.getSwap()))
                .profit(BigDecimal.valueOf(order.getProfit()))
                .commission(BigDecimal.valueOf(order.getCommission()))
                .time(order.OpenTime.toEpochSecond(zoneOffset) * 1000)
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .build();
    }

    OrderData toOrderData(OrderInternal order) {
        return OrderData.builder()
                .TID(order.ExpertId)
                .ticket(order.TicketNumber)
                .comment(order.Comment)
                .lot(BigDecimal.valueOf(order.Lots))
                .price(BigDecimal.valueOf(order.OpenPrice))
                .sl(BigDecimal.valueOf(order.StopLoss))
                .tp(BigDecimal.valueOf(order.TakeProfit))
                .symbol(order.Symbol)
                .orderType(toOrderTypeData(order.Type))
                .time(order.OpenTime)
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .build();
    }

    CloseOrderData toCloseOrderData(Order order) {
        return CloseOrderData.builder()
                .TID(order.ExpertId)
                .ticket(order.getTicket())
                .comment(order.getComment())
                .lot(BigDecimal.valueOf(order.getLots()))
                .openPrice(BigDecimal.valueOf(order.getOpenPrice()))
                .closePrice(BigDecimal.valueOf(order.getClosePrice()))
                .closeTime(order.getCloseTime().toEpochSecond(zoneOffset) * 1000)
                .openTime(order.getCloseTime().toEpochSecond(zoneOffset) * 1000)
                .sl(BigDecimal.valueOf(order.getStopLoss()))
                .tp(BigDecimal.valueOf(order.getTakeProfit()))
                .symbol(order.getSymbol())
                .orderType(toOrderTypeData(order.OrderType))
                .swap(BigDecimal.valueOf(order.getSwap()))
                .profit(BigDecimal.valueOf(order.getProfit()))
                .commission(BigDecimal.valueOf(order.getCommission()))
                .accountId(getAccountId())
                .brokerId(getBrokerId())
                .build();
    }

    class MessageHandler {
        private static final short PackCompress = 1;
        private static final short PackComplete = 2;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public final boolean getRunning() {
            return running.get();
        }

        public void start() {
            running.getAndSet(true);
            setStatus(ConnectionStatus.ONLINE);

            messageHandlerExecutor.execute(() -> {
                LocalDateTime LastPing = LocalDateTime.now();
                final BytePacket bytePacket = new BytePacket();
                byte cmd = 0;
                try {
                    while (running.get() && !Thread.currentThread().isInterrupted()) {
                        InBuf buf = Connection.ReceivePacket();
                        cmd = buf.Hdr.Type;
                        if ((buf.Hdr.Flags & PackCompress) > 0) {
                            Connection.Decompress(buf);
                        }
                        if (bytePacket.has(cmd)) {
                            bytePacket.add(cmd, buf.ToBytes());
                            if ((buf.Hdr.Flags & PackComplete) > 0) {
                                buf.SetBuf(bytePacket.toBytes(cmd));
                            } else {
                                continue;
                            }
                        } else {
                            if ((buf.Hdr.Flags & PackComplete) > 0) {
                            } else {
                                bytePacket.add(cmd, buf.ToBytes());
                                continue;
                            }
                        }
                        switch (cmd) {
                            case 0x65:
                                orderHistory.Parse(buf);
                                break;
                            case 0x66:
                                quoteHistory.Parse(buf);
                                break;
                            case 0xC:
                                accountLoader.Parse(buf);
                                break;
                            case 0x32:
                                subscriber.Parse(buf);
                                break;
                            case 0x36:
                                break;
                            case 0x37:
                                orders.ParseTrades(buf);
                                break;
                            case 0x33:
                                break;
                            case 0xA:
                                break;
                            case 0x6C:
                                ParseResult(buf);
                                break;
                            default:
                                break;
                        }
                        if (Duration.between(LastPing, LocalDateTime.now()).getSeconds() > 10) {
                            Connection.SendPacket((byte) 0xA, new OutBuf());
                            LastPing = LocalDateTime.now();
                        }
                    }
                } catch (Exception e) {
                    notifyConnectError(e, EventType.CONNECT);
                }
                running.getAndSet(false);
            });

        }

        private void ParseResult(InBuf buf) {
            Code status = Code.getById(buf.Int());
//            log.info("ParseResult: {}", status);
        }
    }

    class AccountLoader {
        public final void Parse(InBuf buf) {
            try {
                Code status = Code.getById(buf.Int());
                if (status != Code.DONE) {
                    throw new CodeException("Account status error.", status);
                }
                while (buf.gethasData()) {
                    byte cmd = buf.Byte();
                    switch ((cmd & 0xFF)) {
                        case 0x07: //symbols
                            LoadSymbols(buf);
                            break;
                        case 0x11: //tickers
                            LoadTickers(buf);
                            break;
                        case 0x17: //server
                            setServerDetails(LoadServer(buf));
                            break;
                        case 0x18: //mail recepients
                            MailRecipient[] mr = LoadMailRecepients(buf);
                            break;
                        case 0x1F: //order
                            orders.AddOrders(LoadOrders(buf));
                            break;
                        case 0x24: //deal
                            orders.AddDeals(LoadDeals(buf));
                            break;
                        case 0x25: //account
                            Account = LoadAccount(buf);
                            break;
                        case 0x28: //spreads
                            LoadSpreads(buf);
                            break;
                        case 0x67:                              //subscriptions
                            LoadSubscriptions(buf);
                            break;
                        case 0x69:                              //subscription categories
                            int size = buf.Int();
                            buf.Bytes(0x80 * size);
                            break;
                        case 0x78:                              //payments
                            LoadPayments(buf);
                            break;
                        case 0x79:                              //payments
                        case 0x80:
                            LoadPayments2(buf);
                            break;
                        case 0x84:
                            LoadSomeAccountData(buf);
                            break;
                        default:
                            break;
                    }
                }

                Connection.SendPacket((byte) 0xA, new OutBuf());
                produce(EventTopic.CONNECT, new ConnectEvent(getBrokerId(), getAccountId()));
                produce(EventTopic.ACCOUNT, toAccountData());
                produce(EventTopic.LOAD, new SnapshotEvent<>(
                        EventType.LOAD_OPEN
                        , getBrokerId()
                        , getAccountId()
                        , opened()));
                produce(EventTopic.LOAD, new SnapshotEvent<>(
                        EventType.LOAD_PENDING
                        , getBrokerId()
                        , getAccountId()
                        , pending()));
                outcome.execute(() -> {
                    try {
                        orderHistory.Request(LocalDateTime.now().minusDays(30)
                                , LocalDateTime.now()
                                , 0, 0, true);
                    } catch (Exception e) {
                        produce(EventTopic.LOAD, new SnapshotEvent<>(
                                EventType.LOAD_CLOSE
                                , getBrokerId()
                                , getAccountId()
                                , closed()));
                    }
                });

            } catch (Exception ex) {
                throw new CodeException(ex, ACCOUNT_LOAD_ERROR);
            }
        }

        private void LoadSubscriptions(InBuf buf) {
            Code status = Code.getById(buf.Int());
            if (status == Code.OK)
                return;
            if (status != Code.DONE)
                throw new RuntimeException(status.toString());
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                buf.Bytes(1240); //vSubscriptionInfo
                int size = buf.Int();
                buf.Bytes(size);
                int count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(256);
                count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(256);
                count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(292);
                count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(292);
            }
        }

        private void LoadPayments(InBuf buf) {
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                buf.Bytes(776);
                int size = buf.Int();
                buf.Bytes(size);
                int count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(528);
                count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(208);
                count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(112);
            }
        }

        private void LoadPayments2(InBuf buf) {
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                buf.Bytes(20);          //vPaymentRec
                int size = buf.Int();   //vPaymentRec
                buf.Bytes(104);         //vPaymentRec
                buf.Bytes(size);
            }
        }

        private void LoadSomeAccountData(InBuf buf) {
            buf.Bytes(3084);
            int num = buf.Int();
            for (int i = 0; i < num; i++)
                buf.Bytes(1288);
        }

        private void LoadSpreads(InBuf buf) {
            Code status = Code.getById(buf.Int());
            if (status == Code.OK) {
                return;
            }
            if (status != Code.DONE) {
                throw new CodeException(status);
            }
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                LoadSpread(buf);
            }
            LoadRemoveList(buf);
        }

        private void LoadSpread(InBuf buf) {
            SpreadInfo si = buf.Struct(new SpreadInfo());
            int num = buf.Int();
            SpreadData[] buy = new SpreadData[num];
            for (int i = 0; i < num; i++) {
                buy[i] = buf.Struct(new SpreadData());
            }
            num = buf.Int();
            SpreadData[] sell = new SpreadData[num];
            for (int i = 0; i < num; i++) {
                sell[i] = buf.Struct(new SpreadData());
            }
        }

        private AccountRec LoadAccount(InBuf buf) {
            return buf.Struct(new AccountRec());
        }

        private ArrayList<DealInternal> LoadDeals(InBuf buf) {
            int updateID = buf.Int();
            int num = buf.Int();
            ArrayList<DealInternal> list = new ArrayList<DealInternal>();
            for (int i = 0; i < num; i++) {
                if (Connection.TradeBuild < 1891) {
                    throw new UnsupportedOperationException();
                }
                DealInternal d = buf.Struct(new DealInternal());
                list.add(d);
            }
            return list;
        }

        private ArrayList<OrderInternal> LoadOrders(InBuf buf) {
            int updateID = buf.Int();
            int num = buf.Int();
            ArrayList<OrderInternal> list = new ArrayList<OrderInternal>();
            for (int i = 0; i < num; i++) {
                if (Connection.TradeBuild < 1891) {
                    throw new CodeException(Code.OLD_VERSION);
                }
                OrderInternal o = buf.Struct(new OrderInternal());
                list.add(o);
            }
            return list;
        }

        private MailRecipient[] LoadMailRecepients(InBuf buf) {
            return buf.Array(new MailRecipient());
        }

        private Map.Entry<ServerRec, ArrayList<Map.Entry<AccessInfo, List<AddressRec>>>> LoadServer(InBuf buf) {
            ServerRec sr = buf.Struct(new ServerRec());
            int num = buf.Int();
            ArrayList<Map.Entry<AccessInfo, List<AddressRec>>> list
                    = new ArrayList<>();
            for (int i = 0; i < num; i++) {
                list.add(LoadAccess(buf));
            }
            return new AbstractMap.SimpleEntry<>(sr, list);
        }


        private Map.Entry<AccessInfo, List<AddressRec>> LoadAccess(InBuf buf) {
            AccessInfo ai = buf.Struct(new AccessInfo());
            int num = buf.Int();
            ArrayList<AddressRec> list = new ArrayList<AddressRec>();
            for (int i = 0; i < num; i++) {
                list.add(buf.Struct(new AddressRec()));
            }
            return new AbstractMap.SimpleEntry<>(ai, list);
        }

        private void LoadTickers(InBuf buf) {
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                if (Connection.SymBuild <= 1036) {
                    throw new CodeException(Code.OLD_VERSION);
                } else {
                    Ticker ticker = buf.Struct(new Ticker());
                }
            }
        }

        private void LoadSymbols(InBuf buf) throws ConnectException {
            buf.SymBuild = Connection.SymBuild;
            LoadSymBase(buf);
            if (Connection.SymBuild >= 4072) {
                LoadSymXX4072(buf);
            }
            Code status = Code.getById(buf.Int());
            if (status == Code.OK) {
                //DeleteDuplicatedSymbols();
                return;
            }
            if (status != Code.DONE) {
                throw new CodeException(status);
            }
            if (Connection.SymBuild <= 1891) {
                throw new CodeException("SymBuild <= 1891", Code.OLD_VERSION);
            }
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                SymbolInfo si = UDT.ReadStruct(buf, new SymbolInfo());
                symbols.Infos.put(si.Name, si);
                SymGroup gr = UDT.ReadStruct(buf, new SymGroup());
                symbols.Groups.put(si.Name, gr);
                symbols.Sessions.put(si.Name, LoadSessions(buf));

                C54 sc54 = UDT.ReadStruct(buf, new C54());
            }
            LoadRemoveList(buf);
            LoadSymbolSets(buf);
        }

        void LoadSymXX4072(InBuf buf) {
            buf.Bytes(656); //vSymXXInfo
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                buf.Bytes(932); //vSymYY
                int count = buf.Int();
                for (int j = 0; j < count; j++)
                    buf.Bytes(160);
            }
        }

        private void LoadRemoveList(InBuf buf) {
            int num = buf.Int();
            int[] ar = new int[num]; //m_SymInfo.m_nId
            for (int i = 0; i < num; i++) {
                ar[i] = buf.Int();
            }
        }

        private void LoadSymbolSets(InBuf buf) {
            Code status = Code.getById(buf.Int());
            if (status == Code.OK) {
                return;
            }
            if (status != Code.DONE) {
                throw new CodeException(status);
            }
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                SymbolSet ss = UDT.ReadStruct(buf, new SymbolSet());
            }
        }

        private SymbolSessions LoadSessions(InBuf buf) {
            ArrayList<ArrayList<Session>> quotes = new ArrayList<>();
            ArrayList<ArrayList<Session>> trades = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                int num = buf.Int();
                ArrayList<Session> ses = new ArrayList<Session>();
                for (int j = 0; j < num; j++) {
                    Session s = UDT.ReadStruct(buf, new Session());
                    ses.add(s);
                }
                quotes.add(ses);
                num = buf.Int();
                ArrayList<Session> tr = new ArrayList<Session>();
                for (int j = 0; j < num; j++) {
                    Session s = UDT.ReadStruct(buf, new Session());
                    tr.add(s);
                }
                trades.add(tr);
            }
            SymbolSessions ss = new SymbolSessions();
            ss.Quotes = quotes;
            ss.Trades = trades;
            return ss;
        }

        private void LoadSymBase(InBuf buf) throws ConnectException {
            short build = Connection.SymBuild;
            if (build <= 2204) {
                //LoadBuild2204(buf);
                if (build > 0)
                    throw new CodeException("SymBuild: " + Connection.SymBuild, Code.OLD_VERSION);
                else
                    throw new CodeException("Connection Reset", Code.NETWORK_ERROR);
                //return;
            }
            LoadLastBuild(buf);
        }

        private void LoadLastBuild(InBuf buf) {
            symbols.Base = UDT.ReadStruct(buf, new SymBaseInfo());
            int num = buf.Int();
            SymGroup[] ar = new SymGroup[num];
            for (int i = 0; i < num; i++) {
                SymGroup gr = UDT.ReadStruct(buf, new SymGroup());
                ar[i] = gr;
            }
            symbols.SymGroups = ar;
            num = buf.Int();
            if (num > 0) {

            }
            for (int i = 0; i < num; i++) {
                LoadSymXX(buf);
            }
        }

        private void LoadSymXX(InBuf buf) {
            buf.Bytes(0x38C); //vSymXXInfo
            int num = buf.Int();
            for (int i = 0; i < num; i++) {
                buf.Bytes(0xA0); //vSymYY
            }
        }
    }

    class OrderHistory {

        public final void Request(LocalDateTime from, LocalDateTime to, int existCount, long historyTime, boolean deals) throws IOException {
            OutBuf buf = new OutBuf();
            if (deals)
                buf.ByteToBuffer((byte) 0x21);
            else
                buf.ByteToBuffer((byte) 0x20);
            buf.LongLongToBuffer(ConvertTo.Long(from));
            buf.LongLongToBuffer(ConvertTo.Long(to));
            if (existCount > 0) {
                buf.LongToBuffer(1);
                buf.LongLongToBuffer(ConvertTo.Long(from));
                buf.LongLongToBuffer(ConvertTo.Long(to));
                buf.LongLongToBuffer(historyTime);
                buf.LongToBuffer(existCount);
                buf.LongToBuffer(0);
                buf.LongToBuffer(0);
                buf.LongToBuffer(0);
            } else
                buf.LongToBuffer(0);
            Connection.SendPacket((byte) 0x65, buf);
        }

        public final void Parse(InBuf buf) {
            byte cmd = buf.Byte();
            if (cmd == 0x20) {
                int[] action = new int[1];
                List<OrderInternal> res = ParseOrders(buf, action);
                ArrayList<Order> list = new ArrayList<Order>();
                for (int i = 0; i < res.size(); i++) {
                    Order order = new Order(res.get(i));
                    list.add(order);
                }
                OrderHistoryEventArgs args = new OrderHistoryEventArgs();
                args.Action = action[0];
                args.Orders = list;
                args.InternalOrders = res;
                OnOrderHisotyCall(args);
            } else if (cmd == 0x21) {
                int[] action = new int[1];
                Map<Long, List<DealInternal>> res = ParseDeals(buf, action);
                List<Order> list = new LinkedList<Order>();
                for (long key : res.keySet()) {
                    List<DealInternal> value = res.get(key);
                    if (value.size() > 1) {
                        if (key == 0) // balance
                            for (DealInternal deal : value)
                                list.add(new Order(deal));
                        else
                            list.add(new Order(value.toArray(new DealInternal[0])));
                    }
                    if (res.get(key).size() == 1)
                        if (value.get(0).Type != DealType.DealBuy && value.get(0).Type != DealType.DealSell)
                            list.add(new Order(value.get(0)));

                }
                List<DealInternal> deals = new LinkedList<>();
                for (long key : res.keySet())
                    deals.addAll(res.get(key));
                OrderHistoryEventArgs args = new OrderHistoryEventArgs();
                args.Action = action[0];
                args.Orders = list;
                args.InternalDeals = deals;
                OnOrderHisotyCall(args);
            } else
                throw new CodeException("Unknown Trade Parse Cmd = 0x" + String.format("%X", cmd & 0xFF), UNKNOWN_ORDER_TYPE);
        }

        private Map<Long, List<DealInternal>> ParseDeals(InBuf buf, int[] action) {
            int updId = buf.Int();
            int num = buf.Int();
            Map<Long, List<DealInternal>> res = new HashMap<Long, List<DealInternal>>();
            for (int i = 0; i < num; i++) {
                LocalDateTime time = ConvertTo.DateTime(buf.Long());
                action[0] = buf.Int();
                if (action[0] == 1)
                    continue;
                if (action[0] == 4) {
                    //RemoveItem(time);
                    //continue;
                }

                Map<Long, List<DealInternal>> map = ParseReceivedDeals(action[0], buf);
                for (long key : map.keySet())
                    if (!res.containsKey(key))
                        res.put(key, map.get(key));
                    else
                        res.get(key).addAll(map.get(key));

            }
            return res;
        }

        private List<OrderInternal> ParseOrders(InBuf buf, int[] action) {
            int updId = buf.Int();
            int num = buf.Int();
            List<OrderInternal> res = new LinkedList<>();
            for (int i = 0; i < num; i++) {
                LocalDateTime time = ConvertTo.DateTime(buf.Long());
                action[0] = buf.Int();
                if (action[0] == 1)
                    continue;
                if (action[0] == 4) {
                    //RemoveItem(time);
                    continue;
                }
                res.addAll(Arrays.asList(ParseReceivedOrders(action[0], buf)));
            }
            return res;
        }

        private Map<Long, List<DealInternal>> ParseReceivedDeals(int action, InBuf buf) {
            if (action == 0) {
                int num = buf.Int();
                long[] tickets = new long[num];
                for (int i = 0; i < num; i++)
                    tickets[i] = buf.Long();
                if (Connection.TradeBuild <= 1892)
                    throw new RuntimeException("TradeBuild <= 1892");
                Map<Long, List<DealInternal>> res = buf.ArrayDeal();
                buf.Bytes(16);
                return res;
            } else {
                Code status = Code.getById(buf.Int());
                Map<Long, List<DealInternal>> res = buf.ArrayDeal();
                return res;
            }
        }

        private OrderInternal[] ParseReceivedOrders(int action, InBuf buf) {
            if (action == 0) {
                int num = buf.Int();
                long[] tickets = new long[num];
                for (int i = 0; i < num; i++)
                    tickets[i] = buf.Long();
                if (Connection.TradeBuild <= 1892)
                    throw new RuntimeException("TradeBuild <= 1892");
                OrderInternal[] res = buf.Array(new OrderInternal());
                buf.Bytes(16);
                return res;
            } else {
                Code status = Code.getById(buf.Int());
                OrderInternal[] res = buf.Array(new OrderInternal());
                return res;
            }
        }


        private <T extends FromBufReader> ArrayList<T> ParseBuf(InBuf buf, T t) // extends FromBufReader
        {
            //buf.Int();
            int updId = buf.Int();
            int num = buf.Int();
            ArrayList<T> res = new ArrayList<T>();
            for (int i = 0; i < num; i++) {
                LocalDateTime time = ConvertTo.DateTime(buf.Long());
                int action = buf.Int();
                if (action == 1) {
                    continue;
                }
                if (action == 4) {
                    //RemoveItem(time);
                    continue;
                }
                //if ((action != 0) && (action != 0xE))
                //{
                long[] tickets = null;
                RefObject<long[]> tempRef_tickets = new RefObject<long[]>(tickets);
                for (Object item : ParseReceivedData(action, buf, tempRef_tickets, t))
                    res.add((T) item);
                tickets = tempRef_tickets.argValue;
                //}
            }
            return res;
        }


        private <T extends FromBufReader> T[] ParseReceivedData(int action, InBuf buf, RefObject<long[]> tickets, T t) {
            int num = buf.Int();
            tickets.argValue = new long[num];
            for (int i = 0; i < num; i++) {
                tickets.argValue[i] = buf.Long();
            }
            if (Connection.TradeBuild <= 1892) {
                throw new UnsupportedOperationException("TradeBuild <= 1892");
            }
            T[] res = buf.<T>Array(t);
            if (action != 0) {
                return res;
            }
            buf.Bytes(16);
            return res;
        }

        private DealInternal LoadDeal(InBuf buf) {
            if (Connection.TradeBuild <= 1892) {
                throw new CodeException("TradeBuild <= 1892", SERVER_BUILD);
            }
            return buf.Struct(new DealInternal());
        }
    }

    class OpenedClosedOrders {

        public final Map<Long, Order> Opened = new ConcurrentHashMap<>();
        public final Map<Long, Order> Closed = new ConcurrentHashMap<>();

        private static CodeException getCodeException(OrderRequest orderRequest
                , TradeRequest tradeRequest
                , TradeResult tradeResult) {
            final String message =
                    "TID:" + (tradeRequest.ExpertId * -1)
                    + "|Ticket:" + orderRequest.getTicket()
                    + "|Symbol:" + tradeRequest.Currency
                    + "|Lot:" + orderRequest.getLot()
                    + "|Price:" + tradeRequest.Price
                    + "|RequestPrice:" + orderRequest.getPrice()
                    + "|Operation:" + orderRequest.getRequestType()
                    + "|OrderType:" + tradeRequest.OrderType
                    + "|TradeType:" + tradeRequest.TradeType;
            return new CodeException(message, tradeResult.Status);
        }

        public final void onOrderUpdate(OrderUpdate update) {
            try {
                if (update.OrderInternal != null) {
                    if (update.OrderInternal.State == OrderState.Placed) {
                        if (update.OrderInternal.DealTicket == 0) {

                            if (Opened.containsKey(update.OrderInternal.TicketNumber)) {
                                Opened.get(update.OrderInternal.TicketNumber).Update(new Order(update.OrderInternal));
                                update.Type = UpdateType.PendingModify;
                                sendOrderEvent(EventTopic.ORDER, null, OrderStateData.Modified
                                        , toOrderTypeData(update.Order.OrderType)
                                        , update.Order, null);
                            } else {
                                Opened.put(update.OrderInternal.TicketNumber, new Order(update.OrderInternal));
                                update.Type = UpdateType.PendingOpen;
                                sendOrderEvent(EventTopic.ORDER, null, OrderStateData.Placed
                                        , toOrderTypeData(update.Order.OrderType)
                                        , update.Order, null);
                            }
                            update.Order = Opened.get(update.OrderInternal.TicketNumber);
                        }

                    } else if (update.OrderInternal.State == OrderState.Cancelled) {
                        if (Opened.containsKey(update.OrderInternal.TicketNumber)) {
                            Opened.get(update.OrderInternal.TicketNumber).Update(new Order(update.OrderInternal));

                            if (!Closed.containsKey(update.OrderInternal.TicketNumber)) {
                                Closed.put(update.OrderInternal.TicketNumber, Opened.get(update.OrderInternal.TicketNumber));
                            }
                            Opened.remove(update.OrderInternal.TicketNumber);
                            update.Order = Closed.get(update.OrderInternal.TicketNumber);
                            update.Type = UpdateType.PendingClose;
                            sendOrderEvent(EventTopic.ORDER, null, OrderStateData.Cancelled
                                    , toOrderTypeData(update.Order.OrderType)
                                    , update.Order, null);
                        }

                    } else if (update.OrderInternal.State == OrderState.Started) {
                        update.Order = new Order(update.OrderInternal);
                        update.Type = UpdateType.Started;
                        var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderSend"));
                        if (request == null) {
                            request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderClose"));
                        }
                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Initial
                                , toOrderTypeData(update.Order.OrderType)
                                , update.Order, null);
                    } else if (update.OrderInternal.State == OrderState.Filled) {
                        if (Opened.containsKey(update.OrderInternal.TicketNumber)) {
                            update.Order = Opened.get(update.OrderInternal.TicketNumber);
                        } else {
                            update.Order = new Order(update.OrderInternal);
                        }
                        update.Type = UpdateType.Filled;
                        if (update.Order.OrderType != OrderType.Buy && update.Order.OrderType != OrderType.Sell) {
                            var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderSend"));
                            sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Filled
                                    , toOrderTypeData(update.Order.OrderType)
                                    , update.Order, null);
                        }
                    } else if (update.OrderInternal.State == OrderState.RequestCancelling) {
                        if (Opened.containsKey(update.OrderInternal.TicketNumber)) {
                            update.Order = Opened.get(update.OrderInternal.TicketNumber);
                        } else {
                            update.Order = new Order(update.OrderInternal);
                        }
                        update.Type = UpdateType.Cancelling;
                        var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderClose"));
                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Cancelled
                                , toOrderTypeData(update.Order.OrderType)
                                , update.Order, null);
                    }
                }
                if (update.Deal != null) {
                    if (update.Deal.Type == DealType.Balance) {
                        update.Order = new Order(update.Deal);
                        update.Type = UpdateType.Balance;
                        eventProducer.eventConsumer(EventTopic.ACCOUNT)
                                .accept(MT5API.this, toAccountData());
                    } else {
                        long ticket = update.Deal.PositionTicket;
                        if (ticket != 0 && update.Deal.OpenTimeMs == update.OppositeDeal.OpenTimeMs) {
                            if (Opened.containsKey(ticket)) {
                                Opened.get(ticket).Update(new Order(update.Deal));
                            } else {
                                Opened.put(ticket, new Order(update.Deal));
                            }
                            update.Order = Opened.get(ticket);
                            update.Type = UpdateType.MarketOpen;
                            var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderSend"));
                            sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Opened
                                    , toOrderTypeData(update.Order.OrderType)
                                    , update.Order, null);
                        } else {
                            if (Opened.containsKey(ticket)) {
                                var oldAccount = Account.Balance;
                                Account.Balance += update.Deal.Profit + update.Deal.Swap + update.Deal.Commission;
                                if (oldAccount != Account.Balance) {
                                    eventProducer.eventConsumer(EventTopic.ACCOUNT)
                                            .accept(MT5API.this, toAccountData());
                                }
                                Order open = Opened.get(ticket);
                                if (update.Deal.Direction == Direction.Out && (update.Deal.PlacedType == PlacedType.OnSL || update.Deal.PlacedType == PlacedType.OnTP || update.Deal.PlacedType == PlacedType.OnStopOut)) {
                                    if (update.Deal.PlacedType == PlacedType.OnSL) {
                                        update.Type = UpdateType.OnStopLoss;
                                    } else if (update.Deal.PlacedType == PlacedType.OnTP) {
                                        update.Type = UpdateType.OnTakeProfit;
                                    } else {
                                        update.Type = UpdateType.OnStopOut;
                                    }
                                    open.UpdateOnStop(update.Deal, false);
                                    if (!Closed.containsKey(ticket)) {
                                        Closed.put(ticket, open);
                                    }
                                    Opened.remove(ticket);
                                    update.Order = open;
                                } else {
                                    Order close = new Order(update.Deal);
                                    Order closed = new Order(new DealInternal[]{update.Deal, update.OppositeDeal});
                                    closed.Lots = closed.CloseVolume;
                                    closed.setOpenPrice(close.OpenPrice);
                                    closed.setOpenTime(close.OpenTime);
                                    closed.ExpertId = close.ExpertId;
                                    closed.setComment(close.Comment);

                                    if (Closed.containsKey(ticket)) {
                                        Closed.get(ticket).Update(closed);
                                    } else {
                                        Closed.put(ticket, closed);
                                    }
                                    if (Math.round(open.Lots * Math.pow(10, 8)) / Math.pow(10, 8) == close.Lots) {
                                        Opened.remove(ticket);
                                        update.Type = UpdateType.MarketClose;
                                        update.Order = Closed.get(ticket);
                                        var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderClose"));
                                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Closed
                                                , toOrderTypeData(update.Order.OrderType)
                                                , update.Order, null);
                                    } else {
                                        open.Lots -= close.Lots;
                                        update.Type = UpdateType.PartialClose;
                                        update.Order = Closed.get(ticket);
                                        var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderClose"));
                                        sendOrderEvent(EventTopic.ORDER, request, OrderStateData.PartialClosed
                                                , toOrderTypeData(update.Order.OrderType)
                                                , update.Order, null);
                                    }
                                }
                            } else if (Opened.containsKey(update.OppositeDeal.PositionTicket)) {
                                Opened.get(update.OppositeDeal.PositionTicket).Update(new Order(update.OppositeDeal));
                                update.Order = Opened.get(update.OppositeDeal.PositionTicket);
                                update.Type = UpdateType.MarketModify;
                                var request = orderRequests.getIfPresent(getRequestKey(update.Order.ExpertId + "_OrderModify"));
                                sendOrderEvent(EventTopic.ORDER, request, OrderStateData.Modified
                                        , toOrderTypeData(update.Order.OrderType)
                                        , update.Order, null);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Api_OnOrderUpdate", e);
            }

        }

        public final void AddDeals(ArrayList<DealInternal> deals) {
            for (DealInternal item : deals) {
                Opened.put(item.TicketNumber, new Order(item));
            }
        }

        public final void AddOrders(ArrayList<OrderInternal> orders) {
            for (OrderInternal item : orders) {
                Opened.put(item.TicketNumber, new Order(item));
            }
        }

        public final void ParseTrades(InBuf buf) {
            byte cmd = buf.Byte();
            switch ((cmd & 0xFF)) {
                case 7: //symbol configuration
                    UpdateSymbols(buf);
                    break;
                case 8: //symbol group configuration
                    UpdateSymbolGroup(buf);
                    break;
                case 9: //group configuration
                    UpdateSymbolsBase(buf);
                    break;
                case 0x11: //tickers
                    UpdateTickers(buf);
                    break;
                case 0x13: //users
                    UpdateAccount(buf);
                    break;
                case 0x1F: //orders
                    UpdateOrders(buf);
                    break;
                case 0x20: //history of orders
                    UpdateHistoryOrders(buf);
                    break;
                case 0x21: //all deals
                    UpdateDeals(buf);
                    break;
                case 0x23: //request
                    UpdateTradeRequest(buf);
                    break;
                case 0x28: //spread config
                    UpdateSpreads(buf);
                    break;
                default:
                    String.valueOf(cmd);
                    break;
            }
        }

        private void UpdateSpreads(InBuf buf) {

        }

        private void UpdateTradeRequest(InBuf buf) {

            int num = buf.Int();
            OrderProgress[] array = new OrderProgress[num];
            int count = buf.getLeft() / num - 1212; // size TransactionInfo + TradeRequest + TradeResult 476
            for (int i = 0; i < num; i++) {
                OrderProgress progress = new OrderProgress();
                progress.OrderUpdate = buf.Struct(new TransactionInfo());
                if (Connection.TradeBuild <= 1891) {
                    throw new CodeException("TradeBuild <= 1891", SERVER_BUILD);
                }
                progress.TradeRequest = buf.Struct(new TradeRequest());
                progress.TradeResult = buf.Struct(new TradeResult());
                //progress.DealsResult = buf.Struct<DealsResult>();
                buf.Bytes(count);
                array[i] = progress;
            }
            OnOrderProgressCall(array);
        }

        private void UpdateDeals(InBuf buf) {
            int num = buf.Int();
            OrderUpdate[] ar = new OrderUpdate[num];
            if (Connection.TradeBuild <= 1892) {
                throw new CodeException("TradeBuild <= 1892", SERVER_BUILD);
            }
            for (int i = 0; i < num; i++) {
                OrderUpdate ou = new OrderUpdate();
                ou.Trans = buf.Struct(new TransactionInfo());
                ou.Deal = buf.Struct(new DealInternal());
                ou.OppositeDeal = buf.Struct(new DealInternal());
                PumpDeals5D8 s5 = buf.Struct(new PumpDeals5D8());
                PumpDeals698 s6 = buf.Struct(new PumpDeals698());
                if (Connection.TradeBuild <= 1241) {
                    continue;
                }
                DealInternal[] deals = buf.Array(new DealInternal());
                DealInternal[] opposite = buf.Array(new DealInternal());
                ar[i] = ou;
            }
            OnOrderUpdateCall(ar);
        }

        private void UpdateHistoryOrders(InBuf buf) {
            int num = buf.Int();
            OrderUpdate[] ar = new OrderUpdate[num];
            for (int i = 0; i < num; i++) {
                if (Connection.TradeBuild <= 1891) {
                    throw new CodeException("TradeBuild <= 1891", SERVER_BUILD);
                }
                OrderUpdate ou = new OrderUpdate();
                ou.Trans = buf.Struct(new TransactionInfo());
                ou.OrderInternal = buf.Struct(new OrderInternal());
                ar[i] = ou;
            }
        }

        private void UpdateOrders(InBuf buf) {
            int num = buf.Int();
            OrderUpdate[] ar = new OrderUpdate[num];
            for (int i = 0; i < num; i++) {
                if (Connection.TradeBuild <= 1891) {
                    throw new CodeException("TradeBuild <= 1891", SERVER_BUILD);
                }
                OrderUpdate ou = new OrderUpdate();
                ou.Trans = buf.Struct(new TransactionInfo());
                ou.OrderInternal = buf.Struct(new OrderInternal());
                ar[i] = ou;
            }
            OnOrderUpdateCall(ar);
        }

        private void UpdateAccount(InBuf buf) {
        }

        private void UpdateTickers(InBuf buf) {
        }

        private void UpdateSymbolsBase(InBuf buf) {
        }

        private void UpdateSymbolGroup(InBuf buf) {
        }

        private void UpdateSymbols(InBuf buf) {
        }

        final void OnOrderUpdateCall(OrderUpdate[] update) {
            for (OrderUpdate item : update) {
                try {
                    orders.onOrderUpdate(item);
                } catch (Exception ex) {
                    log.error("OnOrderUpdateCall", ex);
                }
            }
        }

        void OnOrderProgressCall(OrderProgress[] progr) {
            for (OrderProgress item : progr) {
                try {
                    TradeRequest tradeRequest = item.TradeRequest;
                    TradeResult tradeResult = item.TradeResult;
                    if (tradeRequest != null && tradeResult != null) {
                        String key = getRequestKey(String.valueOf(tradeRequest.RequestId));
                        var request = orderRequests.getIfPresent(key);
                        if (request != null) {
                            if (tradeResult.Status != null
                                && tradeResult.Status != DONE
                                && tradeResult.Status != OK
                                && tradeResult.Status != REQUEST_ACCEPTED
                                && tradeResult.Status != REQUEST_EXECUTED
                                && tradeResult.Status != REQUEST_PROCESSED
                                && tradeResult.Status != REQUEST_EXECUTED_PARTIALLY
                                && tradeResult.Status != REQUEST_EXECUTED_PARTIALLY_) {
                                CodeException error = getCodeException(request, tradeRequest, tradeResult);
                                errorHandler(request, null, error);
                            } else {
                                Event event = OrderEvent.builder()
                                        .eventType(EventType.INFO)
                                        .TID((tradeRequest.ExpertId * -1))
                                        .request(request)
                                        .accountId(getAccountId())
                                        .brokerId(getBrokerId())
                                        .code(tradeResult.Status)
                                        .build();
                                produce(EventTopic.ORDER_REQUEST, event);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.error("OnOrderProgressCall", ex);
                }
            }
        }


    }
}