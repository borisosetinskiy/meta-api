package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.connector.connection.codec.MT4Crypt;
import com.ob.api.mtx.mt4.connector.connection.codec.vSHA1Java;
import com.ob.api.mtx.mt4.connector.util.ByteArrayUtil;

public class HashPacket implements Delegate {
    @Override
    public byte[] invoke(byte[] hardId, Session cp, Connection connection) {
        vSHA1Java sha = new vSHA1Java();
        sha.hashData(hardId);
        sha.hashData(ByteArrayUtil.intToByteArray(cp.account));
        sha.hashData(connection.getKey());
        sha.hashData(cp.hashKey);
        byte[] hash = sha.finalizeHash();
        System.arraycopy(cp.hashKey, 0, cp.transactionKey1, 0, cp.hashKey.length);

        cp.transactionKey1 = MT4Crypt.encode(cp.transactionKey1, hash);
        cp.transactionKey1 = MT4Crypt.encode(cp.transactionKey1, hardId);
        sha = new vSHA1Java();
        sha.hashData(ByteArrayUtil.intToByteArray(cp.account));
        sha.hashData(connection.getKey());
        sha.hashData(ByteArrayUtil.intToByteArray(cp.session));
        hash = sha.finalizeHash();
        byte[] hdr = new byte[8];
        hdr[0] = 20;
        hdr[2] = 1;
        cp.seed = (cp.seed * 214013) + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        short sShort = (short) ((cp.seed >>> 16) & 0x7FFFL);
        ByteArrayUtil.copyShortToByteArray(sShort, hdr, 6);
        return ConnectionUtil.cryptPacket(hdr, hash, cp.hashKey);
    }
}
