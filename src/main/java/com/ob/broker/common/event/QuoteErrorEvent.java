package com.ob.broker.common.event;

import com.ob.broker.common.error.CodeException;
import lombok.ToString;

@ToString(callSuper = true)
public class QuoteErrorEvent extends GeneralErrorEvent {
    String symbol;

    public QuoteErrorEvent(Long brokerId, Long account, CodeException error, String symbol) {
        super(brokerId, account, 0L, error, EventType.PRICE);
        this.symbol = symbol;
    }
}
