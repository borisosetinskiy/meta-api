package com.ob.broker.service;

public interface ReconnectAgent {
    void reconnect();

    boolean isConnected();

    boolean isDead();

    String getId();

}
