package com.ob.broker.common.client.rest;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ToString
public class RestApache5Client implements RestApiClient {
    final RestApiConfig restApiConfig;
    CloseableHttpClient client;

    public RestApache5Client(RestApiConfig restApiConfig) {
        this.restApiConfig = restApiConfig;
        var requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(restApiConfig.getRequestTimeout()))
                .setResponseTimeout(Timeout.ofSeconds(restApiConfig.getResponseTimeout())).build();
        var builder = HttpClients.custom();
        if (restApiConfig.getRetryEnabled())
            builder.setRetryStrategy(new CustomRetryStrategy(restApiConfig.getRetryAttempts(), Timeout.ofMilliseconds(100)));
        builder.setConnectionManager(restApiConfig.manager());
        builder.setDefaultRequestConfig(requestConfig);
        builder.setConnectionManagerShared(true);
        this.client = builder.build();
    }

    static void toHeaders(ClassicHttpRequest request, Map<String, String> headers) {
        headers.forEach(request::addHeader);
    }

    static void fromHeaders(ClassicHttpResponse response, Map<String, Object> headers) {
        for (Header header : response.getHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
    }

    ResponseMessage exchange(ClassicHttpRequest httpRequest, HttpContext context, String url, Map<String, String> headers) {
        try {
            toHeaders(httpRequest, headers);
            return client.execute(httpRequest, context, response -> {
                var message = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                Map<String, Object> responseHeaders = new HashMap<>();
                fromHeaders(response, responseHeaders);
                return new ResponseMessage(message, response.getCode(), responseHeaders);
            });
        } catch (CodeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while making request url {}", url, e);
            throw new CodeException(e.getMessage(), Code.COMMON_ERROR);
        }
    }

    @Override
    public ResponseMessage get(String url, Map<String, String> headers) {
        var httpRequest = ClassicRequestBuilder.get(url).build();
        return exchange(httpRequest, null, url, headers);
    }

    @Override
    public ResponseMessage post(String url, String body, Map<String, String> headers) {
        ClassicHttpRequest httpRequest = null;
        if (body == null)
            httpRequest = ClassicRequestBuilder.post(url).build();
        else
            httpRequest = ClassicRequestBuilder.post(url).setEntity(body).build();
        return exchange(httpRequest, null, url, headers);
    }

    @Override
    public ResponseMessage put(String url, String body, Map<String, String> headers) {
        var httpRequest = ClassicRequestBuilder.put(url).setEntity(body).build();
        return exchange(httpRequest, null, url, headers);
    }

    @Override
    public ResponseMessage delete(String url, Map<String, String> headers) {
        var httpRequest = ClassicRequestBuilder.delete(url).build();
        return exchange(httpRequest, null, url, headers);

    }
}
