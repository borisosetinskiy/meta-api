package com.ob.api.mtx.mt4.connector.util;

public class Convert {
    public static long ToUInt64(boolean b) {
        return b ? 1 : 0;
    }

    public static boolean ToBoolean(long l) {
        return l != 0;
    }
}