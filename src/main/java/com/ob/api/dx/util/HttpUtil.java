package com.ob.api.dx.util;

import com.ob.api.dx.model.UserSession;
import com.ob.broker.common.client.rest.ResponseMessage;
import com.ob.broker.common.client.rest.RestApiClient;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@UtilityClass
public class HttpUtil {
    public static void get(String url
            , UserSession userSession
            , RestApiClient restApiClient
            , Consumer<ResponseMessage> responseMessageConsumer) {
        log.info("GET URL: {} ", url);
        exchange(url, userSession, restApiClient::get, responseMessageConsumer);
    }

    public static void post(String url
            , UserSession userSession
            , RestApiClient restApiClient
            , String body
            , Consumer<ResponseMessage> responseMessageConsumer) {
        log.info("POST URL: {} BODY: {}", url, body);
        exchange(url, userSession, (url1, headers) -> restApiClient.post(url1, body, headers), responseMessageConsumer);
    }

    public static void post(String url
            , UserSession userSession
            , RestApiClient restApiClient
            , Consumer<ResponseMessage> responseMessageConsumer) {
        post(url, userSession, restApiClient, null, responseMessageConsumer);
    }

    public static void delete(String url
            , UserSession userSession
            , RestApiClient restApiClient
            , Consumer<ResponseMessage> responseMessageConsumer) {
        log.info("DELETE URL: {} ", url);
        exchange(url, userSession, restApiClient::delete, responseMessageConsumer);
    }

    public static void put(String url
            , UserSession userSession
            , RestApiClient restApiClient
            , String body
            , Consumer<ResponseMessage> responseMessageConsumer) {
        log.info("PUT URL: {} BODY: {}", url, body);
        exchange(url, userSession, (url1, headers) -> restApiClient.put(url1, body, headers), responseMessageConsumer);
    }

    private static void exchange(String url
            , UserSession userSession
            , HttpCall call
            , Consumer<ResponseMessage> consumer) {
        var headers = new HashMap<String, String>();
        HeaderUtil.setAuthHeader(headers, userSession);
        HeaderUtil.prepareContextTypeHeader(headers);
        HeaderUtil.setETagHeader(headers, userSession);
        var responseMessage = call.call(url, headers);
        ErrorUtil.throwIfError(responseMessage);
        consumer.accept(responseMessage);
        HeaderUtil.getETagHeader(responseMessage.headers(), userSession);
    }

    @FunctionalInterface
    private interface HttpCall {
        ResponseMessage call(String url, Map<String, String> headers);
    }
}
