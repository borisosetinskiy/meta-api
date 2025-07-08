package com.ob.broker.common.event;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class PingEvent extends GeneralEvent {
    final Long pingTime;

    public PingEvent(Long brokerId, Object accountId, EventType eventType, Long pingTime) {
        super(brokerId, accountId, eventType);
        this.pingTime = pingTime;
    }
}
