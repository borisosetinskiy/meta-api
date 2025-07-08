package com.ob.api.dx.util;

import com.ob.api.dx.model.UserSession;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class HeaderUtil {
    public static void prepareAuthorizedHeader(String token, Map<String, String> headers) {
        headers.put("Authorization", "DXAPI " + token);
    }

    public static void prepareContextTypeHeader(Map<String, String> headers) {
        headers.put("Content-Type", "application/json");
    }

    public static Map<String, String> getContextTypeHeader() {
        return Map.of("Content-Type", "application/json");
    }

    public static void setETagHeader(Map<String, String> headers, UserSession userSession) {
        headers.put("If-None-Match", userSession.getEtag().get());
    }

    public static void getETagHeader(Map<String, Object> headers, UserSession userSession) {
        var eTag = headers.get("ETag");
        if (eTag != null) {
            userSession.getEtag().getAndSet((String) eTag);
        }
    }

    public static void setAuthHeader(Map<String, String> headers, UserSession userSession) {
        if (!userSession.isValid()) {
            throw new CodeException("User session is not valid", Code.UNAUTHORIZED);
        }
        prepareAuthorizedHeader(userSession.getToken(), headers);
    }
}
