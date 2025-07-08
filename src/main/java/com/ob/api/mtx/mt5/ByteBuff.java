package com.ob.api.mtx.mt5;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.nio.ByteBuffer;
import java.util.Arrays;


@FieldDefaults(level = AccessLevel.PRIVATE)
public final class ByteBuff {
    ByteBuffer buffer;

    public ByteBuff(byte[] bytes) {
        buffer = create(bytes);
    }

    ByteBuffer create(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer = byteBuffer.position(bytes.length - 1);
        return byteBuffer;
    }

    public void add(byte[] bytes) {
        int lastPosition = buffer.position() + bytes.length;
        if (lastPosition > buffer.capacity() - 1) {
            final byte[] buf = Arrays.copyOf(toBytes(), lastPosition + 1);
            System.arraycopy(bytes, 0, buf, buffer.position() + 1,
                    bytes.length);
            buffer = create(buf);
        } else {
            buffer.put(bytes);
        }
    }

    public byte[] toBytes() {
        return buffer.array();
    }


}
