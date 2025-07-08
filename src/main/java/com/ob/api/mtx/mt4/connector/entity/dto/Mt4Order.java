package com.ob.api.mtx.mt4.connector.entity.dto;

import com.ob.api.mtx.mt4.connector.entity.trading.OrderType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "ticket")
public class Mt4Order {
    private int ticket;
    private int login;
    private int state;
    private long valueDate;
    private long expiration;
    private int convReserv;
    private int internalId;
    private int activation;
    private int spread;
    private long timestamp;
    private int next;
    private long openTimestamp;
    private long closeTimestamp;
    // command
    private OrderType orderType;
    // Volume
    private BigDecimal lots;
    private String symbol;
    private int digits;
    private BigDecimal commissionAgent;
    private BigDecimal taxes;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal stopLossPrice;
    private BigDecimal takeProfitPrice;
    private int clOrdId;//magicNumber
    private BigDecimal swap;
    private BigDecimal commission;
    private String comment; //Can be rewritten by server.
    private BigDecimal profit;
    private BigDecimal openRate;//Convertation Rate from profit currency to group deposit currency for open time.
    private BigDecimal closeRate;//Convertation Rate from profit currency to group deposit currency for close time.
    private BigDecimal marginRate;//Rate of Convertation from margin currency to deposit one.


    public Mt4Order(Mt4Order other, int ticket, BigDecimal lots, String comment
            , BigDecimal closePrice, long closeTimestamp) {
        this.ticket = ticket;
        this.login = other.login;
        this.state = other.state;
        this.valueDate = other.valueDate;
        this.expiration = other.expiration;
        this.convReserv = other.convReserv;
        this.internalId = other.internalId;
        this.activation = other.activation;
        this.spread = other.spread;
        this.timestamp = other.timestamp;
        this.next = other.next;
        this.openTimestamp = other.openTimestamp;
        this.closeTimestamp = closeTimestamp;
        this.orderType = other.orderType;
        this.lots = lots;
        this.symbol = other.symbol;
        this.digits = other.digits;
        this.commissionAgent = other.commissionAgent;
        this.taxes = other.taxes;
        this.openPrice = other.openPrice;
        this.closePrice = closePrice;
        this.stopLossPrice = other.stopLossPrice;
        this.takeProfitPrice = other.takeProfitPrice;
        this.clOrdId = other.clOrdId;
        this.commission = other.commission;
        this.comment = comment;
        this.profit = BigDecimal.ZERO;
        this.openRate = other.openRate;
        this.closeRate = other.closeRate;
        this.marginRate = other.marginRate;
    }


    public Mt4Order(int ticket, int login, int state, int valueDate, int expiration, int convReserv, int internalId, int activation, int spread, long timestamp, int next, long openTimestamp, long closeTimestamp, int orderType, BigDecimal lots, String symbol, int digits, BigDecimal commissionAgent, BigDecimal taxes, BigDecimal openPrice, BigDecimal closePrice, BigDecimal stopLossPrice, BigDecimal takeProfitPrice, int clOrdId, BigDecimal swap, BigDecimal commission, String comment, BigDecimal profit, BigDecimal openRate, BigDecimal closeRate, BigDecimal marginRate) {
        this.ticket = ticket;
        this.login = login;
        this.state = state;
        this.valueDate = valueDate;
        this.expiration = expiration;
        this.convReserv = convReserv;
        this.internalId = internalId;
        this.activation = activation;
        this.spread = spread;
        this.timestamp = timestamp;
        this.next = next;
        this.openTimestamp = openTimestamp;
        this.closeTimestamp = closeTimestamp;
        this.orderType = OrderType.of(orderType);
        this.lots = lots;
        this.symbol = symbol;
        this.digits = digits;
        this.commissionAgent = commissionAgent;
        this.taxes = taxes;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.stopLossPrice = stopLossPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.clOrdId = clOrdId;
        this.swap = swap;
        this.commission = commission;
        this.comment = comment;
        this.profit = profit;
        this.openRate = openRate;
        this.closeRate = closeRate;
        this.marginRate = marginRate;
    }

}

