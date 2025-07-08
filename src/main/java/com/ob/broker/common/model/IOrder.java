package com.ob.broker.common.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "orderType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CloseOrderData.class, name = "0"),
        @JsonSubTypes.Type(value = OrderData.class, name = "1")
})
public interface IOrder {
    Object getTID();

    Long getBrokerId();

    Object getAccountId();

    Long getTicket();

    OrderTypeData getOrderType();

    String getSymbol();

    Long getTime();

//    default BigDecimal getSwap() {
//        return BigDecimal.ZERO;
//    }
//
//    ;
//
//    default BigDecimal getCommission() {
//        return BigDecimal.ZERO;
//    }
//
//    ;
//
//    default BigDecimal getTaxes() {
//        return BigDecimal.ZERO;
//    }
//
//    ;
}
