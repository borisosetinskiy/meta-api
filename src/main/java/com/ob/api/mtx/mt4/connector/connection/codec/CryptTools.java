package com.ob.api.mtx.mt4.connector.connection.codec;


import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;

public class CryptTools {
    public static long getReversedIntFromBytes(ByteBuffer byteBuffer, int index) {
        byte[] intByteArr = new byte[4];
        byteBuffer.position(index);
        byteBuffer.get(intByteArr, 0, 4);
        ArrayUtils.reverse(intByteArr);
        return Integer.toUnsignedLong(ByteBuffer.wrap(intByteArr).getInt());
    }

    public static byte[] getBytesFromInt(int someInt) {
        byte[] dwDataByteArr = ByteBuffer.allocate(4).putInt(someInt).array();
        ArrayUtils.reverse(dwDataByteArr);
        return dwDataByteArr;
    }
}
