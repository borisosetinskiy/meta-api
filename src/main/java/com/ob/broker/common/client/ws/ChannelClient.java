package com.ob.broker.common.client.ws;

public interface ChannelClient {
    void connect();

    void reconnect();

    void send(String message);

    void close();

    void clear();

    boolean isDead();

    boolean isOpen();

}
