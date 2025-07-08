package com.ob.api.mtx.mt4.connector.connection.codec;

public class EncoderJava {
    private byte last = 0;
    private int hashInd = 0;
    private byte[] hash;

    public EncoderJava(byte[] hash) {
        this.hash = hash;
    }

    public  void changeKey(byte[] hash) {
        this.hash = hash;
    }

    public byte[] getKey() {
        return this.hash;
    }

    public  void reset() {
        this.last = 0;
        this.hashInd = 0;
    }

    public  byte[] encode(final byte[] buf) {
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            hashInd &= 0xF;
            byte hashByte = hash[hashInd];
            res[i] = (byte) (buf[i] ^ (Byte.toUnsignedInt(last) + Byte.toUnsignedInt(hashByte)));
            hashInd++;
            last = buf[i];
        }
        return res;
    }
}
