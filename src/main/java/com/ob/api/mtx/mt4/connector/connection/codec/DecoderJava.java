package com.ob.api.mtx.mt4.connector.connection.codec;

public class DecoderJava {
    private byte last = 0;
    private int hashInd = 0;
    private byte[] hash;

    public DecoderJava(byte[] hash) {
        this.hash = hash;
    }

    public synchronized void changeKey(byte[] hash) {
        this.hash = hash;
    }

    public byte[] getKey() {
        return this.hash;
    }

    public synchronized void reset() {
        this.last = 0;
        this.hashInd = 0;
    }

    public synchronized byte[] decode(byte[] buf, int offset, int len) {
        byte[] res = new byte[len];
        for (int i = offset; i < offset + len; i++) {
            hashInd &= 0xF;
            byte hashByte = hash[hashInd];
            res[i - offset] = (byte) (buf[i] ^ (Byte.toUnsignedInt(last) + Byte.toUnsignedInt(hashByte)));
            hashInd++;
            last = res[i - offset];
        }
        return res;
    }

    public synchronized void decode(byte[] buf) {
        int len = buf.length;
        for (int i = 0; i < len; i++) {
            hashInd &= 0xF;
            byte hashByte = hash[hashInd];
            buf[i] = (byte) (buf[i] ^ (Byte.toUnsignedInt(last) + Byte.toUnsignedInt(hashByte)));
            hashInd++;
            last = buf[i];
        }
    }


}
