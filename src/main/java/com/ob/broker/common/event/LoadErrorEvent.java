package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.error.CodeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoadErrorEvent implements ErrorEvent {
    Long brokerId;
    Long accountId;
    CodeException error;
    EventType eventType;
    final long timestamp = System.currentTimeMillis();
}
