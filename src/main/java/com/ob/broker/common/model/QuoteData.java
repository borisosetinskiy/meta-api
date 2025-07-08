package com.ob.broker.common.model;

import com.ob.broker.common.event.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuoteData implements SymbolData {
    short code;
    Long brokerId;
    Object accountId;
    String symbol;
    BigDecimal bid;
    BigDecimal ask;
    long time;
    Boolean tradable;
    final long timestamp = System.currentTimeMillis();

    @Override
    public EventType getEventType() {
        return EventType.PRICE;
    }
}
