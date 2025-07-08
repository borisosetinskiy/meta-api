package com.ob.api.mtx.mt4.connector.connection.codec;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class vSHA1Java {
    private long[] Regs = new long[5];
    private int nBitCount;
    private long dwData;
    private byte[] dwBlock = new byte[64];
    private int dwCount;
    private int dbCount;

    public vSHA1Java() {
        nBitCount = 0;
        dwData = 0;
        dwCount = 0;
        dbCount = 0;
        Regs[0] = 0x67452301L;
        Regs[1] = 0xEFCDAB89L;
        Regs[2] = 0x98BADCFEL;
        Regs[3] = 0x10325476L;
        Regs[4] = 0xC3D2E1F0L;
        Arrays.fill(dwBlock, 0, 16, (byte) 0);
    }

    public void hashData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            dwData = (dwData << 8) + Byte.toUnsignedInt(data[i]);
            nBitCount += 8;
            if (++dbCount >= 4) {
                dbCount = 0;
                byte[] dwDataByteArr = CryptTools.getBytesFromInt((int) dwData);
                System.arraycopy(dwDataByteArr, 0, dwBlock, dwCount * 4, 4);
                if (++dwCount >= 16) {
                    dwCount = 0;
                    transform(dwBlock);
                }
                dwData = 0;
            }
        }
    }

    public byte[] finalizeHash() {
        int bitCnt = nBitCount;
        dwData = (dwData << 8) + 0x80;
        while (true) {
            nBitCount += 8;
            if (++dbCount >= 4) {
                dbCount = 0;
                byte[] dwDataByteArr = CryptTools.getBytesFromInt((int) dwData);
                System.arraycopy(dwDataByteArr, 0, dwBlock, dwCount * 4, 4);
                if (++dwCount >= 16) {
                    dwCount = 0;
                    transform(dwBlock);
                }
                dwData = 0;
            }
            if ((dbCount == 0) && (dwCount == 14))
                break;
            dwData <<= 8;
        }
        byte[] zeroByteArr = CryptTools.getBytesFromInt(0);
        System.arraycopy(zeroByteArr, 0, dwBlock, dwCount * 4, 4);
        if (++dwCount >= 16) {
            dwCount = 0;
            transform(dwBlock);
        }
        byte[] bitCntArr = CryptTools.getBytesFromInt(bitCnt);
        System.arraycopy(bitCntArr, 0, dwBlock, dwCount * 4, 4);
        if (++dwCount >= 16) {
            dwCount = 0;
            transform(dwBlock);
        }
        return new byte[]
                {
                        (byte) (Regs[0] >>> 24),
                        (byte) (Regs[0] >>> 16),
                        (byte) (Regs[0] >>> 8),
                        (byte) (Regs[0] >>> 0),
                        (byte) (Regs[1] >>> 24),
                        (byte) (Regs[1] >>> 16),
                        (byte) (Regs[1] >>> 8),
                        (byte) (Regs[1] >>> 0),
                        (byte) (Regs[2] >>> 24),
                        (byte) (Regs[2] >>> 16),
                        (byte) (Regs[2] >>> 8),
                        (byte) (Regs[2] >>> 0),
                        (byte) (Regs[3] >>> 24),
                        (byte) (Regs[3] >>> 16),
                        (byte) (Regs[3] >>> 8),
                        (byte) (Regs[3] >>> 0),
                        (byte) (Regs[4] >>> 24),
                        (byte) (Regs[4] >>> 16),
                        (byte) (Regs[4] >>> 8),
                        (byte) (Regs[4] >>> 0),
                };
    }

    public byte[] computeHash(byte[] data) {
        int len = data.length;
        int left = 0;
        if (len >= 64) {
            byte[] block = new byte[64];
            for (int i = 0; i < len / 64; i++) {
                System.arraycopy(data, i * 64, block, 0, 64);
                transform(block);
                left += 64;
            }
        }
        int rem = len % 64;
        if (rem > 0) {
            byte[] block = new byte[64];
            System.arraycopy(data, left, block, 0, rem);
            transform(block);
        }
        return new byte[]
                {
                        (byte) (Regs[0] >>> 0),
                        (byte) (Regs[0] >>> 8),
                        (byte) (Regs[0] >>> 16),
                        (byte) (Regs[0] >>> 24),
                        (byte) (Regs[1] >>> 0),
                        (byte) (Regs[1] >>> 8),
                        (byte) (Regs[1] >>> 16),
                        (byte) (Regs[1] >>> 24),
                        (byte) (Regs[2] >>> 0),
                        (byte) (Regs[2] >>> 8),
                        (byte) (Regs[2] >>> 16),
                        (byte) (Regs[2] >>> 24),
                        (byte) (Regs[3] >>> 0),
                        (byte) (Regs[3] >>> 8),
                        (byte) (Regs[3] >>> 16),
                        (byte) (Regs[3] >>> 24),
                        (byte) (Regs[4] >>> 0),
                        (byte) (Regs[4] >>> 8),
                        (byte) (Regs[4] >>> 16),
                        (byte) (Regs[4] >>> 24),
                };
    }

   /* public long sha1Shift(int bits, long word)
    {
        return ((word << bits) | (word >>> (32 - bits))) & 0xFFFFFFFFL;
    }*/

    public long sha1Shift(int bits, int word) {
        return Integer.toUnsignedLong((word << bits) | (word >>> (32 - bits)));
    }

    private void transform(byte[] data) {
        long temp;
        long[] W = new long[80];
        ByteBuffer wrappedData = ByteBuffer.wrap(data);
        for (int i = 0; i < 16; i++)
            W[i] = CryptTools.getReversedIntFromBytes(wrappedData, i * 4);
        for (int i = 16; i < 80; i++)
            W[i] = sha1Shift(1, (int) (W[i - 3] ^ W[i - 8] ^ W[i - 14] ^ W[i - 16]));
        long A = Regs[0];
        long B = Regs[1];
        long C = Regs[2];
        long D = Regs[3];
        long E = Regs[4];
        for (int i = 0; i < 20; i++) {
            temp = sha1Shift(5, (int) A) + ((B & C) | (~B & D)) + E + W[i] + 0x5A827999L;
            temp = temp & 0xFFFFFFFFL;
            E = D;
            D = C;
            C = sha1Shift(30, (int) B);
            B = A;
            A = temp;
        }
        for (int i = 20; i < 40; i++) {
            temp = sha1Shift(5, (int) A) + (B ^ C ^ D) + E + W[i] + 0x6ED9EBA1L;
            temp = temp & 0xFFFFFFFFL;
            E = D;
            D = C;
            C = sha1Shift(30, (int) B);
            B = A;
            A = temp;
        }
        for (int i = 40; i < 60; i++) {
            temp = sha1Shift(5, (int) A) + ((B & C) | (B & D) | (C & D)) + E + W[i] + 0x8F1BBCDCL;
            temp = temp & 0xFFFFFFFFL;
            E = D;
            D = C;
            C = sha1Shift(30, (int) B);
            B = A;
            A = temp;
        }
        for (int i = 60; i < 80; i++) {
            temp = sha1Shift(5, (int) A) + (B ^ C ^ D) + E + W[i] + 0xCA62C1D6L;
            temp = temp & 0xFFFFFFFFL;
            E = D;
            D = C;
            C = sha1Shift(30, (int) B);
            B = A;
            A = temp;
        }
        Regs[0] += A;
        Regs[0] = Regs[0] & 0xFFFFFFFFL;

        Regs[1] += B;
        Regs[1] = Regs[1] & 0xFFFFFFFFL;

        Regs[2] += C;
        Regs[2] = Regs[2] & 0xFFFFFFFFL;

        Regs[3] += D;
        Regs[3] = Regs[3] & 0xFFFFFFFFL;

        Regs[4] += E;
        Regs[4] = Regs[4] & 0xFFFFFFFFL;
    }

}
