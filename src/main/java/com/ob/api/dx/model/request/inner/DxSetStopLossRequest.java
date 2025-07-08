package com.ob.api.dx.model.request.inner;

import com.ob.broker.common.request.IOrderRequest;
import com.ob.broker.common.request.RequestType;
import com.ob.broker.util.TypeUtil;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
public class DxSetStopLossRequest implements IOrderRequest {
    @Getter
    final Long time = System.currentTimeMillis();
    Long TID;
    String RID;
    String feUID;
    String groupId;
    Long groupTransactionId;
    Long ticket;
    Object accountId;
    Long brokerId;
    BigDecimal price;
    RequestType requestType;
    final long timestamp = System.currentTimeMillis();

    @Override
    public void setTID(Object tid) {
        this.TID = TypeUtil.castType(tid, Long.class);
    }

    @Override
    public Long getCreated() {
        return time;
    }


    @Override
    public BigDecimal getLot() {
        return BigDecimal.ZERO;
    }
}
