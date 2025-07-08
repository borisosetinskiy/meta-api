package com.ob.api.dx;

import com.ob.api.dx.model.UserSession;
import com.ob.api.dx.model.data.UrlConfig;
import com.ob.api.dx.service.AuthenticationService;
import com.ob.api.dx.service.InstrumentService;
import com.ob.api.dx.service.MarketDataService;
import com.ob.api.dx.service.PingService;
import com.ob.api.dx.util.ConnectionUtil;
import com.ob.broker.common.*;
import com.ob.broker.common.client.rest.RestApiClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.*;
import com.ob.broker.common.model.ConnectionStatus;
import com.ob.broker.common.model.ContractData;
import com.ob.broker.service.ReconnectService;
import com.ob.broker.util.ErrorUtil;
import com.ob.broker.util.ExecutorUtil;
import com.ob.broker.util.LogUtil;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ob.broker.common.error.Code.*;

@Slf4j
@Data
@ToString(of = {"apiCredentials"})
public class BaseDxApi implements IBaseApi, ApiConnectAction {
    protected final AtomicReference<DxApiCredentials> apiCredentials = new AtomicReference<>(
            new DxApiCredentials(null, 0L, 0L, ""));
    final ExecutorManager executorManager;
    final PingService pingService;
    final AuthenticationService authenticationService;
    final RestApiClient restApiClient;
    final UrlConfig urlConfig;
    final MetricService metricService;
    final Lock connectLock = new ReentrantLock();
    final Key key = new Key();
    final UserSession userSession = new UserSession();
    final AtomicBoolean criticalError = new AtomicBoolean();
    final Lock setingLock = new ReentrantLock();
    final ReconnectService reconnect;
    EventProducer eventProducer;
    InstrumentService instrumentService;
    MarketDataService marketDataService;

    public BaseDxApi(UrlConfig urlConfig
            , ExecutorManager executorManager
            , PingService pingService
            , AuthenticationService authenticationService
            , RestApiClient restApiClient, MetricService metricService) {
        this.executorManager = executorManager;
        this.metricService = metricService;
        this.reconnect = new ReconnectService(Set.of(ACCOUNT_DISABLED.getValue()
                , OLD_VERSION.getValue()
                , UNALLOWED_COMPANY.getValue()
                , INVALID_ACCOUNT.getValue()
                , INVESTOR_PASSWORD.getValue()), Set.of(), this);
        this.pingService = pingService;
        this.eventProducer = new EventProducer((connectorId, task) -> executorManager.income().submit(task), key);
        this.authenticationService = authenticationService;
        this.restApiClient = restApiClient;
        this.urlConfig = urlConfig;
        executorManager.scheduler().scheduleAtFixedRate(() -> {
            try {
                reconnect.reconnect();
                if (userSession.isUnauthorized()) {
                    notifyConnectError(new CodeException("Unauthorized", UNAUTHORIZED), EventType.CONNECT);
                }
            } catch (Exception e) {
                log.error("Reconnect issue account {}", getAccountId(), e);
            }
        }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
        eventProducer.listener(EventTopic.CONNECT, new EventConsumer() {
            @Override
            public void onNext(EventTopic topic, IBaseApi api, Event event) {

                if (!ConnectionUtil.isConnected(getUserSession().getConnectionStatus().get())
                    && event instanceof LogonEvent logonEvent) {
                    if (logonEvent.getEventType() == EventType.LOGON) {
                        LogUtil.log("LOGON", "api", getAccountId()
                                , getBrokerId(),
                                text ->
                                        log.info("{}", text));
                        setStatus(ConnectionStatus.ONLINE);
                        var userSession = getUserSession();
                        pingService.register(userSession);
                        try {
                            load();
                            var connectEvent = new ConnectEvent(getBrokerId(), getAccountId());
                            produce(EventTopic.CONNECT, connectEvent);
                        } catch (Exception e) {
                            notifyConnectError(e, EventType.CONNECT);
                        }
                    }
                } else if (event instanceof ConnectEvent connectEvent) {
                    if (connectEvent.getEventType() == EventType.CONNECT) {
                        LogUtil.log("CONNECTED", "api", getAccountId()
                                , getBrokerId(),
                                text ->
                                        log.info("{}", text));
                    }
                } else if (event instanceof GeneralErrorEvent generalErrorEvent) {
                    if (generalErrorEvent.getEventType() == EventType.RECONNECT
                        || generalErrorEvent.getEventType() == EventType.DISCONNECT) {
                        var userSession = getUserSession();
                        pingService.unregister(userSession.getAccountId());
                    }
                }

            }

            @Override
            public String getId() {
                return "connect-" + getAccountId()+"-" + getBrokerId();
            }
        });
        marketDataService = new MarketDataService(this, true);
    }

    protected void init() {
        getUserSession().clean();
        var domain = apiCredentials.get().getDomain();
        var userName = apiCredentials.get().getUserName();
        var accountId = apiCredentials.get().getAccountId();
        var brokerId = apiCredentials.get().getBrokerId();
        log.info("Init session domain {} user {} ", domain, userName);
        var userSession = getUserSession();
        userSession.setDomain(domain);
        userSession.setUsername(userName);
        userSession.getConnectionStatus().set(ConnectionStatus.OFFLINE);
        userSession.setBrokerId(brokerId);
        userSession.setAuthUrl(urlConfig.getAuthUrl());
        userSession.setAccountId(accountId);
        userSession.setAccountCode(domain + ":" + accountId);
        key.setAccountId(userSession.getAccountId());
        key.setBrokerId(userSession.getBrokerId());
        this.instrumentService = new InstrumentService(urlConfig.getRestApiUrl()
                , userSession, restApiClient);
    }

    @Override
    public void update(ConnectionStatus connectionStatus) {
        getUserSession().getConnectionStatus().set(connectionStatus);
    }

    @Override
    public boolean isDead() {
        return ConnectionUtil.isDead(getUserSession().getConnectionStatus().get());
    }

    @Override
    public void fireError(Throwable throwable) {
        var error = ErrorUtil.toError(throwable);
        var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, EventType.RECONNECT);
        produce(EventTopic.ERROR, event);
    }

