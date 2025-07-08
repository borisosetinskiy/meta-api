package com.ob.api.mtx.mt4.connector.connection.codec;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class vAESJava {
    private int m_nCipherRnd;
    private long[] m_Ks = new long[64];             //KeySchedule
    private long[] m_Ke = new long[64];             //KeyEncoded
    private long[][] s_tabIT = new long[4][256];
    private long[][] s_tabFT = new long[4][256];
    private long[] s_tabIB = new long[256];
    private long[] s_tabSB = new long[256];

    public vAESJava() {
        Arrays.fill(m_Ks, 0);
        Arrays.fill(m_Ke, 0);
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        String data = "boroasdkm2asdl0klasd2";
        String key = "asdmm2s9kd";
        vAESJava aesJava = new vAESJava();
        byte[] dataByte = data.getBytes();
        byte[] s = aesJava.encryptData(dataByte, key.getBytes());
        byte[] res = aesJava.decryptData(s, key.getBytes());


    }

    private void encodeKey(byte[] key, int szKey) {
        if (szKey > 256)
            return;
        if (s_tabSB[0] == 0)
            generateTables();
        for (int i = 0; i < szKey / 32; i++)
            m_Ks[i] = CryptTools.getReversedIntFromBytes(ByteBuffer.wrap(key), i * 4);
        long v;
        int w = 1;
        int indKs;
        if (szKey == 128) {
            for (int i = 0; i < 2; i++) {
                m_Ks[i * 20 + 4] = (((((s_tabSB[bKs(i * 80 + 12)] << 8) ^ s_tabSB[bKs(i * 80 + 15)]) << 8) ^
                                     s_tabSB[bKs(i * 80 + 14)]) << 8) ^ s_tabSB[bKs(i * 80 + 13)] ^ m_Ks[i * 20] ^ w;
                m_Ks[i * 20 + 5] = m_Ks[i * 20 + 1] ^ m_Ks[i * 20 + 4];
                m_Ks[i * 20 + 6] = m_Ks[i * 20 + 1] ^ m_Ks[i * 20 + 2] ^ m_Ks[i * 20 + 4];
                m_Ks[i * 20 + 7] = m_Ks[i * 20 + 3] ^ m_Ks[i * 20 + 6];
                v = w;
                w = (int) (((v << 1) ^ (((w & 0x80) != 0) ? 0x1B : 0)) & 0xFFL);
                m_Ks[i * 20 + 8] = (((((s_tabSB[bKs(i * 80 + 28)] << 8) ^ s_tabSB[bKs(i * 80 + 31)]) << 8) ^
                                     s_tabSB[bKs(i * 80 + 30)]) << 8) ^ s_tabSB[bKs(i * 80 + 29)] ^ m_Ks[i * 20 + 4] ^ w;
                m_Ks[i * 20 + 9] = m_Ks[i * 20 + 5] ^ m_Ks[i * 20 + 8];
                m_Ks[i * 20 + 10] = m_Ks[i * 20 + 5] ^ m_Ks[i * 20 + 6] ^ m_Ks[i * 20 + 8];
                m_Ks[i * 20 + 11] = m_Ks[i * 20 + 7] ^ m_Ks[i * 20 + 10];
                v = w;
                w = (int) (((v << 1) ^ (((w & 0x80) != 0) ? 0x1B : 0)) & 0xFFL);
                m_Ks[i * 20 + 12] = (((((s_tabSB[bKs(i * 80 + 44)] << 8) ^ s_tabSB[bKs(i * 80 + 47)]) << 8) ^
                                      s_tabSB[bKs(i * 80 + 46)]) << 8) ^ s_tabSB[bKs(i * 80 + 45)] ^ m_Ks[i * 20 + 8] ^ w;
                m_Ks[i * 20 + 13] = m_Ks[i * 20 + 9] ^ m_Ks[i * 20 + 12];
                m_Ks[i * 20 + 14] = m_Ks[i * 20 + 9] ^ m_Ks[i * 20 + 10] ^ m_Ks[i * 20 + 12];
                m_Ks[i * 20 + 15] = m_Ks[i * 20 + 11] ^ m_Ks[i * 20 + 14];
                v = w;
                w = (int) (((v << 1) ^ (((w & (byte) 0x80) != 0) ? 0x1B : 0)) & 0xFFL);
                m_Ks[i * 20 + 16] = (((((s_tabSB[bKs(i * 80 + 60)] << 8) ^ s_tabSB[bKs(i * 80 + 63)]) << 8) ^
                                      s_tabSB[bKs(i * 80 + 62)]) << 8) ^ s_tabSB[bKs(i * 80 + 61)] ^ m_Ks[i * 20 + 12] ^ w;
                m_Ks[i * 20 + 17] = m_Ks[i * 20 + 13] ^ m_Ks[i * 20 + 16];
                m_Ks[i * 20 + 18] = m_Ks[i * 20 + 13] ^ m_Ks[i * 20 + 14] ^ m_Ks[i * 20 + 16];
                m_Ks[i * 20 + 19] = m_Ks[i * 20 + 15] ^ m_Ks[i * 20 + 18];
                v = w;
                w = (int) (((v << 1) ^ (((w & (byte) 0x80) != 0) ? 0x1B : 0)) & 0xFFL);
                m_Ks[i * 20 + 20] = (((((s_tabSB[bKs(i * 80 + 76)] << 8) ^ s_tabSB[bKs(i * 80 + 79)]) << 8) ^
                                      s_tabSB[bKs(i * 80 + 78)]) << 8) ^ s_tabSB[bKs(i * 80 + 77)] ^ m_Ks[i * 20 + 16] ^ w;
                m_Ks[i * 20 + 21] = m_Ks[i * 20 + 17] ^ m_Ks[i * 20 + 20];
                m_Ks[i * 20 + 22] = m_Ks[i * 20 + 17] ^ m_Ks[i * 20 + 18] ^ m_Ks[i * 20 + 20];
                m_Ks[i * 20 + 23] = m_Ks[i * 20 + 19] ^ m_Ks[i * 20 + 22];
                v = w;
                w = (int) (((v << 1) ^ (((w & (byte) 0x80) != 0) ? 0x1B : 0)) & 0xFFL);
            }
            m_nCipherRnd = 10;
            indKs = 80 * 2;
        } else if (szKey == 196) {
            for (int i = 0; i < 2; i++) {
                m_Ks[i * 24 + 6] = (((((s_tabSB[bKs(i * 96 + 20)] << 8) ^ s_tabSB[bKs(i * 96 + 23)]) << 8) ^
                                     s_tabSB[bKs(i * 96 + 22)]) << 8) ^ s_tabSB[bKs(i * 96 + 21)] ^ m_Ks[i * 24] ^ w;
                m_Ks[i * 24 + 7] = m_Ks[i * 24 + 1] ^ m_Ks[i * 24 + 6];
                m_Ks[i * 24 + 8] = m_Ks[i * 24 + 1] ^ m_Ks[i * 24 + 2] ^ m_Ks[i * 24 + 6];
                m_Ks[i * 24 + 9] = m_Ks[i * 24 + 3] ^ m_Ks[i * 24 + 8];
                m_Ks[i * 24 + 10] = m_Ks[i * 24 + 3] ^ m_Ks[i * 24 + 4] ^ m_Ks[i * 24 + 8];
                m_Ks[i * 24 + 11] = m_Ks[i * 24 + 5] ^ m_Ks[i * 24 + 10];
                w <<= 1;
                m_Ks[i * 24 + 12] = (((((s_tabSB[bKs(i * 96 + 44)] << 8) ^ s_tabSB[bKs(i * 96 + 47)]) << 8) ^
                                      s_tabSB[bKs(i * 96 + 46)]) << 8) ^ s_tabSB[bKs(i * 96 + 45)] ^ m_Ks[i * 24 + 6] ^ w;
                m_Ks[i * 24 + 13] = m_Ks[i * 24 + 7] ^ m_Ks[i * 24 + 12];
                m_Ks[i * 24 + 14] = m_Ks[i * 24 + 7] ^ m_Ks[i * 24 + 8] ^ m_Ks[i * 24 + 12];
                m_Ks[i * 24 + 15] = m_Ks[i * 24 + 9] ^ m_Ks[i * 24 + 14];
                m_Ks[i * 24 + 16] = m_Ks[i * 24 + 9] ^ m_Ks[i * 24 + 10] ^ m_Ks[i * 24 + 14];
                m_Ks[i * 24 + 17] = m_Ks[i * 24 + 11] ^ m_Ks[i * 24 + 16];
                w <<= 1;
                m_Ks[i * 24 + 18] = (((((s_tabSB[bKs(i * 96 + 68)] << 8) ^ s_tabSB[bKs(i * 96 + 71)]) << 8) ^
                                      s_tabSB[bKs(i * 96 + 70)]) << 8) ^ s_tabSB[bKs(i * 96 + 69)] ^ m_Ks[i * 24 + 12] ^ w;
                m_Ks[i * 24 + 19] = m_Ks[i * 24 + 13] ^ m_Ks[i * 24 + 18];
                m_Ks[i * 24 + 20] = m_Ks[i * 24 + 13] ^ m_Ks[i * 24 + 14] ^ m_Ks[i * 24 + 18];
                m_Ks[i * 24 + 21] = m_Ks[i * 24 + 15] ^ m_Ks[i * 24 + 20];
                m_Ks[i * 24 + 22] = m_Ks[i * 24 + 15] ^ m_Ks[i * 24 + 16] ^ m_Ks[i * 24 + 20];
                m_Ks[i * 24 + 23] = m_Ks[i * 24 + 17] ^ m_Ks[i * 24 + 22];
                w <<= 1;
                m_Ks[i * 24 + 24] = (((((s_tabSB[bKs(i * 96 + 92)] << 8) ^ s_tabSB[bKs(i * 96 + 95)]) << 8) ^
                                      s_tabSB[bKs(i * 96 + 94)]) << 8) ^ s_tabSB[bKs(i * 96 + 93)] ^ m_Ks[i * 24 + 18] ^ w;
                m_Ks[i * 24 + 25] = m_Ks[i * 24 + 19] ^ m_Ks[i * 24 + 18];
                m_Ks[i * 24 + 26] = m_Ks[i * 24 + 19] ^ m_Ks[i * 24 + 20] ^ m_Ks[i * 24 + 18];
                m_Ks[i * 24 + 27] = m_Ks[i * 24 + 21] ^ m_Ks[i * 24 + 26];
                m_Ks[i * 24 + 28] = m_Ks[i * 24 + 21] ^ m_Ks[i * 24 + 22] ^ m_Ks[i * 24 + 26];
                m_Ks[i * 24 + 29] = m_Ks[i * 24 + 23] ^ m_Ks[i * 24 + 28];
                w <<= 1;
            }
            m_nCipherRnd = 12;
            indKs = 96 * 2;
        } else if (szKey == 256) {
            for (int i = 0; i < 7; i++) {
                m_Ks[i * 8 + 8] = (((((s_tabSB[bKs(i * 32 + 28)] << 8) ^ s_tabSB[bKs(i * 32 + 31)]) << 8) ^
                                    s_tabSB[bKs(i * 32 + 30)]) << 8) ^ s_tabSB[bKs(i * 32 + 29)] ^ m_Ks[i * 8] ^ w;
                m_Ks[i * 8 + 9] = m_Ks[i * 8 + 1] ^ m_Ks[i * 8 + 8];
                m_Ks[i * 8 + 10] = m_Ks[i * 8 + 1] ^ m_Ks[i * 8 + 2] ^ m_Ks[i * 8 + 8];
                m_Ks[i * 8 + 11] = m_Ks[i * 8 + 3] ^ m_Ks[i * 8 + 10];
                w <<= 1;
                m_Ks[i * 8 + 12] = (((((s_tabSB[bKs(i * 32 + 44)] << 8) ^ s_tabSB[bKs(i * 32 + 47)]) << 8) ^
                                     s_tabSB[bKs(i * 32 + 46)]) << 8) ^ s_tabSB[bKs(i * 32 + 45)] ^ m_Ks[i * 8 + 4];
                m_Ks[i * 8 + 13] = m_Ks[i * 8 + 5] ^ m_Ks[i * 8 + 12];
                m_Ks[i * 8 + 14] = m_Ks[i * 8 + 5] ^ m_Ks[i * 8 + 6] ^ m_Ks[i * 8 + 12];
                m_Ks[i * 8 + 15] = m_Ks[i * 8 + 7] ^ m_Ks[i * 8 + 14];
            }
            m_nCipherRnd = 14;
            indKs = 32 * 7;
        } else {
            m_nCipherRnd = 0;
            return;
        }
        m_Ke[0] = m_Ks[indKs / 4];
        m_Ke[1] = m_Ks[indKs / 4 + 1];
        m_Ke[2] = m_Ks[indKs / 4 + 2];
        m_Ke[3] = m_Ks[indKs / 4 + 3];
        int ind = 0;
        for (int i = (m_nCipherRnd - 1) * 4; i > 0; i--, ind++) {
            indKs += (((i & 3) != 0) ? 1 : -7) * 4;
            m_Ke[ind + 4] = s_tabIT[3][(int) s_tabSB[bKs(indKs + 15)]] ^ s_tabIT[2][(int) s_tabSB[bKs(indKs + 14)]] ^
                            s_tabIT[1][(int) s_tabSB[bKs(indKs + 13)]] ^ s_tabIT[0][(int) s_tabSB[bKs(indKs + 12)]];
        }
        m_Ke[ind + 4] = m_Ks[indKs / 4 - 4];
        m_Ke[ind + 5] = m_Ks[indKs / 4 - 3];
        m_Ke[ind + 6] = m_Ks[indKs / 4 - 2];
        m_Ke[ind + 7] = m_Ks[indKs / 4 - 1];
    }

    public int upr(int x) {
        return (x << 8) | (x >>> (32 - 8));
    }

    public void generateTables() {
        int[] log = new int[256];
        int[] pow = new int[256];
        log[0] = 0;
        int w = 1;
        for (int i = 0; i < 256; i++) {
            int v = w;
            log[v] = i;
            pow[i] = w;
            w ^= ((v << 1) ^ (((w & (byte) 0x80) != 0) ? 0x1B : 0)) & 0xFF;
        }
        pow[255] = 0;
        for (int i = 0; i < 256; i++) {
            int v = pow[255 - log[i]] & 0xFF;
            w = ((((((((v >>> 1) ^ v) >>> 1) ^ v) >>> 1) ^ v) >>> 4) ^
                 (((((((v << 1) ^ v) << 1) ^ v) << 1) ^ v) << 1) ^ v ^ 0x63) & 0xFF;
            s_tabSB[i] = Integer.toUnsignedLong(w);
            s_tabIB[w] = Integer.toUnsignedLong(i);
        }
        for (int i = 0; i < 256; i++) {
            int v1 = (int) s_tabSB[i] & 0xFF;
            int v2 = (v1 << 1) & 0xFF;
            if ((v1 & 0x80) != 0)
                v2 ^= 0x1B;
            long wt = Integer.toUnsignedLong(((v1 ^ v2) << 24) | (v1 << 16) | (v1 << 8) | v2);
            s_tabFT[0][i] = wt;
            wt = upr((int) wt);
            s_tabFT[1][i] = wt;
            wt = upr((int) wt);
            s_tabFT[2][i] = wt;
            wt = upr((int) wt);
            s_tabFT[3][i] = wt;
            wt = 0;
            int v = (int) s_tabIB[i] & 0xFF;
            int vIndex = v;
            if (v != 0)
                wt = Integer.toUnsignedLong((pow[(log[vIndex] + 0x68) % 255] << 24) ^ (pow[(log[vIndex] + 0xEE) % 255] << 16) ^
                                            (pow[(log[vIndex] + 0xC7) % 255] << 8) ^ pow[(log[vIndex] + 0xDF) % 255]);
            s_tabIT[0][i] = wt;
            wt = upr((int) wt);
            s_tabIT[1][i] = wt;
            wt = upr((int) wt);
            s_tabIT[2][i] = wt;
            wt = upr((int) wt);
            s_tabIT[3][i] = wt;
        }
    }

    public int bKs(int index) {
        long ks = m_Ks[index / 4];
        switch (index % 4) {
            case 0:
                return (int) (ks & 0xFFL);
            case 1:
                return (int) ((ks >>> 8) & 0xFFL);
            case 2:
                return (int) ((ks >>> 16) & 0xFFL);
            case 3:
                return (int) ((ks >>> 24) & 0xFFL);
        }
        return 0;
    }

    private byte[] encryptBlock(byte[] data) {
        long[] ind = new long[4];
        long[] w = new long[4];
        ByteBuffer dataByteBuffer = ByteBuffer.wrap(data);
        ind[0] = m_Ks[0] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 0);
        ind[1] = m_Ks[1] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 4);
        ind[2] = m_Ks[2] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 8);
        ind[3] = m_Ks[3] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 12);
        w[0] = s_tabFT[0][(int) (ind[0] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[1] >>> 8) & 0xFFL)] ^
               s_tabFT[2][(int) ((ind[2] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[3] >>> 24) & 0xFFL)] ^ m_Ks[4];
        w[1] = s_tabFT[0][(int) (ind[1] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[2] >>> 8) & 0xFFL)] ^
               s_tabFT[2][(int) ((ind[3] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[0] >>> 24) & 0xFFL)] ^ m_Ks[5];
        w[2] = s_tabFT[0][(int) (ind[2] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[3] >>> 8) & 0xFFL)] ^
               s_tabFT[2][(int) ((ind[0] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[1] >>> 24) & 0xFFL)] ^ m_Ks[6];
        w[3] = s_tabFT[0][(int) (ind[3] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[0] >>> 8) & 0xFFL)] ^
               s_tabFT[2][(int) ((ind[1] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[2] >>> 24) & 0xFFL)] ^ m_Ks[7];
        ind = Arrays.copyOf(w, ind.length);
        int i;
        for (i = 0; i < m_nCipherRnd - 2; i += 2) {
            w[0] = s_tabFT[0][(int) (ind[0] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[1] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[2] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[3] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 8];
            w[1] = s_tabFT[0][(int) (ind[1] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[2] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[3] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[0] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 9];
            w[2] = s_tabFT[0][(int) (ind[2] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[3] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[0] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[1] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 10];
            w[3] = s_tabFT[0][(int) (ind[3] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[0] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[1] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[2] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 11];
            ind = Arrays.copyOf(w, ind.length);
            w[0] = s_tabFT[0][(int) (ind[0] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[1] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[2] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[3] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 12];
            w[1] = s_tabFT[0][(int) (ind[1] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[2] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[3] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[0] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 13];
            w[2] = s_tabFT[0][(int) (ind[2] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[3] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[0] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[1] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 14];
            w[3] = s_tabFT[0][(int) (ind[3] & 0xFFL)] ^ s_tabFT[1][(int) ((ind[0] >>> 8) & 0xFFL)] ^
                   s_tabFT[2][(int) ((ind[1] >>> 16) & 0xFFL)] ^ s_tabFT[3][(int) ((ind[2] >>> 24) & 0xFFL)] ^ m_Ks[i * 4 + 15];
            ind = Arrays.copyOf(w, ind.length);
        }
        byte[] crypt = new byte[16];
        byte[] someBytes = CryptTools.getBytesFromInt((int) (
                s_tabSB[(int) (ind[0] & 0xFFL)] ^ (s_tabSB[(int) ((ind[1] >>> 8) & 0xFFL)] << 8) ^
                (s_tabSB[(int) ((ind[2] >>> 16) & 0xFFL)] << 16) ^ (s_tabSB[(int) ((ind[3] >>> 24) & 0xFFL)] << 24) ^
                m_Ks[i * 4 + 8]));
        System.arraycopy(someBytes, 0, crypt, 0, 4);
        someBytes = CryptTools.getBytesFromInt((int) (
                s_tabSB[(int) (ind[1] & 0xFFL)] ^ (s_tabSB[(int) ((ind[2] >>> 8) & 0xFFL)] << 8) ^
                (s_tabSB[(int) ((ind[3] >>> 16) & 0xFFL)] << 16) ^ (s_tabSB[(int) ((ind[0] >>> 24) & 0xFFL)] << 24) ^
                m_Ks[i * 4 + 9]));
        System.arraycopy(someBytes, 0, crypt, 4, 4);
        someBytes = CryptTools.getBytesFromInt((int) (
                s_tabSB[(int) (ind[2] & 0xFFL)] ^ (s_tabSB[(int) ((ind[3] >>> 8) & 0xFFL)] << 8) ^
                (s_tabSB[(int) ((ind[0] >>> 16) & 0xFFL)] << 16) ^ (s_tabSB[(int) ((ind[1] >>> 24) & 0xFFL)] << 24) ^
                m_Ks[i * 4 + 10]));
        System.arraycopy(someBytes, 0, crypt, 8, 4);
        someBytes = CryptTools.getBytesFromInt((int) (
                s_tabSB[(int) (ind[3] & 0xFFL)] ^ (s_tabSB[(int) ((ind[0] >>> 8) & 0xFFL)] << 8) ^
                (s_tabSB[(int) ((ind[1] >>> 16) & 0xFFL)] << 16) ^ (s_tabSB[(int) ((ind[2] >>> 24) & 0xFFL)] << 24) ^
                m_Ks[i * 4 + 11]));
        System.arraycopy(someBytes, 0, crypt, 12, 4);
        return crypt;
    }

    private byte[] decryptBlock(byte[] data) {
        long[] ind = new long[4];
        long[] w = new long[4];
        ByteBuffer dataByteBuffer = ByteBuffer.wrap(data);
        ind[0] = this.m_Ke[0] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 0);
        ind[1] = this.m_Ke[1] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 4);
        ind[2] = this.m_Ke[2] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 8);
        ind[3] = this.m_Ke[3] ^ CryptTools.getReversedIntFromBytes(dataByteBuffer, 12);

        w[0] = s_tabIT[0][(int) (ind[0] & 0xFFL)] ^ s_tabIT[1][(int) ((ind[3] >>> 8) & 0xFFL)]
               ^ s_tabIT[2][(int) ((ind[2] >>> 16) & 0xFFL)] ^ s_tabIT[3][(int) ((ind[1] >>> 24) & 0xFFL)] ^ m_Ke[4];
        w[1] = s_tabIT[0][(int) (ind[1] & 0xFFL)] ^ s_tabIT[1][(int) ((ind[0] >>> 8) & 0xFFL)]
               ^ s_tabIT[2][(int) ((ind[3] >>> 16) & 0xFFL)] ^ s_tabIT[3][(int) ((ind[2] >>> 24) & 0xFFL)] ^ m_Ke[5];
        w[2] = s_tabIT[0][(int) (ind[2] & 0xFFL)] ^ s_tabIT[1][(int) ((ind[1] >>> 8) & 0xFFL)]
               ^ s_tabIT[2][(int) ((ind[0] >>> 16) & 0xFFL)] ^ s_tabIT[3][(int) ((ind[3] >>> 24) & 0xFFL)] ^ m_Ke[6];
        w[3] = s_tabIT[0][(int) (ind[3] & 0xFFL)] ^ s_tabIT[1][(int) ((ind[2] >>> 8) & 0xFFL)]
               ^ s_tabIT[2][(int) ((ind[1] >>> 16) & 0xFFL)] ^ s_tabIT[3][(int) ((ind[0] >>> 24) & 0xFFL)] ^ m_Ke[7];


        ind = Arrays.copyOf(w, ind.length);
        int i;
        for (i = 0; i < this.m_nCipherRnd - 2; i += 2) {
            w[0] = s_tabIT[0][(int) (ind[0] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[3] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[2] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[1] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 8];
            w[1] = s_tabIT[0][(int) (ind[1] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[0] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[3] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[2] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 9];
            w[2] = s_tabIT[0][(int) (ind[2] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[1] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[0] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[3] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 10];
            w[3] = s_tabIT[0][(int) (ind[3] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[2] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[1] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[0] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 11];
            ind = Arrays.copyOf(w, ind.length);

            w[0] = s_tabIT[0][(int) (ind[0] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[3] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[2] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[1] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 12];
            w[1] = s_tabIT[0][(int) (ind[1] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[0] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[3] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[2] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 13];
            w[2] = s_tabIT[0][(int) (ind[2] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[1] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[0] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[3] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 14];
            w[3] = s_tabIT[0][(int) (ind[3] & 0xFFL)]
                   ^ s_tabIT[1][(int) ((ind[2] >>> 8) & 0xFFL)]
                   ^ s_tabIT[2][(int) ((ind[1] >>> 16) & 0xFFL)]
                   ^ s_tabIT[3][(int) ((ind[0] >>> 24) & 0xFFL)]
                   ^ m_Ke[i * 4 + 15];
            ind = Arrays.copyOf(w, ind.length);
        }
        byte[] crypt = new byte[16];

        byte[] someBytes = CryptTools.getBytesFromInt((int) (s_tabIB[(int) (ind[0] & 0xFFL)]
                                                             ^ (s_tabIB[(int) ((ind[3] >>> 8) & 0xFFL)] << 8)
                                                             ^ (s_tabIB[(int) ((ind[2] >>> 16) & 0xFFL)] << 16)
                                                             ^ (s_tabIB[(int) ((ind[1] >>> 24) & 0xFFL)] << 24)
                                                             ^ m_Ke[i * 4 + 8]));
        System.arraycopy(someBytes, 0, crypt, 0, 4);
        someBytes = CryptTools.getBytesFromInt((int) (s_tabIB[(int) (ind[1] & 0xFFL)]
                                                      ^ (s_tabIB[(int) ((ind[0] >>> 8) & 0xFFL)] << 8)
                                                      ^ (s_tabIB[(int) ((ind[3] >>> 16) & 0xFFL)] << 16)
                                                      ^ (s_tabIB[(int) ((ind[2] >>> 24) & 0xFFL)] << 24)
                                                      ^ m_Ke[i * 4 + 9]));
        System.arraycopy(someBytes, 0, crypt, 4, 4);
        someBytes = CryptTools.getBytesFromInt((int) (s_tabIB[(int) (ind[2] & 0xFFL)]
                                                      ^ (s_tabIB[(int) ((ind[1] >>> 8) & 0xFFL)] << 8)
                                                      ^ (s_tabIB[(int) ((ind[0] >>> 16) & 0xFFL)] << 16)
                                                      ^ (s_tabIB[(int) ((ind[3] >>> 24) & 0xFFL)] << 24)
                                                      ^ m_Ke[i * 4 + 10]));
        System.arraycopy(someBytes, 0, crypt, 8, 4);
        someBytes = CryptTools.getBytesFromInt((int) (s_tabIB[(int) (ind[3] & 0xFFL)]
                                                      ^ (s_tabIB[(int) ((ind[2] >>> 8) & 0xFFL)] << 8)
                                                      ^ (s_tabIB[(int) ((ind[1] >>> 16) & 0xFFL)] << 16)
                                                      ^ (s_tabIB[(int) ((ind[0] >>> 24) & 0xFFL)] << 24)
                                                      ^ m_Ke[i * 4 + 11]));
        System.arraycopy(someBytes, 0, crypt, 12, 4);
        return crypt;
    }

    public byte[] encryptData(byte[] data, byte[] key) {
        encodeKey(key, key.length * 8);
        byte[] crypt = new byte[(data.length + 15) & ~15];
        byte[] block = new byte[16];
        int ib = 0;
        for (int i = 0; i < data.length / 16; i++, ib += 16) {
            for (int k = 0; k < 16; k++)
                block[k] ^= data[ib + k];
            block = encryptBlock(block);
            System.arraycopy(block, 0, crypt, ib, 16);
        }
        if ((data.length & 0xF) != 0) {
            for (int i = 0; i < (data.length & 0xF); i++)
                block[i] ^= data[ib + i];
            block = encryptBlock(block);
            System.arraycopy(block, 0, crypt, ib, 16);
        }
        return crypt;
    }
    /*

     */

    public byte[] decryptData(byte[] data, byte[] key) {
        encodeKey(key, key.length * 8);

        byte[] numArray1 = new byte[data.length];
        byte[] numArray2 = new byte[16];
        byte[] numArray3 = new byte[16];
        byte[] data1 = new byte[16];
        byte[] data2 = new byte[16];
        byte[] numArray4 = new byte[16];
        int num1 = 0;
        int num2 = data.length / 16;
        while (num2 > 0) {
            if ((num2 & 1) == 0) {
                System.arraycopy(data, num1, numArray2, 0, 16);
                System.arraycopy(data, num1, data1, 0, 16);
                data1 = decryptBlock(data1);
                for (int index = 0; index < 16; ++index)
                    numArray4[index] = (byte) ((long) numArray3[index] ^ (long) data1[index]);
            } else {
                System.arraycopy(data, num1, numArray3, 0, 16);
                System.arraycopy(data, num1, data2, 0, 16);
                data2 = decryptBlock(data2);
                for (int index = 0; index < 16; ++index)
                    numArray4[index] = (byte) ((long) numArray2[index] ^ (long) data2[index]);
            }
            System.arraycopy(numArray4, 0, numArray1, num1, 16);
            --num2;
            num1 += 16;
        }
        return numArray1;
    }
}
