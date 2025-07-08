package com.ob.api.mtx.mt4.connector.util;

public class ByteUtil {

    private static final int SHORT_BYTE_SIZE = 2;
    private static final int INT_BYTE_SIZE = 4;
    private static final int FLOAT_BYTE_SIZE = 4;
    private static final int LONG_BYTE_SIZE = 8;
    private static final int DOUBLE_BYTE_SIZE = 8;

    private static void swapByIndexes(byte[] arr, int i, int j) {
        byte tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    public static int getInt(byte[] byteArr, int offset) {
        byte[] intByteArr = new byte[INT_BYTE_SIZE];
        System.arraycopy(byteArr, offset, intByteArr, 0, INT_BYTE_SIZE);
        swapByIndexes(intByteArr, 0, 3);
        swapByIndexes(intByteArr, 1, 2);

        return ((intByteArr[0] << 24) |
                ((intByteArr[1] & 0xff) << 16) |
                ((intByteArr[2] & 0xff) << 8) |
                ((intByteArr[3] & 0xff)));
    }

    public static short getShort(byte[] byteArr, int offset) {
        byte[] shortByteArr = new byte[SHORT_BYTE_SIZE];
        System.arraycopy(byteArr, offset, shortByteArr, 0, SHORT_BYTE_SIZE);
        swapByIndexes(shortByteArr, 0, 1);
        return (short) (shortByteArr[0] << 8 | shortByteArr[1] & 0xFF);
    }

    public static byte[] getNotReversedBytes(byte[] byteArrOld, int offset, int size) {
        byte[] byteArr = new byte[size];
        System.arraycopy(byteArrOld, offset, byteArr, 0, size);
        return byteArr;
    }

    public static double getDouble(byte[] byteArr, int offset) {
        byte[] doubleByteArr = new byte[DOUBLE_BYTE_SIZE];
        System.arraycopy(byteArr, offset, doubleByteArr, 0, DOUBLE_BYTE_SIZE);
        swapByIndexes(doubleByteArr, 0, 7);
        swapByIndexes(doubleByteArr, 1, 6);
        swapByIndexes(doubleByteArr, 2, 5);
        swapByIndexes(doubleByteArr, 3, 4);

        long longBits = ((((long) doubleByteArr[0]) << 56) |
                         (((long) doubleByteArr[1] & 0xff) << 48) |
                         (((long) doubleByteArr[2] & 0xff) << 40) |
                         (((long) doubleByteArr[3] & 0xff) << 32) |
                         (((long) doubleByteArr[4] & 0xff) << 24) |
                         (((long) doubleByteArr[5] & 0xff) << 16) |
                         (((long) doubleByteArr[6] & 0xff) << 8) |
                         (((long) doubleByteArr[7] & 0xff)));
        return Double.longBitsToDouble(longBits);
    }

    public static float getFloat(byte[] byteArr, int offset) {
        return Float.intBitsToFloat(getInt(byteArr, offset));
    }

    public static byte[] toByte(String str) {
        byte[] res = new byte[str.length()];

        for (int i = 0; i < str.length(); i++)
            res[i] = (byte) (str.charAt(i) & 0xFF);

        return res;
    }

    public static String byteToString(byte[] bytes, int offset) {
        StringBuilder res = new StringBuilder();
        for (int i = offset; i < bytes.length; i++) {
            if (bytes[i] == 0)
                break;
            int value = bytes[i];
            if (value < 0)
                value += 256;
            if (value == 0)
                break;
            res.append((char) value);
        }
        return res.toString();
    }

    public static String byteAllToString(byte[] bytes, int offset) {
        StringBuilder res = new StringBuilder();
        for (int i = offset; i < bytes.length; i++) {

            int value = bytes[i];
            if (value < 0)
                value += 256;
            res.append((char) value);
        }
        return res.toString();
    }


//
//    private static int[] signedByteToUnsignedByte(byte[] bytes) {
//        int[] unsignedBytes = new int[bytes.length];
//
//        for (int i = 0; i < bytes.length; i++) {
//            int unsignedByte = (int) bytes[i];
//            if (unsignedByte < 0) {
//                unsignedByte = 256 + unsignedByte;
//            }
//            unsignedBytes[i] = unsignedByte;
//        }
//
//        return unsignedBytes;
//    }

}
