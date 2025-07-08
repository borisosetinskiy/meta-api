package com.ob.broker.common.client.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.RequestAbortedException;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.http.HttpStatusCode;

import java.io.IOException;

@Slf4j
public record CustomRetryStrategy(int maxRetries, TimeValue retryInterval) implements HttpRequestRetryStrategy {

    @Override
    public boolean retryRequest(
            final HttpRequest request,
            final IOException exception,
            final int execCount,
            final HttpContext context) {
        if (exception != null) {
            if (exception instanceof ConnectionRequestTimeoutException
                || exception instanceof ConnectTimeoutException
                || exception instanceof RequestAbortedException
                || exception instanceof RequestFailedException)
                return execCount <= this.maxRetries;
        }
        return false;
    }

    @Override
    public boolean retryRequest(
            final HttpResponse response,
            final int execCount,
            final HttpContext context) {

        if (response != null && HttpStatusCode.valueOf(response.getCode()).is5xxServerError())
            return execCount <= this.maxRetries;
        return false;
    }

    @Override
    public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
        log.info("Retrying HTTP request after {}", retryInterval.toString());
        return retryInterval;
    }
}