package com.ob.api.dx.model.data;

import com.ob.broker.common.model.OrderConditionData;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DxConditionOrderData extends OrderConditionData implements DxOrder {
    Types.Type type;
    String orderCode;
}
