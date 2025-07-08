package com.ob.api.dx.model.data;

import com.ob.broker.common.model.IOrder;
import com.ob.broker.common.model.OrderTypeData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DxRejectOrderData implements DxOrder, IOrder {
    Long brokerId;
    Object accountId;
    BigDecimal price;
    BigDecimal lot;
    Long time;
    String reason;
    String symbol;
    OrderTypeData orderType;
    Long ticket;
    String TID;
    Integer version;

}
