package com.ob.api.dx.model.request.inner;

import com.ob.broker.common.request.IRequest;
import com.ob.broker.common.request.RequestType;
import com.ob.broker.util.TypeUtil;
import lombok.Data;
import lombok.Getter;

@Data
public class DxPendingRequest implements IRequest {
    @Getter
    final Long time = System.currentTimeMillis();
    Long TID;
    String RID;
    String feUID;
    String groupId;
    Object accountId;
    Long groupTransactionId;
    Long brokerId;
    final long timestamp = System.currentTimeMillis();

    @Override
    public void setTID(Object tid) {
        this.TID = TypeUtil.castType(tid, Long.class);
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.LOAD_PENDING;
    }
}
