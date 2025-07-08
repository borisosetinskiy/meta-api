package com.ob.api.mtx.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class HostAndPortStructs implements ByteBufferValue, Comparable<HostAndPortStructs> {
    final List<HostAndPortStruct> hosts = new ArrayList<>();
    long timestamp;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putLong(timestamp);
        byteBuffer.putInt(size());
        for (HostAndPortStruct struct : hosts) {
            struct.write(byteBuffer);
        }
        byteBuffer.position(0);
    }

    @Override
    public void read(ByteBuffer byteBuffer) {
        timestamp = byteBuffer.getLong();
        final int size = byteBuffer.getInt();
        for (int i = 12; i < size; ) {
            final HostAndPortStruct struct = new HostAndPortStruct();
            try {
                struct.read(byteBuffer);
                hosts.add(struct);
            } catch (Exception ignored) {
            }
            i += struct.size();
        }
    }

    @Override
    public int size() {
        int size = 0;
        if (!hosts.isEmpty()) {
            size = 12;
            for (HostAndPortStruct struct : hosts) {
                size += struct.size();
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return hosts.isEmpty();
    }


    @Override
    public int compareTo(HostAndPortStructs o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
