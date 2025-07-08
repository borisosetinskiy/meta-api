package com.ob.api.mtx.mt4.connector.util;

import com.ob.api.mtx.mt4.connector.connection.Connection;
import com.ob.api.mtx.mt4.connector.entity.StatusCode;
import com.ob.api.mtx.mt4.connector.entity.dto.Mt4Order;
import com.ob.api.mtx.mt4.connector.parser.OrderBufParser;
import com.ob.api.mtx.mt4.connector.parser.TimeComponent;
import com.ob.broker.common.error.CodeException;

import java.util.function.Consumer;

public final class OrderHistoryUtil {
    static final int SIZE_OF_HISTORY = 50;

    public static void read(Connection con, int from, int to, Consumer<Mt4Order> consumer, TimeComponent timeComponent) throws Exception {
        if (Thread.currentThread().isInterrupted())
            throw new Exception();
        byte[] buf = new byte[9];
        buf[0] = 0x22;                              //request order history
        ByteArrayUtil.copyIntToByteArray(from, buf, 1);
        ByteArrayUtil.copyIntToByteArray(to, buf, 5);
        con.send(buf);
        buf = con.receiveDecode(1);
        if (buf[0] == 0) {
            buf = con.readCompressed();
        } else if (buf[0] == 1) {
            return;
        } else {
            var statusCode = StatusCode.getById(buf[0]);
            throw new CodeException(StatusCode.codeByNumber(statusCode.getId()));
        }
        parse(buf, SIZE_OF_HISTORY, consumer, timeComponent);
    }


    public static void parse(byte[] buf, int maxSize, Consumer<Mt4Order> consumer, TimeComponent timeComponent) throws InterruptedException {
        int i = 0;
        int count = 0;
        while (i < buf.length && count++ <= maxSize) {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            Mt4Order mt4Order = OrderBufParser.parse(buf, i, timeComponent);
            if (mt4Order == null)
                continue;
            try {
                consumer.accept(mt4Order);
            } catch (Exception ignored) {
            }
            i += 14 * 16;
        }
    }
}
