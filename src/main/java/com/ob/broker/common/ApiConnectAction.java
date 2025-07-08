package com.ob.broker.common;

import com.ob.broker.common.model.ConnectionStatus;

public interface ApiConnectAction {
    ConnectionStatus getConnectionStatus();

    void update(ConnectionStatus connectionStatus);

    boolean isConnected();

    boolean isDead();

    void fireError(Throwable throwable);

    void fireReconnectError(Throwable throwable);

    void fireConnect() throws Exception;

    Long getBrokerId();

    Object getAccountId();
}
