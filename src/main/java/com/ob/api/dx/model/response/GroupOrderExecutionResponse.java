package com.ob.api.dx.model.response;

import lombok.Data;

import java.util.List;

@Data
public class GroupOrderExecutionResponse {
    List<OrderExecutionResponse> orderResponses;
}
