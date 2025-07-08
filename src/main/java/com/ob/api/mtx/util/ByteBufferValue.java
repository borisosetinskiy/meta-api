package com.ob.api.mtx.util;

import java.nio.ByteBuffer;

public interface ByteBufferValue {
    void write(ByteBuffer byteBuffer);

    void read(ByteBuffer byteBuffer);

    int size();

    boolean isEmpty();
}
