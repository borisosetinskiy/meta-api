package com.ob.api.mtx.mt4.connector.parser;

import com.ob.api.mtx.mt4.connector.entity.OrderUpdateEvent;
import com.ob.api.mtx.mt4.connector.entity.dto.Mt4Order;
import com.ob.api.mtx.mt4.connector.entity.trading.OrderType;
import com.ob.api.mtx.mt4.connector.entity.trading.TradingEvent;
import com.ob.api.mtx.mt4.connector.util.ByteUtil;
import com.ob.api.mtx.mt4.connector.util.NumberUtil;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderBufParser {
    private static final int OFFSET_SIZE_WITH_TICKET = 4;
    private static final int OFFSET_SIZE_WITH_LOGIN = 8;
    private static final int OFFSET_SIZE_WITH_SYMBOL = 20;
    private static final int OFFSET_SIZE_WITH_DIGITS = 24;
    private static final int OFFSET_SIZE_WITH_COMMAND = 28;
    private static final int OFFSET_SIZE_WITH_LOTS = 32;
    private static final int OFFSET_SIZE_WITH_OPEN_TIME = 36;
    private static final int OFFSET_SIZE_WITH_STATE = 40;
    private static final int OFFSET_SIZE_WITH_OPEN_PRICE = 48;
    private static final int OFFSET_SIZE_WITH_STOP_LOSS = 56;
    private static final int OFFSET_SIZE_WITH_TAKE_PROFIT_PRICE = 64;
    private static final int OFFSET_SIZE_WITH_CLOSE_TIME = 68;
    private static final int OFFSET_SIZE_WITH_VALUE_DATE = 72;
    private static final int OFFSET_SIZE_WITH_EXPIRATION = 76;
    private static final int OFFSET_SIZE_WITH_CONC_RESERC = 80;
    private static final int OFFSET_SIZE_WITH_OPEN_RATE = 88;
    private static final int OFFSET_SIZE_WITH_CLOSE_RATE = 96;
    private static final int OFFSET_SIZE_WITH_COMMISSION = 104;
    private static final int OFFSET_SIZE_WITH_COMMISSION_AGENT = 112;
    private static final int OFFSET_SIZE_WITH_STORAGE = 120;
    private static final int OFFSET_SIZE_WITH_CLOSE_PRICE = 128;
    private static final int OFFSET_SIZE_WITH_PROFIT = 136;
    private static final int OFFSET_SIZE_WITH_TAXES = 144;
    private static final int OFFSET_SIZE_WITH_MAGIC = 148;
    private static final int OFFSET_SIZE_WITH_COMMENT = 180;
    private static final int OFFSET_SIZE_WITH_INTERNAL_ID = 184;
    private static final int OFFSET_SIZE_WITH_ACTIVATION = 188;
    private static final int OFFSET_SIZE_WITH_SPREAD = 192;
    private static final int OFFSET_SIZE_WITH_MARGIN_RATE = 200;
    private static final int OFFSET_SIZE_WITH_TIMESTAMP = 204;
    private static final int OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES = 220;
    private static final int OFFSET_SIZE_WITH_NEXT = 224;

    private static final int STRING_COMMENT_SIZE = 31;

    private static final int SYMBOL_SIZE = 12;


    //
    public static Mt4Order parse(byte[] orderArray, int offset, TimeComponent timeComponent) {

        final Mt4Order mt4Order = new Mt4Order();

        int ticket = ByteUtil.getInt(orderArray, offset);

        mt4Order.setTicket(ticket);

        int login = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_TICKET + offset);
//        internalOffset = OFFSET_SIZE_WITH_LOGIN;
        mt4Order.setLogin(login);

        byte[] symbolNotReversed = ByteUtil.getNotReversedBytes(orderArray
                , OFFSET_SIZE_WITH_LOGIN + offset, SYMBOL_SIZE);
//        internalOffset = OFFSET_SIZE_WITH_SYMBOL;
        mt4Order.setSymbol(ByteUtil.byteToString(symbolNotReversed, 0));

        int digits = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_SYMBOL + offset);
//        internalOffset = OFFSET_SIZE_WITH_DIGITS;
        mt4Order.setDigits(digits);

        int command = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_DIGITS + offset);
