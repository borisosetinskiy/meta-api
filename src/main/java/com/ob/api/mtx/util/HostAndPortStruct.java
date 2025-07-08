package com.ob.api.mtx.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostAndPortStruct implements ByteBufferValue {
    int port;
    byte[] host;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer = byteBuffer.putInt(port);
        final int hostLength = host.length;
        byteBuffer = byteBuffer.putInt(hostLength);
        byteBuffer.put(host, 0, hostLength);
    }

    @Override
    public void read(ByteBuffer byteBuffer) {
        this.port = byteBuffer.getInt();
        final int hostLength = byteBuffer.getInt();
        this.host = new byte[hostLength];
        byteBuffer.get(host, 0, hostLength);
    }

    @Override
    public int size() {
        return 8 + ((host != null) ? host.length : 0);
    }

    @Override
    public boolean isEmpty() {
        return port == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostAndPortStruct that = (HostAndPortStruct) o;
        return port == that.port && Arrays.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(port);
        result = 31 * result + Arrays.hashCode(host);
        return result;
    }
}
