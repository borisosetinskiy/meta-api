package com.ob.broker.common.client.rest;

import lombok.Getter;

@Getter
public enum HttpCode {
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NO_CONTENT(204),
    MOVED_PERMANENTLY(301),
    FOUND(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),
    TEMPORARY_REDIRECT(307),
    PERMANENT_REDIRECT(308),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NOT_ACCEPTABLE(406),
    REQUEST_TIMEOUT(408),
    CONFLICT(409),
    GONE(410),
    LENGTH_REQUIRED(411),
    PRECONDITION_FAILED(412),
    PAYLOAD_TOO_LARGE(413),
    UNSUPPORTED_MEDIA_TYPE(415),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);

    private final int code;

    HttpCode(int code) {
        this.code = code;
    }

    public static HttpCode fromCode(int code) {
        for (HttpCode httpCode : values()) {
            if (httpCode.code == code) {
                return httpCode;
            }
        }
        return null;
    }
}
