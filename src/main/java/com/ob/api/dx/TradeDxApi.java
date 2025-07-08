package com.ob.api.dx;

import com.google.common.cache.Cache;
import com.ob.api.dx.model.data.DxOrderData;
import com.ob.api.dx.model.data.UrlConfig;
import com.ob.api.dx.model.request.inner.DxPendingRequest;
import com.ob.api.dx.model.request.inner.DxPositionRequest;
import com.ob.api.dx.service.AuthenticationService;
import com.ob.api.dx.service.PingService;
import com.ob.api.dx.service.TradingService;
import com.ob.broker.common.ITradeBaseApi;
import com.ob.broker.common.MetricService;
import com.ob.broker.common.client.rest.RestApiClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.AccountSnapshot;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.event.EventType;
import com.ob.broker.common.event.SnapshotEvent;
import com.ob.broker.common.model.AccountData;
import com.ob.broker.common.model.CloseOrderData;
import com.ob.broker.common.model.OrderData;
import com.ob.broker.common.request.GroupOrderRequest;
import com.ob.broker.common.request.IRequest;
import com.ob.broker.common.request.OrderRequest;
import com.ob.broker.service.LocalEntryService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString(of = {"tradingService"})
public class TradeDxApi extends BaseDxApi implements ITradeBaseApi {
    TradingService tradingService;


    public TradeDxApi(UrlConfig urlConfig, ExecutorManager executorManager
            , PingService pingService
            , AuthenticationService authenticationService
            , RestApiClient restApiClient, MetricService metricService, Cache<String, OrderRequest> orderRequests) {
        super(urlConfig, executorManager, pingService, authenticationService, restApiClient, metricService);
        final LocalEntryService localEntryService = new LocalEntryService(this
                , eventProducer
                , (connectorId, task) -> getExecutorManager().outcome().submit(task));
        tradingService = new TradingService(1000
                , this
                , localEntryService
                , metricService
                , orderRequests);
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if (tradingService != null) {
            tradingService.stop();
        }
    }

    protected void clear() {
        super.clear();
        if (tradingService != null) {
            tradingService.clear();
        }
    }

    @Override
    public boolean isConnected() {
        return super.isConnected() && tradingService.isOpen();
    }

    @Override
    public void simulateConnect() {
        super.simulateConnect();
        snapshot();
    }

    @Override
    protected void snapshot() {
        super.snapshot();
        SnapshotEvent<CloseOrderData> closeEvent = new SnapshotEvent<>();
        var close = tradingService.getClosedOrders().values().stream().toList();
        closeEvent.setBrokerId(getBrokerId());
        closeEvent.setAccountId(getAccountId());
        closeEvent.setEventType(EventType.LOAD_CLOSE);
        closeEvent.setData(close);
        produce(EventTopic.LOAD, closeEvent);
        SnapshotEvent<DxOrderData> openEvent = new SnapshotEvent<>();
        var opened = tradingService.opened().values().stream().toList();
        openEvent.setBrokerId(getBrokerId());
        openEvent.setAccountId(getAccountId());
        openEvent.setEventType(EventType.LOAD_OPEN);
        openEvent.setData(opened);
        produce(EventTopic.LOAD, openEvent);
        SnapshotEvent<DxOrderData> pendingEvent = new SnapshotEvent<>();
        var pending = tradingService.pending().values().stream().toList();
        pendingEvent.setBrokerId(getBrokerId());
        pendingEvent.setAccountId(getAccountId());
        pendingEvent.setEventType(EventType.LOAD_PENDING);
        pendingEvent.setData(pending);
        produce(EventTopic.LOAD, pendingEvent);
        AccountSnapshot accountSnapshot = new AccountSnapshot();
        accountSnapshot.setBalance(tradingService.getAccountData().getBalance());
        accountSnapshot.setCurrency(tradingService.getAccountData().getCurrency());
        accountSnapshot.setAccountId(getAccountId());
        accountSnapshot.setBrokerId(getBrokerId());
        accountSnapshot.setVersion(tradingService.getAccountData().getVersion());
        accountSnapshot.setCredit(tradingService.getAccountData().getCredit());
        produce(EventTopic.LOAD, accountSnapshot);
    }

    @Override
    protected void load() {
        super.load();
        tradingService.loadAccount();
        tradingService.loadHistory();
        tradingService.start();
        snapshot();
    }

    @Override
    public void execute(List<IRequest> requests) {
        requests.forEach(this::execute);
    }

    @Override
    public void execute(IRequest request) {
        if (request instanceof OrderRequest orderRequest) {
            orderRequest.setAccountId(getUserSession().getAccountId());
            orderRequest.setBrokerId(getUserSession().getBrokerId());
            tradingService.execute(orderRequest);
        } else if (request instanceof GroupOrderRequest groupOrderRequest) {
            groupOrderRequest.setAccountId(getUserSession().getAccountId());
            groupOrderRequest.setBrokerId(getUserSession().getBrokerId());
            tradingService.execute(groupOrderRequest);
        } else if (request instanceof DxPositionRequest dxPositionRequest) {
            dxPositionRequest.setAccountId(getUserSession().getAccountId());
            dxPositionRequest.setBrokerId(getUserSession().getBrokerId());
            tradingService.execute(dxPositionRequest);
        } else if (request instanceof DxPendingRequest dxPendingRequest) {
            dxPendingRequest.setAccountId(getUserSession().getAccountId());
            dxPendingRequest.setBrokerId(getUserSession().getBrokerId());
            tradingService.execute(dxPendingRequest);
        } else {
            log.error("Unknown request type: {}", request);
            throw new CodeException("Unknown request type", Code.REQUEST_REJECTED);
        }
    }

    @Override
    public List<? extends OrderData> opened() {
        return tradingService.opened().values().stream().toList();
    }

    @Override
    public List<? extends OrderData> pending() {
        return tradingService.pending().values().stream().toList();
    }

    @Override
    public List<CloseOrderData> closed() {
        return tradingService.getClosedOrders().values().stream().toList();
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
        var order = tradingService.getOpened().get(ticket);
        if (order == null) {
            order = tradingService.getPending().get(ticket);
        }
        return order;
    }

    @Override
    public List<OrderData> findOrders(List<Long> tickets) {
        return tickets.stream().map(this::findOrder).collect(Collectors.toList());
    }

    @Override
    public AccountData getAccountData() {
        return tradingService.getAccountData();
    }
}
