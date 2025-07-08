package com.ob.api.dx.model.data;

import com.ob.broker.common.model.OrderData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class DxOrderData extends OrderData implements DxOrder {
    String slTID;
    String tpTID;
    String slOrderCode;
    String tpOrderCode;
    Long slOrderId;
    Long tpOrderId;
    Integer slVersion;
    Integer tpVersion;
    String orderCode;
    Integer orderId;
}
