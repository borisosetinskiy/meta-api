package com.ob.broker.common.request;

import com.ob.broker.common.event.EventType;
import lombok.Getter;

@Getter
public enum RequestType {
    CLOSE(1, EventType.CLOSE_ORDER), CLOSE_SYMBOL(2, EventType.CLOSE_ORDER)
    , CLOSE_ALL(3, EventType.CLOSE_ORDER), CLOSE_TYPE(4, EventType.CLOSE_ORDER)
    , OPEN(5, EventType.OPEN_ORDER), UPDATE(6, EventType.UPDATE_ORDER)
    , CANCEL_PENDING(7, EventType.CANCEL_ORDER)
    , PENDING(8, EventType.PENDING_ORDER), UPDATE_PENDING(9, EventType.UPDATE_PENDING_ORDER)
    , CANCEL_LOCAL_PENDING(10, EventType.CANCEL_ORDER)
    , LOCAL_PENDING(11, EventType.PENDING_ORDER), UPDATE_LOCAL_PENDING(12, EventType.UPDATE_PENDING_ORDER)
    , LOAD_LOCAL_PENDING(13, EventType.PENDING_ORDER)
    , LOGON(14, EventType.LOGON), LOGOUT(15, EventType.DISCONNECT)
    , LOAD_OPEN(16, EventType.LOAD_OPEN)
    , LOAD_PENDING(17, EventType.LOAD_PENDING)
    , SET_TP(18, EventType.UPDATE_ORDER), SET_SL(19, EventType.UPDATE_ORDER)
    , LOAD_HISTORY(20, EventType.LOAD_HISTORY)
    , LOAD_CLOSE(21, EventType.CLOSE_ORDER)
    , CLOSE_GROUP(22, EventType.CLOSE_ORDER)
    , CANCEL_GROUP(23, EventType.CANCEL_ORDER)
    , CANCEL_LOCAL_GROUP(24, EventType.CANCEL_ORDER)
    , SET_TP_PENDING(25, EventType.UPDATE_PENDING_ORDER)
    , SET_SL_PENDING(26, EventType.UPDATE_PENDING_ORDER)
    ;

    final int intValue;
    final EventType eventType;

    RequestType(int intValue, EventType eventType) {
        this.intValue = intValue;
        this.eventType = eventType;
    }

}
