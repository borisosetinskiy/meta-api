package com.ob.api.dx.model.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Execution {
    String account;
    String executionCode;
    String orderCode;
    Integer updateOrderId;
    Integer version;
    String clientOrderId;
    String actionCode;
    String instrument;
    Types.Status status;
    Boolean finalStatus;
    BigDecimal filledQuantity;
    BigDecimal lastQuantity;
    BigDecimal filledQuantityNotional;
    BigDecimal lastQuantityNotional;
    BigDecimal remainingQuantity;
    BigDecimal lastPrice;
    BigDecimal averagePrice;
    String transactionTime;
    BigDecimal marginRate;
    String rejectReason;
    String rejectCode;
}
