package com.ob.broker.common.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@Data
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostPort {
    public final static HostPort DEFAULT = new HostPort("unknown", 0);
    final String host;
    final int port;
    boolean valid = true;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HostPort hostPort = (HostPort) o;
        return port == hostPort.port && Objects.equals(host, hostPort.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
