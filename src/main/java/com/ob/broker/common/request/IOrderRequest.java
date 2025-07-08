package com.ob.broker.common.request;

import java.math.BigDecimal;

public interface IOrderRequest extends IRequest {
    default Long getTicket(){
        return null;
    };

    default BigDecimal getPrice(){
        return null;
    };

    default BigDecimal getLot(){
        return null;
    };
}
