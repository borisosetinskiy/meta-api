package com.ob.api.mtx.mt4.connector.util;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;

public class ByteArrayUtil {
    public static void copyIntToByteArray(int someInt, byte[] dest, int offset) {
        byte[] dwDataByteArr = toReversedByteArray(someInt);
        System.arraycopy(dwDataByteArr, 0, dest, offset, 4);
    }

    public static byte[] intToByteArray(int someInt) {
        byte[] dwDataByteArr = toReversedByteArray(someInt);
        return dwDataByteArr;
    }

    public static byte[] shortToByteArray(short someShort) {
        byte[] dwDataByteArr = toReversedByteArray(someShort);
        return dwDataByteArr;
    }

    public static void copyShortToByteArray(short someShort, byte[] dest, int offset) {
        byte[] dwDataByteArr = toReversedByteArray(someShort);
        System.arraycopy(dwDataByteArr, 0, dest, offset, 2);
    }

    public static void copyDoubleToByteArray(double someDouble, byte[] dest, int offset) {
        byte[] dwDataByteArr = ByteBuffer.allocate(8).putDouble(someDouble).array();
        ArrayUtils.reverse(dwDataByteArr);
        System.arraycopy(dwDataByteArr, 0, dest, offset, 8);
    }

    public static void copyLongToByteArray(long someLong, byte[] dest, int offset) {
        byte[] dwDataByteArr = ByteBuffer.allocate(8).putLong(someLong).array();
        ArrayUtils.reverse(dwDataByteArr);
        System.arraycopy(dwDataByteArr, 0, dest, offset, 8);
    }

    public static byte[] toByteArray(int value) {
        return new byte[]{(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }

    public static byte[] toByteArray(short value) {
        return new byte[]{(byte) (value >> 8), (byte) value};
    }

    public static byte[] toReversedByteArray(int value) {
        return new byte[]{(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)};
    }

    public static byte[] toReversedByteArray(short value) {
        return new byte[]{(byte) value, (byte) (value >> 8)};
    }
}
