package com.ob.broker.common.client.rest;

import java.util.Map;

public interface RestApiClient {
    ResponseMessage get(String url, Map<String, String> headers);

    ResponseMessage post(String url, String body, Map<String, String> headers);

    ResponseMessage put(String url, String body, Map<String, String> headers);

    ResponseMessage delete(String url, Map<String, String> headers);
}
