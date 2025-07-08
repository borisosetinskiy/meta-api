package com.ob.broker.common.client.ws;

import com.ob.broker.common.model.ChannelStatus;

public interface ChannelListener {
    void onOpen();

    void onClose(ChannelStatus status);

    void onEvent(String event);

    void onError(Exception e);
}
