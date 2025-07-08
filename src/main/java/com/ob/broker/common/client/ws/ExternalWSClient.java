package com.ob.broker.common.client.ws;

import com.ob.broker.common.model.ChannelStatus;
import com.ob.broker.common.model.ConnectionStatus;
import com.ob.broker.util.ErrorUtil;
import com.ob.broker.util.ExecutorUtil;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ExternalWSClient extends WebSocketClient implements ChannelClient {
    final String name;
    final ChannelListener listener;
    final AtomicBoolean dead = new AtomicBoolean(false);

    public ExternalWSClient(URI serverUri, String name, ChannelListener listener) {
        super(serverUri);
        this.name = name;
        this.listener = listener;
    }

    public void connect() {
        if (isOpen()) {
            super.reconnect();
        } else {
            super.connect();
        }
    }


    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        listener.onOpen();
    }

    @Override
    public void onMessage(String s) {
        listener.onEvent(s);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        ConnectionStatus connectionStatus = dead.get() ? ConnectionStatus.DEAD
                : remote ? ConnectionStatus.REMOTE : ConnectionStatus.OFFLINE;
        listener.onClose(new ChannelStatus(code, reason, connectionStatus));
        log.info("Connection closed with code: {} dead {}, reason: {}",  code, dead.get(), reason);
        if (!dead.get()) {
            ExecutorUtil.getExecutorService().execute(() -> {
                try {
                    if (remote) {
                        ExecutorUtil.sleep(1000);
                    }
                    reconnect();
                } catch (Exception e) {
                    log.error("Error reconnecting", e);
                    onError(e);
                }
            });
        }
    }

    @Override
    public void onError(Exception e) {
        var error = ErrorUtil.toError(e);
        listener.onError(error);
    }

    @Override
    public void close() {
        dead.getAndSet(true);
        log.info("Name {} Closing connection with dead {}", name,  dead.get());
        super.close();
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isDead() {
        return dead.get();
    }

    @Override
    public void send(String message) {
        log.info("Name {} Sending message: {}", name,  message);
        super.send(message);
    }


}
