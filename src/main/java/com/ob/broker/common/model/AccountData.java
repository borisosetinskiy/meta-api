package com.ob.broker.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.event.Event;
import com.ob.broker.common.event.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountData implements Event {
    final long timestamp = System.currentTimeMillis();
    Long brokerId;
    Object accountId;
    BigDecimal balance;
    BigDecimal credit;
    Integer amountScale;
    String currency;
    Integer leverage;
    Boolean isDemo;
    Integer version;

    @Override
    public EventType getEventType() {
        return EventType.UPDATE_ACCOUNT;
    }
}
