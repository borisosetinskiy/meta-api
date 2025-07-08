package com.ob.api.dx.model.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CashTransaction {
    String account;
    String transactionCode;
    String orderCode;
    String tradeCode;
    Integer version;
    String clientOrderId;
    Type type;
    BigDecimal value;
    String currency;
    String transactionTime;

    public enum Type {
        COMMISSION, FINANCING, DEPOSIT, WITHDRAWAL, COST, SETTLEMENT, ADJUSTMENT, EX_DIVIDEND, REBATE, NEGATIVE_BALANCE_CORRECTION, BUST
    }
}
