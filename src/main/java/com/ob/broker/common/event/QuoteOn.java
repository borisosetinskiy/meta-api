package com.ob.broker.common.event;

import lombok.Data;

@Data
public class QuoteOn implements Event {
    final EventType eventType = EventType.SUBSCRIBE;
    Long brokerId;
    Object accountId;
    final long timestamp = System.currentTimeMillis();
}