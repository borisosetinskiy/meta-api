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
public class GeneralErrorEvent implements ErrorEvent {
    Long brokerId;
    Object accountId;
    Object TID;
    CodeException error;
    EventType eventType;
    final long timestamp = System.currentTimeMillis();

    public static GeneralErrorEvent of(Long brokerId, Object accountId, CodeException error, EventType eventType) {
        return GeneralErrorEvent.builder()
                .brokerId(brokerId)
                .accountId(accountId)
                .error(error)
                .eventType(eventType)
                .build();
    }
}
