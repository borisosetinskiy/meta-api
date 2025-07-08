package com.ob.broker.common.client.rest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.Timeout;

@ToString
@RequiredArgsConstructor
public final class RestApiConfig {
    @Getter
    final Long connectTimeout;
    @Getter
    final Long socketTimeout;
    @Getter
    final Integer maxConnTotal;
    @Getter
    final Integer maxPerRoute;
    @Getter
    final Long requestTimeout;
    @Getter
    final Long responseTimeout;
    @Getter
    final Boolean retryEnabled;
    @Getter
    final Integer retryAttempts;
    @ToString.Exclude
    volatile
    PoolingHttpClientConnectionManager manager;

    public PoolingHttpClientConnectionManager manager() {
        if (manager == null) {
            synchronized (PoolingHttpClientConnectionManager.class) {
                if (manager == null) {
                    manager = createConnectionManager();
                }
            }
        }
        return manager;
    }

    private PoolingHttpClientConnectionManager createConnectionManager() {
        var connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(connectTimeout))
                .setSocketTimeout(Timeout.ofSeconds(socketTimeout))
                .build();

        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultTlsConfig(
                        TlsConfig.custom()
                                .setSupportedProtocols(TLS.V_1_3, TLS.V_1_2)
                                .build())
                .setMaxConnTotal(maxConnTotal)
                .setMaxConnPerRoute(maxPerRoute)
                .setDefaultConnectionConfig(connectionConfig)
                .setConnPoolPolicy(PoolReusePolicy.FIFO)
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
                .build();
    }
}
