package com.ob.api.mtx.mt4;

import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.OrderData;
import lombok.Getter;

@Getter
public class OrderException extends CodeException {
    final OrderData orderData;

    public OrderException(CodeException codeException, OrderData orderData) {
        super(codeException.getMessage(), codeException.getCode());
        this.orderData = orderData;
    }
}
