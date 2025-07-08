package com.ob.api.mtx.mt4.connector.entity;

import com.ob.broker.common.error.Code;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum StatusCode {
    SERVER_DECIDED_TO_DISCONNECT(-1000), OK_ANSWER(0), OK_REQUEST(1), COMMON_ERROR(2), INVALID_PARAM(3),
    SERVER_BUSY(4), OLD_VERSION(5), NO_CONNECT(6), NOT_ENOUGH_RIGHTS(7), ORDER_NOT_FOUND(160),
    TOO_FREQUENT_REQUEST(8), SECRET_KEY_REQUIRED(0xD), INVALID_ONETIME_PASSWORD(0xE), ACCOUNT_DISABLED(0x40), INVALID_ACCOUNT(0x41),
    TRADE_TIMEOUT(0x80), INVALID_PRICES(0x81), INVALID_SL_TP(0x82), INVALID_VOLUME(0x83), MARKET_CLOSED(0x84), TRADE_DISABLED(0x85),
    NOT_MONEY(0x86), PRICE_CHANGED(0x87), OFF_QUOTES(0x88), BROKER_BUSY(0x89), REQUOTE(0x8A), ORDER_LOCKED(0x8B), LONG_POS_ALLOWED(0x8C),
    TOO_MANY_REQUESTS(0x8D), ORDER_ACCEPTED(0x8E), ORDER_IN_PROCESS(0x8F), REQUEST_CANCELLED(0x90), MODIFICATIONS_DENIED(0x91),
    TRADE_CONTEXT_BUSY(0x92), EXPIRATION_DISABLED(0x93), TOO_MANY_ORDERS(0x94), HEDGE_PROHIBITED(0x95), RPROHIBITED_FIFO(0x96), UNKNOWN_TRADE_TYPE(170), UNKNOWN_TRADE_OPERATION(171), DECOMPRESSION_ERROR(666), LOGIN_ID_FAIL(1000), BROKEN_CONNECT(-1), HISTORY_LOAD_ERROR(737);

    final static Map<Integer, Code> CODE_BY_NUMBER = Map.ofEntries(
            Pair.of(HISTORY_LOAD_ERROR.id, Code.HISTORY_LOAD_ERROR),
            Pair.of(UNKNOWN_TRADE_OPERATION.id, Code.UNSUPPORTED_OPERATION),
            Pair.of(UNKNOWN_TRADE_TYPE.id, Code.UNSUPPORTED_OPERATION),
            Pair.of(ORDER_NOT_FOUND.id, Code.NOT_FOUND),
            Pair.of(LOGIN_ID_FAIL.id, Code.LOGIN_ID_FAIL),
            Pair.of(SERVER_DECIDED_TO_DISCONNECT.id, Code.SERVER_DECIDED_TO_DISCONNECT),
            Pair.of(OK_ANSWER.id, Code.DONE),
            Pair.of(OK_REQUEST.id, Code.OK),
            Pair.of(COMMON_ERROR.id, Code.COMMON_ERROR),
            Pair.of(INVALID_PARAM.id, Code.INVALID_PARAM),
            Pair.of(SERVER_BUSY.id, Code.SERVER_BUSY),
            Pair.of(OLD_VERSION.id, Code.OLD_VERSION),
            Pair.of(NO_CONNECT.id, Code.NO_CONNECTION),
            Pair.of(NOT_ENOUGH_RIGHTS.id, Code.NOT_PERMISSION),
            Pair.of(TOO_FREQUENT_REQUEST.id, Code.TOO_FREQUENT_REQUEST),
            Pair.of(SECRET_KEY_REQUIRED.id, Code.NOT_PERMISSION),
            Pair.of(INVALID_ONETIME_PASSWORD.id, Code.INVALID_PASSWORD),
            Pair.of(ACCOUNT_DISABLED.id, Code.ACCOUNT_DISABLED),
            Pair.of(INVALID_ACCOUNT.id, Code.INVALID_ACCOUNT),
            Pair.of(TRADE_TIMEOUT.id, Code.OPERATION_TIMEOUT),
            Pair.of(INVALID_PRICES.id, Code.INVALID_PRICE),
            Pair.of(INVALID_SL_TP.id, Code.INVALID_SL_TP),
            Pair.of(INVALID_VOLUME.id, Code.INVALID_VOLUME),
            Pair.of(MARKET_CLOSED.id, Code.MARKET_CLOSED),
            Pair.of(TRADE_DISABLED.id, Code.TRADE_DISABLED),
            Pair.of(NOT_MONEY.id, Code.NO_MONEY),
            Pair.of(PRICE_CHANGED.id, Code.PRICE_CHANGED),
            Pair.of(OFF_QUOTES.id, Code.OFF_QUOTES),
            Pair.of(BROKER_BUSY.id, Code.SERVER_BUSY),
            Pair.of(REQUOTE.id, Code.REQUOTE),
            Pair.of(ORDER_LOCKED.id, Code.ORDER_LOCKED),
            Pair.of(LONG_POS_ALLOWED.id, Code.ONLY_LONG_POSITION),
            Pair.of(TOO_MANY_REQUESTS.id, Code.TOO_FREQUENT_REQUEST),
            Pair.of(ORDER_ACCEPTED.id, Code.ORDER_ACCEPTED),
            Pair.of(ORDER_IN_PROCESS.id, Code.ORDER_IN_PROCESS),
            Pair.of(REQUEST_CANCELLED.id, Code.REQUEST_CANCELLED),
            Pair.of(MODIFICATIONS_DENIED.id, Code.MODIFICATIONS_DENIED),
            Pair.of(TRADE_CONTEXT_BUSY.id, Code.SERVER_BUSY),
            Pair.of(EXPIRATION_DISABLED.id, Code.EXPIRATION_DISABLED),
            Pair.of(TOO_MANY_ORDERS.id, Code.TOO_MANY_TRADE_REQUESTS),
            Pair.of(HEDGE_PROHIBITED.id, Code.HEDGE_PROHIBITED),
            Pair.of(RPROHIBITED_FIFO.id, Code.PROHIBITED_FIFO),
            Pair.of(DECOMPRESSION_ERROR.id, Code.DECOMPRESSION_ERROR));


    final static Map<Integer, StatusCode> STATUS_CODE_BY_NUMBER = new HashMap<>();

    static {
        for (StatusCode statusCode : StatusCode.values()) {
            STATUS_CODE_BY_NUMBER.put(statusCode.id, statusCode);
        }
    }

    private final int id;

    StatusCode(int id) {
        this.id = id;
    }

    public static Code codeByNumber(int id) {
        return CODE_BY_NUMBER.getOrDefault(id, Code.COMMON_ERROR);
    }

    public static StatusCode getById(int id) {
        return STATUS_CODE_BY_NUMBER.getOrDefault(id, StatusCode.COMMON_ERROR);
    }

}
