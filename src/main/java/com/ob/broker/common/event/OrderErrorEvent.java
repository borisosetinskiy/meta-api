package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.IOrder;
import com.ob.broker.common.model.SymbolData;
import com.ob.broker.common.request.IRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderErrorEvent extends GeneralErrorEvent implements SymbolData {
    IRequest request;
    IOrder order;

    public OrderErrorEvent(Long brokerId, Object accountId, Object TID, CodeException error
            , EventType eventType, IRequest request, IOrder order) {
        super(brokerId, accountId, TID, error, eventType);
        this.request = request;
        this.order = order;
    }

    @Override
    public String getSymbol() {
        return order != null ? order.getSymbol() : "";
    }
}
