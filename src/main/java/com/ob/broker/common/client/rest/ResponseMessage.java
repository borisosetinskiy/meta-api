package com.ob.broker.common.client.rest;

import java.util.Map;

public record ResponseMessage(String message, int code, Map<String, Object> headers) {
}
