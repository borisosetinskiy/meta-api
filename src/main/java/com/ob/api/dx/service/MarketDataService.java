package com.ob.api.dx.service;

import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.common.DelayedRunnableProcessor;
import com.ob.api.dx.model.request.message.ServerEvent;
import com.ob.api.dx.model.request.message.quote.MarketDataCloseSubscriptionRequest;
import com.ob.api.dx.model.request.message.quote.MarketDataSubscriptionRequest;
import com.ob.broker.common.JsonService;
import com.ob.broker.common.client.ws.ChannelClient;
import com.ob.broker.common.client.ws.ChannelListener;
import com.ob.broker.common.client.ws.ExternalWSClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.event.EventType;
import com.ob.broker.common.event.QuoteOn;
import com.ob.broker.common.event.SubscribeEvent;
import com.ob.broker.common.model.ChannelStatus;
import com.ob.broker.common.model.QuoteData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Slf4j
@Data
public class MarketDataService implements ChannelListener {
    BaseDxApi baseDxApi;
    ChannelClient marketDatachannel;
    DelayedRunnableProcessor delayedRunnableProcessor;
    final Map<String, Integer> subscriptions = new ConcurrentHashMap<>();
    final AtomicInteger requestIdCounter = new AtomicInteger();
    final Map<String, QuoteData> lastQuote = new ConcurrentHashMap<>();
    final Map<String, String> requestedSymbols = new ConcurrentHashMap<>();
    final AtomicBoolean allowed = new AtomicBoolean();

    public MarketDataService(BaseDxApi baseDxApi, boolean allowed) {
        this.baseDxApi = baseDxApi;
        setAllowed(allowed);
        init();
    }

    public void setAllowed(boolean allowed) {
        this.allowed.set(allowed);
    }
    public void init(){
        if (allowed.get()) {
            if(marketDatachannel == null){
                try {
                    URI uri = new URI(getMarketDataUrl());
                    this.marketDatachannel = new ExternalWSClient( uri, this.getClass().getSimpleName()+":"+this.getBrokerId()+"_"+this.getAccountId() ,this);
                } catch (Exception e) {
                    throw new CodeException(e, Code.INVALID_NETWORK_CONFIG);
                }
            }
            if(delayedRunnableProcessor == null){
                this.delayedRunnableProcessor = new DelayedRunnableProcessor("MarketData"
                        , 100
                        , baseDxApi.getAccountId()
                        , baseDxApi.getExecutorManager().event()
                        , baseDxApi.getMetricService());
            }
        }
    }

    public void subscribe(String symbol) {
        if (!allowed.get()) {
            throw new CodeException("Market data is not allowed", Code.INVALID_STATE);
        }
        if (subscriptions.containsKey(symbol)) {
            return;
        }
        var requestId = requestIdCounter.incrementAndGet();
        subscriptions.put(symbol, requestId);
        requestedSymbols.put(String.valueOf(requestId), symbol);
        MarketDataSubscriptionRequest request = new MarketDataSubscriptionRequest();
        request.setSession(baseDxApi.getUserSession().getToken());
        request.setRequestId(String.valueOf(requestId));
        MarketDataSubscriptionRequest.Payload payload = new MarketDataSubscriptionRequest.Payload();
        payload.setSymbols(List.of(symbol));
        payload.setAccount(baseDxApi.getUserSession().getAccountCode());
        request.setPayload(payload);
        send(request);
    }
    /*
     var event = new SubscribeErrorEvent(getBrokerId(), getAccountId(), subscribe.symbol, true, error, EventType.BROKEN_CHANNEL);
                        produce(EventTopic.SUBSCRIBE, event);
     */

    public void unsubscribe(String symbol) {
        if (!allowed.get()) {
            throw new CodeException("Market data is not allowed", Code.INVALID_STATE);
        }
        Integer requestId = subscriptions.remove(symbol);
        if (requestId != null) {
            MarketDataCloseSubscriptionRequest request = new MarketDataCloseSubscriptionRequest();
            request.setRefRequestId(String.valueOf(requestId));
            var rId = String.valueOf(requestIdCounter.incrementAndGet());
            request.setRequestId(rId);
            requestedSymbols.put(rId, symbol);
            request.setSession(baseDxApi.getUserSession().getToken());
            send(request);
        }
    }

