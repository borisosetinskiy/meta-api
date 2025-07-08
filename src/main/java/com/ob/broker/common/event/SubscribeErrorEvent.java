package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.error.CodeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscribeErrorEvent implements ErrorEvent {
    Long brokerId;
    Object accountId;
    String symbol;
    boolean active;
    CodeException error;
    EventType eventType;
    final long timestamp = System.currentTimeMillis();
}
