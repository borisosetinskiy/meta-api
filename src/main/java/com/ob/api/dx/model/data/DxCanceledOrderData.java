package com.ob.api.dx.model.data;

import com.ob.broker.common.model.OrderData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class DxCanceledOrderData extends OrderData implements DxOrder {
    String orderCode;
    Integer orderId;
}