    void send(Object request) {
        if (!allowed.get()) {
            throw new CodeException("Market data is not allowed", Code.INVALID_STATE);
        }
        if (!marketDatachannel.isOpen()) {
            throw new CodeException("Market data channel is not open", Code.INVALID_STATE);
        }
        try {
            var message = JsonService.JSON.toJson(request);
            delayedRunnableProcessor.add(() -> marketDatachannel.send(message));
        } catch (Exception e) {
            throw new CodeException(e.getMessage(), Code.INVALID_JSON_FORMAT);
        }
    }


    String getMarketDataUrl() {
        return baseDxApi.getUrlConfig().getMessageWebSocketUrl() + "/md";
    }

    @Override
    public void onOpen() {
        log.info("Market data channel is open accountId {}", getAccountId());
        QuoteOn quoteOn = new QuoteOn();
        quoteOn.setBrokerId(getBrokerId());
        quoteOn.setAccountId(getAccountId());
        baseDxApi.getEventProducer().eventConsumer(EventTopic.CONNECT)
                .accept(baseDxApi, quoteOn);
    }

    public boolean isOpen() {
        if (!allowed.get()) {
            return true;
        }
        return marketDatachannel.isOpen();
    }

    @Override
    public void onClose(ChannelStatus status) {
        log.info("Market data channel is status {} accountId {} is dead {}", status, getAccountId(), marketDatachannel.isDead());
    }

    @Override
    public void onEvent(String eventMessage) {
        try {
            var event = JsonService.JSON.fromJson(eventMessage, ServerEvent.class);
            event.execute(baseDxApi, marketDatachannel, (BiConsumer<String, Object>) (name, o) -> {
                        switch (name) {
                            case "MarketData":
                                QuoteData quoteData = (QuoteData) o;
                                lastQuote.put(quoteData.getSymbol(), quoteData);
                                break;
                            case "MarketDataSubscriptionClosed":
                                String requestId = String.valueOf(o);
                                var symbol = requestedSymbols.get(requestId);
                                if (symbol != null) {
                                    subscriptions.remove(symbol);
                                    SubscribeEvent subscribeEvent = new SubscribeEvent();
                                    subscribeEvent.setActive(false);
                                    subscribeEvent.setEventType(EventType.SUBSCRIBE);
                                    subscribeEvent.setSymbol(symbol);
                                    subscribeEvent.setBrokerId(getBrokerId());
                                    subscribeEvent.setAccountId(getAccountId());
                                    baseDxApi.getEventProducer().eventConsumer(EventTopic.SUBSCRIBE)
                                            .accept(baseDxApi, subscribeEvent);
                                }
                                break;
                            case "permission":
                                log.info("Market data permission event {} accountId {}", o, getAccountId());
                                allowed.set(false);
                                try {
                                    marketDatachannel.close();
                                } catch (Exception ignored) {
                                }
                                break;
                            default:
                                log.warn("Unhandled event {} accountId {}", name, getAccountId());
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Error processing event {} accountId {}", eventMessage, getAccountId(), e);
        }
    }


    private Object getAccountId() {
        return baseDxApi.getAccountId();
    }

    private Long getBrokerId() {
        return baseDxApi.getBrokerId();
    }

    @Override
    public void onError(Exception e) {
        log.warn("Market data channel error {} accountId {}", e, getAccountId());
    }

    public void start() {
        if (!allowed.get()) {
            return;
        }
        delayedRunnableProcessor.start();
        marketDatachannel.connect();
        log.info("Market data channel started accountId {}", getAccountId());
    }

    public void stop() {
        try{
            delayedRunnableProcessor.stop();
        }catch (Exception ignored){}
        if (!allowed.get()) {
            return;
        }
        try {
            marketDatachannel.close();
        } catch (Exception ignored) {
        }
    }

    public void clear(){

    }
}
