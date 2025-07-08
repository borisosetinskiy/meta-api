package com.ob.broker.common.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.model.OrderTypeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;


@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderRequest implements IOrderRequest {
    final Long created = System.currentTimeMillis();
    String RID;
    String feUID;
    String groupId;
    Long groupTransactionId;
    Long brokerId;
    Object accountId;
    Long userId;
    Object TID;
    Long time;
    Long ticket;
    int version;
    OrderTypeData type;
    String symbol;
    String comment;
    BigDecimal lot;
    BigDecimal price;
    BigDecimal sl;
    BigDecimal tp;
    BigDecimal priceOffset;
    RequestType requestType;
    Integer requestId;
    long sendTime;

    final long timestamp = System.currentTimeMillis();

    public boolean isBuy() {
        return type == OrderTypeData.Buy
               || type == OrderTypeData.BuyLimit
               || type == OrderTypeData.BuyStop;
    }

    public boolean isLimit() {
        return type == OrderTypeData.BuyLimit
               || type == OrderTypeData.SellLimit;
    }

    public boolean isStop() {
        return type == OrderTypeData.BuyStop
               || type == OrderTypeData.SellStop;
    }

    public boolean isSell() {
        return type == OrderTypeData.Sell
               || type == OrderTypeData.SellLimit
               || type == OrderTypeData.SellStop;
    }

    public boolean hasSl() {
        return sl != null;
    }

    public boolean hasTp() {
        return tp != null;
    }

    public boolean hasConditions() {
        return hasTp() || hasSl();
    }

    public boolean isClose() {
        return requestType == RequestType.CLOSE;
    }

    public boolean isPending() {
        return requestType == RequestType.PENDING;
    }

    public boolean isMarket() {
        return requestType == RequestType.OPEN;
    }

    public boolean isMarketUpdate() {
        return requestType == RequestType.UPDATE;
    }

    public boolean isPendingUpdate() {
        return requestType == RequestType.UPDATE_PENDING;
    }

    public boolean isPendingCancel() {
        return requestType == RequestType.CANCEL_PENDING;
    }

    public boolean isLocalPending() {
        return requestType == RequestType.LOCAL_PENDING;
    }

    public boolean isLocalPendingUpdate() {
        return requestType == RequestType.UPDATE_LOCAL_PENDING;
    }

    public boolean isLocalPendingCancel() {
        return requestType == RequestType.CANCEL_LOCAL_PENDING;
    }

    public boolean isLocal() {
        return isLocalPending() || isLocalPendingUpdate() || isLocalPendingCancel();
    }

    public boolean isUpdate() {
        return isPendingUpdate() || isMarketUpdate();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderRequest that = (OrderRequest) o;
        return Objects.equals(brokerId, that.brokerId) && Objects.equals(accountId, that.accountId) && Objects.equals(ticket, that.ticket) && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brokerId, accountId, ticket, symbol);
    }

}