//        internalOffset = OFFSET_SIZE_WITH_COMMAND;
        mt4Order.setOrderType(OrderType.of(command));

        if (mt4Order.getOrderType() == OrderType.CREDIT || mt4Order.getOrderType() == OrderType.BALANCE)
            return null;

        try {
            // Volume field
            int lots = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_COMMAND + offset);
//            internalOffset = OFFSET_SIZE_WITH_LOTS;
            mt4Order.setLots(BigDecimal.valueOf(lots).divide(NumberUtil.HUNDRED));
        } catch (Exception e) {
        }

        try {
            int openTime = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_LOTS + offset);
            long openTimeValue = timeComponent.timeIn(openTime);
            if (openTimeValue > System.currentTimeMillis()) {
                openTimeValue = System.currentTimeMillis();
            }
            mt4Order.setOpenTimestamp(openTimeValue);
        } catch (Exception e) {
        }

        int state = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_OPEN_TIME + offset);
        mt4Order.setState(state);

        try {
            double openPrice = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_STATE + offset);
            mt4Order.setOpenPrice(BigDecimal.valueOf(openPrice));
        } catch (Exception e) {
        }

        double stopLoss = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_OPEN_PRICE + offset);
        mt4Order.setStopLossPrice(BigDecimal.valueOf(stopLoss));

        double TakeProfitPrice = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_STOP_LOSS + offset);
        mt4Order.setTakeProfitPrice(BigDecimal.valueOf(TakeProfitPrice));

        try {
            int closeTime = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_TAKE_PROFIT_PRICE + offset);
            long closeTimeValue = timeComponent.timeIn(closeTime);
            if (closeTimeValue > System.currentTimeMillis()) {
                closeTimeValue = System.currentTimeMillis();
            }
            mt4Order.setCloseTimestamp(closeTimeValue);

        } catch (Exception e) {
        }

        int valueDate = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_CLOSE_TIME + offset);
//        internalOffset = OFFSET_SIZE_WITH_VALUE_DATE;
        mt4Order.setValueDate(timeComponent.timeIn(valueDate));

        int expiration = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_VALUE_DATE + offset);
//        internalOffset = OFFSET_SIZE_WITH_EXPIRATION;
        mt4Order.setExpiration(timeComponent.timeIn(expiration));

        int convReserv = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_EXPIRATION + offset);
//        internalOffset = OFFSET_SIZE_WITH_CONC_RESERC;
        mt4Order.setConvReserv(convReserv);

        double openRate = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_CONC_RESERC + offset);
//        internalOffset = OFFSET_SIZE_WITH_OPEN_RATE;
        mt4Order.setOpenRate(BigDecimal.valueOf(openRate));

        double closeRate = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_OPEN_RATE + offset);
//        internalOffset = OFFSET_SIZE_WITH_CLOSE_RATE;
        mt4Order.setCloseRate(BigDecimal.valueOf(closeRate));

        double commission = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_CLOSE_RATE + offset);
//        internalOffset = OFFSET_SIZE_WITH_COMMISSION;
        mt4Order.setCommission(BigDecimal.valueOf(commission));

        double commission_agent = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_COMMISSION + offset);
//        internalOffset = OFFSET_SIZE_WITH_COMMISSION_AGENT;
        mt4Order.setCommissionAgent(BigDecimal.valueOf(commission_agent));

        double storage = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_COMMISSION_AGENT + offset);
//        internalOffset = OFFSET_SIZE_WITH_STORAGE;
        mt4Order.setSwap(BigDecimal.valueOf(storage));

        double closePrice = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_STORAGE + offset);
//        internalOffset = OFFSET_SIZE_WITH_CLOSE_PRICE;
        mt4Order.setClosePrice(BigDecimal.valueOf(closePrice));

        double profit = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_CLOSE_PRICE + offset);
//        internalOffset = OFFSET_SIZE_WITH_PROFIT;
        mt4Order.setProfit(BigDecimal.valueOf(profit));

        double taxes = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_PROFIT + offset);
//        internalOffset = OFFSET_SIZE_WITH_TAXES;
        mt4Order.setTaxes(BigDecimal.valueOf(taxes));

        int magic = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_TAXES + offset);
//        internalOffset = OFFSET_SIZE_WITH_MAGIC;
        if (magic < 0) {
            magic = -1 * magic;
        }
        mt4Order.setClOrdId(magic);

        byte[] comment = ByteUtil.getNotReversedBytes(orderArray, OFFSET_SIZE_WITH_MAGIC + offset, STRING_COMMENT_SIZE);
