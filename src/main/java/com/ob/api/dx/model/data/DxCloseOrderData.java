package com.ob.api.dx.model.data;

import com.ob.broker.common.model.CloseOrderData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class DxCloseOrderData extends CloseOrderData implements DxOrder {
    Long positionId;
    String positionCode;
}
