package com.ob.api.mtx.mt4.connector.util;

import com.ob.api.mtx.mt4.connector.error.ArraySizeError;

public final class ArrayUtil {
    public static int[] arrInt(int len) {
        return new int[size(len)];
    }

    public static byte[] arrByte(int len) {
        return new byte[size(len)];
    }

    public static double[] arrDouble(int len) {
        return new double[size(len)];
    }

    public static int size(int len) {
        if (len <= 0 || len > 5_000_000)
            throw new ArraySizeError(len);
        return len;
    }
}
