package com.ob.api.dx.model.data;

import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class Order {
    String account;
    Integer version;
    Integer orderId;
    String orderCode;
    String clientOrderId;
    String actionCode;
    Integer legCount;
    Types.Type type;
    String instrument;
    Types.Status status;
    Boolean finalStatus;
    List<OrderLeg> legs;
    Types.Side side;
    Types.Tif tif;
    ZonedDateTime issueTime;
    ZonedDateTime transactionTime;
    BigDecimal marginRate;
    List<OrderLink> links;
    List<Execution> executions;
    List<CashTransaction> cashTransactions;
    BigDecimal priceOffset;
    Types.PriceLink priceLink;
    String externalOrderId;
    Integer hedgedOrderId;
}
