package com.ob.api.dx.service;

import com.ob.api.dx.model.UserSession;
import com.ob.api.dx.model.request.LoginRequest;
import com.ob.api.dx.util.AuthUtil;
import com.ob.api.dx.util.ErrorUtil;
import com.ob.api.dx.util.HeaderUtil;
import com.ob.api.dx.util.HttpUtil;
import com.ob.broker.common.JsonService;
import com.ob.broker.common.client.rest.RestApiClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class AuthenticationService {
    final RestApiClient restApiClient;

    public void login(String password, UserSession userSession) {
        try {
            final LoginRequest loginRequest = new LoginRequest(
                    userSession.getUsername()
                    , userSession.getDomain()
                    , password);
            var url = getLoginUrl(userSession.getAuthUrl());
            var body = JsonService.JSON.toJson(loginRequest);
            var headers = HeaderUtil.getContextTypeHeader();
            var responseMessage = restApiClient.post(url, body, headers);
            ErrorUtil.throwIfError(responseMessage);
            AuthUtil.handleAuth(userSession, responseMessage.message());
        } catch (Exception e) {
            throw new CodeException(e.getMessage(), Code.UNAUTHORIZED);
        }
    }

    public void logout(UserSession userSession) {
        try {
            var url = getLogoutUrl(userSession.getAuthUrl());
            HttpUtil.post(url, userSession, restApiClient, null, responseMessage -> {
                userSession.setToken(null);
                log.info("Logout {} account {} result: {}", userSession.getUsername(), userSession.getAccountId(), responseMessage);
            });
        } catch (Exception e) {
            log.error("Error while making logout {} account {}", userSession.getUsername(), userSession.getAccountId(), e);
        }
    }

    private String getLoginUrl(String url) {
        return url + "/login";
    }

    private String getLogoutUrl(String url) {
        return url + "/logout";
    }
}
/*
curl -X POST --header 'Content-Type: application/json' --header
'Accept: application/json'
--header 'Authorization: DXAPI 1l490nfj2bv5md7b7minla7fur' '
https://demo.dx.trade/dxsca-web/logout'
 */