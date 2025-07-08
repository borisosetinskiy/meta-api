package com.ob.api.dx.service;

import com.ob.api.dx.model.UserSession;
import com.ob.api.dx.util.AuthUtil;
import com.ob.api.dx.util.ConnectionUtil;
import com.ob.api.dx.util.ErrorUtil;
import com.ob.api.dx.util.HeaderUtil;
import com.ob.broker.common.client.rest.RestApiClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.util.ExecutorUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Data
public class PingService {
    final RestApiClient restApiClient;
    final ScheduledExecutorService scheduler;
    final int periodInSec;
    final Map<Object, UserSession> userSessions = new ConcurrentHashMap<>();

    public void register(UserSession userSession) {
        userSessions.put(userSession.getAccountId(), userSession);
    }

    public void unregister(Object accountId) {
        userSessions.remove(accountId);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            userSessions.values().forEach(this::ping);
        }, 10, periodInSec, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void ping(UserSession userSession) {
        if (ConnectionUtil.isConnected(userSession.getConnectionStatus().get())
            && userSession.needRefresh()) {
            long start = System.currentTimeMillis();
            try {
                log.info("Ping session {} user {} account {}"
                        , userSession.getToken()
                        , userSession.getUsername()
                        , userSession.getAccountId());
                var url = getPingUrl(userSession.getAuthUrl());
                var headers = new HashMap<String, String>();
                HeaderUtil.setAuthHeader(headers, userSession);
                HeaderUtil.prepareContextTypeHeader(headers);
                var responseMessage = restApiClient.post(url, null, headers);
                ErrorUtil.throwIfError(responseMessage);
                AuthUtil.handleAuth(userSession, responseMessage.message());
                log.info("Ping session {} user {} account {} result: {}"
                        , userSession.getToken()
                        , userSession.getUsername()
                        , userSession.getAccountId()
                        , responseMessage);
            } catch (Exception e) {
                log.error("Error while making ping", e);
                if(e instanceof CodeException codeException) {
                    if(Code.UNAUTHORIZED.getValue() == codeException.getCode()){
                        userSession.unauthorized();
                    }
                }
            }
            ExecutorUtil.sleep(start, 1000);
        }
    }

    private String getPingUrl(String url) {
        return url + "/ping";
    }
}
