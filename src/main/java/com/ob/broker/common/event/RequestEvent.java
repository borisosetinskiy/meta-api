package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.request.IOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestEvent implements Event {
    final EventType eventType = EventType.REQUEST;
    Long brokerId;
    Object accountId;
    Type type;
    Object response;
    IOrderRequest orderRequest;
    final long timestamp = System.currentTimeMillis();

    public enum Type {
        ORDER,
        TRADE,
        ACCOUNT,
        INSTRUMENT,
        HISTORY
    }
}
