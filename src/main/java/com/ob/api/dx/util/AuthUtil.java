package com.ob.api.dx.util;

import com.ob.api.dx.model.UserSession;
import com.ob.api.dx.model.response.AuthResponse;
import com.ob.broker.common.JsonService;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthUtil {
    public static void handleAuth(UserSession userSession, String responseMessage) {
        AuthResponse authResponse = JsonService.JSON.fromJson(responseMessage, AuthResponse.class);
        var token = authResponse.getSessionToken();
        long timeout = TimeUtil.parseSessionTimeOut(authResponse.getTimeout());
        userSession.setSessionTimeout(timeout);
        userSession.setToken(token);
        userSession.refresh();
    }

}
