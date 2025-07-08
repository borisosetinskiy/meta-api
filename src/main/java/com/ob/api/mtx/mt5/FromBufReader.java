package com.ob.api.mtx.mt5;

public interface FromBufReader {
    Object ReadFromBuf(InBuf buf);

    default String GetString(byte[] buf) {
        return UDT.readString(buf, 0, buf.length);
    }
}