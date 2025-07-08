package com.ob.api.mtx.mt4.connector.connection.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MT4Crypt {
    public static byte[] CryptKey = new byte[]
            {
                    0x41, (byte) 0xB6, 0x7F, 0x58, 0x38, 0x0C, (byte) 0xF0, 0x2D,
                    0x7B, 0x39, 0x08, (byte) 0xFE, 0x21, (byte) 0xBB, 0x41, 0x58
            };


    public static byte[] crypt(byte[] buf) {
        int Last = 0;
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            res[i] = (byte) (buf[i] ^ (Last + CryptKey[i & 0xF]));
            Last = res[i];
        }
        return res;
    }

    public static byte[] decrypt(byte[] buf) {
        int Last = 0;
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            res[i] = (byte) (buf[i] ^ (Last + CryptKey[i & 0xF]));
            Last = buf[i];
        }
        return res;
    }

    public static byte[] encode(byte[] buf, byte[] key) {
        int Last = 0;
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            res[i] = (byte) (buf[i] ^ (Last + key[i % key.length]));
            Last = res[i];
        }
        return res;
    }

    public static byte[] Decrypt(byte[] buf, byte[] key) {
        int Last = 0;
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            res[i] = (byte) (buf[i] ^ (Last + key[i & 0xF]));
            Last = buf[i];
        }
        return res;
    }

    public static byte[] Encrypt(byte[] buf, byte[] key) {
        int Last = 0;
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            res[i] = (byte) (buf[i] ^ (Last + key[i & 0xF]));
            Last = res[i];
        }
        return res;
    }

    public static byte[] decode(byte[] buf, byte[] key) {
        int Last = 0;
        byte[] res = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            res[i] = (byte) (buf[i] ^ (Last + key[i % key.length]));
            Last = buf[i];
        }
        return res;
    }

    public static byte[] createHardId(int ticks) throws NoSuchAlgorithmException {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            ticks = ticks * 214013 + 2531011;
            data[i] = (byte) ((ticks >>> 16) & 0xFF);
        }
        MessageDigest m = MessageDigest.getInstance("MD5");
        byte[] hardId = m.digest(data);
        int temp = 0;
        for (int i = 1; i < 16; i++)
            temp += Byte.toUnsignedInt(hardId[i]);

        hardId[0] = (byte) temp;
        return hardId;
    }


}
