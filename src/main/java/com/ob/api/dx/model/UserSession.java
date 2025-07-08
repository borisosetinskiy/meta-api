package com.ob.api.dx.model;

import com.ob.broker.common.model.ConnectionStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Data
public class UserSession {
    final AtomicReference<String> etag = new AtomicReference<>();
    Long brokerId;
    Object accountId;
    String username;
    String token;
    String accountCode;
    String domain;
    String authUrl;
    AtomicReference<ConnectionStatus> connectionStatus = new AtomicReference<>(ConnectionStatus.OFFLINE);
    AtomicLong lastActivity = new AtomicLong(0);
    long sessionTimeout = 2 * 60 * 1000;

    public boolean isValid() {
        return brokerId != null && accountId != null
               && token != null
               && connectionStatus.get() == ConnectionStatus.ONLINE;
    }

    public String getId() {
        return brokerId + "-" + accountId;
    }

    public boolean needRefresh() {
        return System.currentTimeMillis() - lastActivity.get() > sessionTimeout - (sessionTimeout * 0.4);
    }

    public boolean isUnauthorized() {
        return connectionStatus.get() == ConnectionStatus.UNAUTHORIZED;
    }

    public void unauthorized(){
        connectionStatus.set(ConnectionStatus.UNAUTHORIZED);
        log.error("Unauthorized session {} user {} account {}"
                , token
                , username
                , accountId);
        lastActivity.set(System.currentTimeMillis());
    }

    public void refresh() {
        log.info("Refresh session {} user {} account {}"
                , token
                , username
                , accountId);
        lastActivity.set(System.currentTimeMillis());
    }

    public void clean() {
        etag.set(null);
        lastActivity.getAndSet(0);
        brokerId = null;
        accountId = null;
        token = null;
        accountCode = null;
        domain = null;
        username = null;
    }
}