    void produce(EventTopic topic, Event event) {
        try {
            eventProducer.eventConsumer(topic)
                    .accept(BaseDxApi.this, event);
        } catch (Exception e) {
            log.warn("Event {} topic {} issue ", event, topic, e);
        }
    }

    @Override
    public void fireReconnectError(Throwable throwable) {
        var error = ErrorUtil.toError(throwable);
        var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, EventType.RECONNECT);
        produce(EventTopic.CONNECT, event);
        logout();
    }

    @Override
    public void fireConnect() throws Exception {
        connect();
    }

    @Override
    public IBaseApi setApiCredentials(ApiCredentials apiCredentials) {
        if (apiCredentials instanceof DxApiCredentials dxApiCredentials) {
            this.apiCredentials.getAndSet(dxApiCredentials);
            return this;
        }
        throw new CodeException("Wrong ApiCredentials", Code.INVALID_DATA);
    }

    @Override
    public IBaseApi setApiSetting(ApiSetting apiSetting) {
        reconnect.reconnectConfig.setDisabled(apiSetting.isDisableReconnect());
        if (apiSetting.isDisableReconnect()) {
            if (apiSetting.getMaxConnectAttempt() != null)
                reconnect.reconnectConfig.setMaxReconnectionAttempt(apiSetting.getMaxConnectAttempt());
            if (apiSetting.getMaxConnectOnWeekendAttempt() != null)
                reconnect.reconnectConfig.setMaxReconnectionOnWeekendAttempt(apiSetting.getMaxConnectOnWeekendAttempt());
        }
        return this;
    }

    @Override
    public void connect(ApiCredentials apiCredentials) {
        setApiCredentials(apiCredentials);
        connect();
    }

    @Override
    public void connect() {
        try {
            clear();
        } catch (Exception ignored) {
        }
        setStatus(ConnectionStatus.CONNECTING);

        try {
            init();
            var password = apiCredentials.get().getPassword();
            authenticationService.login(password, userSession);
            var event = new LogonEvent(getBrokerId(), getAccountId());
            produce(EventTopic.CONNECT, event);
        } catch (Exception e) {
            notifyConnectError(e, EventType.CONNECT);
        }
    }

    @Override
    public void simulateConnect() {
        var event = new LogonEvent(getBrokerId(), getAccountId());
        produce(EventTopic.CONNECT, event);
    }

    protected void snapshot() {
        SnapshotEvent<ContractData> event = new SnapshotEvent<>();
        var instruments = instrumentService.getInstruments().values().stream().toList();
        event.setEventType(EventType.LOAD_INSTRUMENT);
        event.setData(instruments);
        event.setBrokerId(getBrokerId());
        event.setAccountId(getAccountId());
        produce(EventTopic.LOAD, event);
    }

    protected void load() {
        loadInstruments();
        marketDataService.start();
        log.info("Load complete accountId {}", getAccountId());
    }

    private void loadInstruments() {
        long start = System.currentTimeMillis();
        for (InstrumentService.Type type : InstrumentService.Type.values()) {
            instrumentService.load(type);
            ExecutorUtil.sleep(500);
        }
        metricService.record("load_instruments", System.currentTimeMillis() - start);
    }

    @Override
    public void disconnect() {
        log.info("Disconnecting brokerId {} accountId {}", getBrokerId(), getAccountId());

        logout();
        try {

            var event = new DisconnectEvent(getBrokerId(), getAccountId());
            produce(EventTopic.CONNECT, event);
            marketDataService.stop();
        } finally {
            setStatus(ConnectionStatus.DEAD);
        }
    }

    public void logout() {
        try {
            authenticationService.logout(userSession);
            clear();
        } catch (Exception ignored) {
        }
    }

    protected void clear() {
        if (marketDataService != null) {
            marketDataService.clear();
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
    public boolean isConnected() {
        return ConnectionUtil.isConnected(getUserSession().getConnectionStatus().get())
               && marketDataService.isOpen();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return getUserSession().getConnectionStatus().get();
    }

    @Override
    public void subscribe(String symbol, boolean critical) {
        subscribe(symbol);
    }

    @Override
    public void subscribe(String symbol) {
        marketDataService.subscribe(symbol);
    }

    @Override
    public void unsubscribe(String symbol) {
        marketDataService.unsubscribe(symbol);
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
        return instrumentService.getInstruments().values().stream().toList();
    }

    @Override
    public ContractData getContractData(String symbol) {
        return instrumentService.getInstruments().get(symbol);
    }

    @Override
    public void shutdown() {
        disconnect();
    }

    protected void notifyConnectError(Throwable throwable, EventType eventType) {
        if (!isDead()) {
            criticalError.getAndSet(true);
            setStatus(ConnectionStatus.OFFLINE);
            var error = ErrorUtil.toError(throwable);
            var event = GeneralErrorEvent.of(getBrokerId(), getAccountId(), error, eventType);
            produce(EventTopic.CONNECT, event);
        }
    }

    private void setStatus(ConnectionStatus s) {
        LogUtil.log("connection-status-update", "api", getAccountId()
                , getBrokerId(),
                text ->
                        log.info("{}, \"status\":\"{}\"", text, s));
        getUserSession().getConnectionStatus().getAndSet(s);
    }
}
