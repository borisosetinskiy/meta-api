package com.ob.broker.common.event;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class PongEvent extends GeneralEvent {
    final Long pongTime;

    public PongEvent(Long brokerId, Object account, EventType eventType, Long pongTime) {
        super(brokerId, account, eventType);
        this.pongTime = pongTime;
    }
}
