package com.ob.api.dx.model.response;

import com.ob.api.dx.model.data.Order;
import lombok.Data;

import java.util.List;

@Data
public class OrdersListResponse {
    List<Order> orders;
}
