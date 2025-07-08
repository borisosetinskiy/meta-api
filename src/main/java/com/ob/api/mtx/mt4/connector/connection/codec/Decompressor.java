package com.ob.api.mtx.mt4.connector.connection.codec;


import com.ob.api.mtx.mt4.connector.error.DecompressorException;
import com.ob.api.mtx.mt4.connector.util.ByteUtil;

import static com.ob.api.mtx.mt4.connector.util.ArrayUtil.arrByte;

public class Decompressor {

    public static byte[] decompress(byte[] src, int len) throws DecompressorException {

        byte[] dst = arrByte(len + 0x800);
        decompressData(src, src.length, dst, len);

        byte[] res = new byte[len];
        System.arraycopy(dst, 0, res, 0, len);

        return res;
    }

    private static void decompressData(byte[] src, int srcLen, byte[] dst, int dstLen) throws DecompressorException {
        if ((src == null) || (srcLen < 3) || (dst == null) || (dstLen < 1))
            throw new DecompressorException();
        int srcIndex = 0;
        int dstIndex = 0;

        int toIndex = dstIndex;
        if ((src[srcLen - 1] != 0) || (src[srcLen - 2] != 0) || (src[srcLen - 3] != 0x11))
            throw new DecompressorException();

        int pIndex;

        int ch = Byte.toUnsignedInt(src[srcIndex]);

        int goThere = 0;
        for (; ; ) {
            switch (goThere) {
                case 0:
                    if (ch > 0x11) {
                        ch -= 0x11;
                        srcIndex++;
                        if (ch < 4) {
                            goThere = 37;
                            continue;
                        }
                        if ((dstLen - dstIndex < ch) || (srcLen - srcIndex < ch + 1)) {
                            throw new DecompressorException();
                        }
                        while (ch-- > 0)
                            dst[dstIndex++] = src[srcIndex++];
                        goThere = 31;
                        continue;
                    }
                    goThere = 15;
                    continue;
                case 15:
                    ch = Byte.toUnsignedInt(src[srcIndex++]);
                    if (ch >= 0x10) {
                        goThere = 41;
                        continue;
                    }
                    if (ch == 0) {
                        if (srcIndex > srcLen)
                            throw new DecompressorException();
                        while (src[srcIndex] == 0) {
                            ch += 0xFF;
                            if (++srcIndex > srcLen) {
                                throw new DecompressorException();
                            }
                        }
                        ch += Byte.toUnsignedInt(src[srcIndex++]) + 0xF;
                    }
                    if ((dstLen - dstIndex < ch + 3) || (srcLen - srcIndex < ch + 4))
                        throw new DecompressorException();
                    // TODO test how bytes should be set
                    dst[dstIndex++] = src[srcIndex++];
                    dst[dstIndex++] = src[srcIndex++];
                    dst[dstIndex++] = src[srcIndex++];
                    dst[dstIndex++] = src[srcIndex++];
                    if (--ch != 0) {
                        while (ch >= 4) {
                            dst[dstIndex++] = src[srcIndex++];
                            dst[dstIndex++] = src[srcIndex++];
                            dst[dstIndex++] = src[srcIndex++];
                            dst[dstIndex++] = src[srcIndex++];
                            ch -= 4;
                        }
                    }
                    while (ch-- > 0)
                        dst[dstIndex++] = src[srcIndex++];
                    goThere = 31;
                    continue;
                case 31:
                    ch = Byte.toUnsignedInt(src[srcIndex++]);
                    if (ch >= 0x10) {
                        goThere = 41;
                        continue;
                    }
                    pIndex = dstIndex - (Byte.toUnsignedInt(src[srcIndex++]) * 4) - (ch / 4) - 0x801;
                    if ((pIndex < toIndex) || (dstLen - dstIndex < 3))
                        throw new DecompressorException();
                    dst[dstIndex++] = dst[pIndex++];
                    dst[dstIndex++] = dst[pIndex++];
                    dst[dstIndex++] = dst[pIndex++];
                    goThere = 36;
                    continue;
                case 36:
                    ch = Byte.toUnsignedInt(src[srcIndex - 2]) & 3;
                    if (ch == 0) {
                        goThere = 15;
                        continue;
                    }
                    goThere = 37;
                    continue;
                case 37:
                    if ((dstLen - dstIndex < ch) || (srcLen - srcIndex < ch + 1))
                        throw new DecompressorException();
                    while (ch-- > 0)
                        dst[dstIndex++] = src[srcIndex++];
                    ch = Byte.toUnsignedInt(src[srcIndex++]);
                    goThere = 41;
                    continue;
                case 41:
                    if (ch >= 0x40) {
                        pIndex = dstIndex - (Byte.toUnsignedInt(src[srcIndex++]) * 8) - ((ch / 4) & 7) - 1;
                        ch = ch / 32 - 1;
                        if ((pIndex < toIndex) || (dstLen - dstIndex < ch + 2))
                            throw new DecompressorException();
                        dst[dstIndex++] = dst[pIndex++];
                        do dst[dstIndex++] = dst[pIndex++];
                        while (ch-- > 0);
                        goThere = 36;
                        continue;
                    }
                    if (ch >= 0x20) {
                        ch &= 0x1F;
                        if (ch == 0) {
                            if (srcIndex > srcLen)
                                throw new DecompressorException();
                            while (src[srcIndex] == 0) {
                                ch += 0xFF;
                                if (++srcIndex > srcLen)
                                    throw new DecompressorException();
                            }
                            ch += Byte.toUnsignedInt(src[srcIndex++]) + 0x1F;
                        }
                        int shortFromSrc = Short.toUnsignedInt(ByteUtil.getShort(src, srcIndex));
                        pIndex = dstIndex - (shortFromSrc / 4) - 1;
                        srcIndex += 2;
                        if ((pIndex < toIndex) || (dstLen - dstIndex < ch + 2))
                            throw new DecompressorException();
                        if ((ch >= 6) && (dstIndex - pIndex >= 4)) {
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                            ch -= 2;
                            while (ch >= 4) {
                                dst[dstIndex++] = dst[pIndex++];
                                dst[dstIndex++] = dst[pIndex++];
                                dst[dstIndex++] = dst[pIndex++];
                                dst[dstIndex++] = dst[pIndex++];
                                ch -= 4;
                            }
                        } else {
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                        }
                        while (ch-- > 0)
                            dst[dstIndex++] = dst[pIndex++];
                        goThere = 36;
                        continue;
                    }
                    if (ch >= 0x10) {
                        pIndex = dstIndex - ((ch & 8) << 11);
                        ch &= 7;
                        if (ch == 0) {
                            if (srcIndex > srcLen)
                                throw new DecompressorException();
                            while (src[srcIndex] == 0) {
                                ch += 0xFF;
                                if (++srcIndex > srcLen)
                                    throw new DecompressorException();
                            }
                            ch += Byte.toUnsignedInt(src[srcIndex++]) + 7;
                        }
                        int tempShort = Short.toUnsignedInt(ByteUtil.getShort(src, srcIndex));
                        pIndex -= (tempShort / 4);
                        srcIndex += 2;
                        if (pIndex == dstIndex) {
                            dst[dstIndex] = (byte) (dstIndex - toIndex);
                            if (srcIndex != srcLen)
                                throw new DecompressorException();
                            return;
                        }
                        pIndex -= 0x4000;
                        if ((pIndex < toIndex) || (dstLen - dstIndex < ch + 2))
                            throw new DecompressorException();
                        if ((ch >= 6) && (dstIndex - pIndex >= 4)) {
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                            ch -= 2;
                            while (ch >= 4) {
                                dst[dstIndex++] = dst[pIndex++];
                                dst[dstIndex++] = dst[pIndex++];
                                dst[dstIndex++] = dst[pIndex++];
                                dst[dstIndex++] = dst[pIndex++];
                                ch -= 4;
                            }
                        } else {
                            dst[dstIndex++] = dst[pIndex++];
                            dst[dstIndex++] = dst[pIndex++];
                        }
                        while (ch-- > 0)
                            dst[dstIndex++] = dst[pIndex++];
                        goThere = 36;
                        continue;
                    }
                    pIndex = dstIndex - (src[srcIndex++] * 4) - (ch / 4) - 1;
                    dst[dstIndex++] = dst[pIndex++];
                    dst[dstIndex++] = dst[pIndex++];
                    if ((pIndex >= toIndex) && (dstLen - dstIndex >= 2)) {
                        goThere = 36;
                        continue;
                    }
                    throw new DecompressorException();
            }

        }
    }

}
