package com.ob.api.dx.model.response;

import lombok.Data;

@Data
public class OrderExecutionResponse {
    Long orderId;
    Long updateOrderId;
}
/*
{
  "orderId": 709280343,
  "updateOrderId": 709280343
}
 */