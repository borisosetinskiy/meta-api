package com.ob.api.dx.model.data;

import java.util.Map;

public record DxOrders(Map<Long, DxOrderData> orders
        , Map<Long, DxCloseOrderData> closedOrders
        , Map<Long, DxCanceledOrderData> canceledOrders
        , Map<Long, DxRejectOrderData> rejectOrders
        , Map<Long, DxConditionOrderData> sl
        , Map<Long, DxConditionOrderData> tp) {
}