//        internalOffset = OFFSET_SIZE_WITH_COMMENT;
        String commentStr = ByteUtil.byteToString(comment, 0);
        try {
            if (!commentStr.isEmpty()) {
                String tmp = StringEscapeUtils.escapeXml11(commentStr);
                mt4Order.setComment(tmp);
            }
        } catch (Exception e) {
        }

        int internalId = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_COMMENT + offset);
//        internalOffset = OFFSET_SIZE_WITH_INTERNAL_ID;
        mt4Order.setInternalId(internalId);

        int activation = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_INTERNAL_ID + offset);
//        internalOffset = OFFSET_SIZE_WITH_ACTIVATION;
        mt4Order.setActivation(activation);

        int spread = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_ACTIVATION + offset);
//        internalOffset = OFFSET_SIZE_WITH_SPREAD;
        mt4Order.setSpread(spread);

        double marginRate = ByteUtil.getDouble(orderArray, OFFSET_SIZE_WITH_SPREAD + offset);
//        internalOffset = OFFSET_SIZE_WITH_MARGIN_RATE;
        mt4Order.setMarginRate(BigDecimal.valueOf(marginRate));

        int timestamp = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_MARGIN_RATE + offset);
//        internalOffset = OFFSET_SIZE_WITH_TIMESTAMP;
        mt4Order.setTimestamp(timeComponent.timeIn(timestamp));

        // reserved
        /*for(int i = 0; i < RESERVED_SIZE; i++)
            internalOffset = INT_BYTE_SIZE;*/
//        internalOffset = OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES;

        int next = ByteUtil.getInt(orderArray, OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES + offset);
//        internalOffset = OFFSET_SIZE_WITH_NEXT;
        mt4Order.setNext(next);

        return mt4Order;
    }

    public static List<Mt4Order> readOrders(byte[] buf, int serverBuild, TimeComponent timeComponent) {
        List<Mt4Order> mt4Orders = new ArrayList<>();

        int i = 0x3600;
        if (serverBuild >= 1290)
            i = 0x3620;
        if (serverBuild >= 1350)
            i = 0x3624;
        while (i < buf.length) {
            try {
                Mt4Order mt4Order = OrderBufParser.parse(buf, i, timeComponent);
                if (mt4Order.getTicket() > 0)
                    mt4Orders.add(mt4Order);
            } catch (Exception e) {
            }
            i += 14 * 16;
        }
        return mt4Orders;
    }


    public static List<OrderUpdateEvent> parseOrderEvents(byte[] buf, TimeComponent timeComponent) throws IOException {
        List<OrderUpdateEvent> orderUpdateEvents = new ArrayList<>();
        for (int i = 0; i < buf.length; i += 272) {
            double balance = ByteUtil.getDouble(buf, i + 24);
            double credit = ByteUtil.getDouble(buf, i + 32);
            Mt4Order mt4Order = OrderBufParser.parse(buf, i + 48, timeComponent);
            if (mt4Order == null) {
                continue;
            }
            OrderType op = mt4Order.getOrderType();
            TradingEvent action = TradingEvent.POSITION_OPEN;
            switch (buf[i + 4]) {
                case 0:
                    if (op == OrderType.BUY || op == OrderType.SELL)
                        action = TradingEvent.POSITION_OPEN;
                    else
                        action = TradingEvent.PENDING_OPEN;
                    break;
                case 1:
                    if (op == OrderType.BALANCE)
                        action = TradingEvent.BALANCE;
                    else if (op == OrderType.CREDIT)
                        action = TradingEvent.CREDIT;
                    else if (op == OrderType.BUY || op == OrderType.SELL)
                        action = TradingEvent.POSITION_CLOSE;
                    else
                        action = TradingEvent.PENDING_CLOSE;
                    break;
                case 2:
                    if (op == OrderType.BUY || op == OrderType.SELL)
                        action = TradingEvent.POSITION_MODIFY;
                    else
                        action = TradingEvent.PENDING_MODIFY;
                    break;
            }
            orderUpdateEvents.add(new OrderUpdateEvent(mt4Order, action, BigDecimal.valueOf(balance), BigDecimal.valueOf(credit)));
        }
        return orderUpdateEvents;
    }
}
