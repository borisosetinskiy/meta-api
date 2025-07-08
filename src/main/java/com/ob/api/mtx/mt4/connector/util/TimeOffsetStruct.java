package com.ob.api.mtx.mt4.connector.util;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class TimeOffsetStruct {
    public long offset;//8
    public byte[] key;

    public TimeOffsetStruct() {
    }

    public TimeOffsetStruct(long offset, byte[] key) {
        this.offset = offset;
        this.key = key;
    }


    public void write(ByteBuffer byteBuffer) {
        byteBuffer = byteBuffer.putLong(offset);
        byteBuffer = byteBuffer.putInt(key.length);
        byteBuffer.put(key, 0, key.length);
    }

    public void read(ByteBuffer byteBuffer) {
        this.offset = byteBuffer.getLong();
        int length = byteBuffer.getInt();
        this.key = new byte[length];
        byteBuffer.get(key, 0, length);
    }

    public int size() {
        return 12 + ((key != null) ? key.length : 0);
    }

    @Override
    public String toString() {
        return "TimeOffsetStruct{" +
               "offset=" + offset +
               ", key=" + Arrays.toString(key) +
               ", keyString=" + (key != null ? key : "NoN") +
               '}';
    }
}
