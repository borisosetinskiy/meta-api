package com.ob.broker.common;

import com.ob.broker.common.model.HostPort;

import java.util.function.Consumer;

public interface RouterApi {
    void getHostPort(Long id, Consumer<HostPort> consumer);
}
