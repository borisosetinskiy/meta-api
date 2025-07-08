package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class AccountSnapshot implements Event {
    Long brokerId;
    Object accountId;
    BigDecimal balance;
    BigDecimal credit;
    Integer amountScale;
    String currency;
    Integer leverage;
    Integer version;
    Integer marginMode;
    final long timestamp = System.currentTimeMillis();

    @Override
    public EventType getEventType() {
        return EventType.LOAD_ACCOUNT;
    }
}
