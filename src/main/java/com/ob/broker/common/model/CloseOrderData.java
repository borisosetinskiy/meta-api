package com.ob.broker.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PROTECTED)
public class CloseOrderData implements IOrder {
    Long brokerId;
    Object accountId;
    Object TID;
    Long ticket;
    String symbol;
    OrderTypeData orderType;
    String comment;
    BigDecimal lot;
    BigDecimal openPrice;
    BigDecimal closePrice;
    BigDecimal sl;
    BigDecimal tp;
    Long time;
    Long openTime;
    Long closeTime;
    BigDecimal profit;
    BigDecimal swap;
    BigDecimal commission;
    BigDecimal taxes;
    Long transaction;
    OrderData orderData;
    BigDecimal balance;
    Integer version;

}
