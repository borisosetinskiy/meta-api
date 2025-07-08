package com.ob.broker.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;


@Builder(toBuilder = true)
@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = {"ticket", "brokerId", "accountId", "symbol"})
public class OrderConditionData implements IOrder {
    Object TID;
    Long brokerId;
    Object accountId;
    Long ticket;
    OrderTypeData orderType;
    String symbol;
    BigDecimal price;
    Long time;
    Integer version;
}
