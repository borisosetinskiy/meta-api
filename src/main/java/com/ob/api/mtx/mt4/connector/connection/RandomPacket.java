package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.connector.util.ByteArrayUtil;

public class RandomPacket implements Delegate {
    @Override
    public byte[] invoke(byte[] hardId, Session cp, Connection connection) {
        byte[] hdr = new byte[8];
        cp.seed = (cp.seed * 214013) + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        int cnt = (int) ((cp.seed >>> 16) & 0x1FL) + 8;
        ByteArrayUtil.copyShortToByteArray((short) cnt, hdr, 0);
        hdr[2] = 4;
        cp.seed = (cp.seed * 214013) + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        short sShort = (short) ((cp.seed >>> 16) & 0x7FFFL);
        ByteArrayUtil.copyShortToByteArray(sShort, hdr, 6);
        byte[] data = new byte[cnt];
        for (int i = 0; i < cnt; i++) {
            cp.seed = (cp.seed * 214013) + 2531011;
            cp.seed &= ConnectionUtil.MAX_INT_VALUE;
            data[i] = (byte) ((cp.seed >>> 16) & 0xFFL);
        }
        return ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);
    }
}
