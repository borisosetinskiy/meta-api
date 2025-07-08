package com.ob.api.mtx.mt4.connector.util;


public interface OrderSerializer {
    static byte[] get(int user, int ticket, int cmd, int operation,
                      String symbol, int lots, double price, double sl, double tp, int slip,
                      String comment, int magic, int expiration) {
        byte[] buf = new byte[97];
        buf[0] = (byte) 0xBE;                                      //order transaction request
        buf[1] = (byte) cmd;
//        buf[2] = (byte) 1;
        buf[3] = (byte) operation;
        ByteArrayUtil.copyIntToByteArray(ticket, buf, 5);
        ByteArrayUtil.copyIntToByteArray(magic, buf, 9);
        byte[] symbolArray = ByteUtil.toByte(symbol);
        System.arraycopy(symbolArray, 0, buf, 13, symbol.length());
        ByteArrayUtil.copyIntToByteArray(lots, buf, 25);
        ByteArrayUtil.copyDoubleToByteArray(price, buf, 29);
        ByteArrayUtil.copyDoubleToByteArray(sl, buf, 37);
        ByteArrayUtil.copyDoubleToByteArray(tp, buf, 45);
        ByteArrayUtil.copyIntToByteArray(slip, buf, 53);
        if (comment != null) {
            if (comment.length() > 31)
                comment = comment.substring(0, 31);
            byte[] commentArr = ByteUtil.toByte(comment);
            System.arraycopy(commentArr, 0, buf, 57, comment.length());
        }
        ByteArrayUtil.copyIntToByteArray(expiration, buf, 89);
        long sum = Integer.toUnsignedLong(user);
        for (int i = 1; i < 93; i++) {
            sum += Byte.toUnsignedInt(buf[i]);
            sum &= 0xFFFFFFFFL;
        }
        ByteArrayUtil.copyIntToByteArray((int) sum, buf, 93);
        return buf;
    }
}
