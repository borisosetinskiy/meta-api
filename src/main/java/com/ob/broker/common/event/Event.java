package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ob.broker.common.model.AccountData;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderEvent.class, name = "0"),
        @JsonSubTypes.Type(value = ConnectEvent.class, name = "1"),
        @JsonSubTypes.Type(value = DisconnectEvent.class, name = "2"),
        @JsonSubTypes.Type(value = SnapshotEvent.class, name = "3"),
        @JsonSubTypes.Type(value = GeneralErrorEvent.class, name = "4"),
        @JsonSubTypes.Type(value = OrderErrorEvent.class, name = "5"),
        @JsonSubTypes.Type(value = AccountData.class, name = "6")
})
public interface Event {
    Long getBrokerId();

    Object getAccountId();

    default Object getTID() {
        return 0L;
    }

    EventType getEventType();

    long getTimestamp();
}
