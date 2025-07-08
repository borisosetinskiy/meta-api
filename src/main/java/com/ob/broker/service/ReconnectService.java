package com.ob.broker.service;

import com.ob.broker.common.ApiConnectAction;
import com.ob.broker.common.IBaseApi;
import com.ob.broker.common.event.*;
import com.ob.broker.common.model.ConnectionStatus;
import com.ob.broker.util.LogUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.ob.broker.common.event.EventTopic.CONNECT;

@Slf4j
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReconnectService implements EventConsumer, ReconnectAgent {
    final Random random = new Random();
    final String id = UUID.randomUUID().toString().replace("-", String.valueOf((char) random.nextInt(100) + 1));
    public final ReconnectConfig reconnectConfig = new ReconnectConfig();
    final Set<Integer> codes;
    final Set<Throwable> throwable;
    final ApiConnectAction apiConnectAction;
    final AtomicReference<ReconnectStatus> reconnectStatus = new AtomicReference<>(ReconnectStatus.INIT);
    final AtomicInteger reconnectId = new AtomicInteger(-1);
    final AtomicInteger reconnectCounter = new AtomicInteger(0);
    final AtomicLong startReconnect = new AtomicLong();
    final AtomicReference<Exception> exception = new AtomicReference<>();

    boolean isTryReconnect() {
        return ReconnectStatus.WAITING.equals(reconnectStatus.get())
               && !apiConnectAction.isDead()
               && !apiConnectAction.isConnected()
                ;
    }

    public void reconnect() {
        if (reconnectConfig.isDisabled()) return;
        var status = isTryReconnect();
        var allowTime = (System.currentTimeMillis() - startReconnect.get() > ReconnectConfig.calculateDelay());
        if (!status || !allowTime)
            return;
        changeStatus(ReconnectStatus.RUNNING);
        apiConnectAction.update(ConnectionStatus.RECONNECTING);
        startReconnect.getAndSet(System.currentTimeMillis());
        try {
            final int attemptCount = reconnectCounter.get();
            if (attemptCount > reconnectConfig.attempt()) {
                apiConnectAction.fireReconnectError(exception.get());
                return;
            }
            apiConnectAction.fireConnect();
            LogUtil.log("reconnect", "api", apiConnectAction.getAccountId()
                    , apiConnectAction.getBrokerId(),
                    text -> log.info("{}, \"action\":\"fire-connect\", \"attempt\":\"{}\""
                            , text
                            , reconnectCounter.getAndIncrement()
                            , exception.get()));
        } catch (Exception e1) {
            apiConnectAction.fireError(e1);
        }
    }

    @Override
    public boolean isConnected() {
        return apiConnectAction.isConnected();
    }

    @Override
    public boolean isDead() {
        return apiConnectAction.isDead();
    }

    @Override
    public String getId() {
        return id;
    }


    @Override
    public String toString() {
        return "ReconnectService";
    }

    @Override
    public void onNext(EventTopic topic, IBaseApi api, Event event) {
        if (reconnectConfig.isDisabled() || apiConnectAction.isDead()) return;
        if (CONNECT.equals(topic)) {
            if (event instanceof ConnectEvent) {
                if (EventType.CONNECT.equals(event.getEventType())) {
                    changeStatus(ReconnectStatus.DONE);
                    reconnectCounter.getAndSet(0);
                    exception.getAndSet(null);
                }
            } else if (event instanceof GeneralErrorEvent errorEvent) {
                if (EventType.CONNECT.equals(event.getEventType())
                    || EventType.PING.equals(event.getEventType())
                    || EventType.TEST.equals(event.getEventType())
                ) {
                    var error = errorEvent.getError();
                    if (codes.contains(error.getCode()) || throwable.contains(errorEvent.getError())) {
                        LogUtil.log("reconnect-skipped", "api", apiConnectAction.getAccountId()
                                , apiConnectAction.getBrokerId(), text
                                        -> log.info("{}, \"status\":\"{}\", \"error\":\"{}\"", text
                                        , reconnectStatus.get(), error.getMessage()));
                        apiConnectAction.fireReconnectError(error);
                    } else {
                        if (!ReconnectStatus.WAITING.equals(reconnectStatus.get())) {
                            exception.getAndSet(error);
                            changeStatus(ReconnectStatus.WAITING);
                            LogUtil.log("reconnect-triggered", "api", apiConnectAction.getAccountId()
                                    , apiConnectAction.getBrokerId(), text
                                            -> log.info("{}, \"online\":\"{}\", \"error\":\"{}\"", text
                                            , api.isConnected(), error.getMessage()));
                        }
                    }
                }
            }
        }
    }

    public void changeStatus(ReconnectStatus status) {
        var previousStatus = reconnectStatus.getAndSet(status);
        LogUtil.log("change-reconnect-status", "api", apiConnectAction.getAccountId()
                , apiConnectAction.getBrokerId(), text
                        -> log.info("{}, \"previous-status\":\"{}\", \"new--status\":\"{}\"", text
                        , previousStatus, status));
    }


}
