package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.connector.util.ByteArrayUtil;

import java.util.Random;

public class DataPacket implements Delegate {
    public static long randomLong(Random rnd) {
        byte[] buffer = new byte[8];
        rnd.nextBytes(buffer);
        return bytesToLong(buffer);
    }

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = (value << 8) + (bytes[i] & 0xff);
        }
        return value;
    }

    @Override
    public byte[] invoke(byte[] hardId, Session cp, Connection connection) {
        byte[] hdr = new byte[8];
        hdr[4] = 1;
        cp.seed = (cp.seed * 214013) + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        short sShort = (short) ((cp.seed >>> 16) & 0x7FFFL);
        ByteArrayUtil.copyShortToByteArray(sShort, hdr, 6);
        byte[] data;
        if (cp.serverBuild > 1294) {
            data = new byte[68];
            hdr[0] = 68;
            long softid = randomLong(new Random());
            ByteArrayUtil.copyLongToByteArray(softid, data, 60);
        } else {
            data = new byte[60];
            hdr[0] = 60;
        }
        System.arraycopy(hardId, 0, data, 0, hardId.length);
        ByteArrayUtil.copyIntToByteArray(ConnectionData.CLIENT_EXE_SIZE, data, 20);
        ByteArrayUtil.copyIntToByteArray(0xCD75F640, data, 24);
        return ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);
    }
}
