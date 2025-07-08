package com.ob.broker.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.IOrder;
import com.ob.broker.common.model.OrderStateData;
import com.ob.broker.common.model.SymbolData;
import com.ob.broker.common.request.IRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderEvent implements Event, SymbolData {
    EventType eventType;
    Object TID;
    Long brokerId;
    Object accountId;
    Long ticket;
    String symbol;
    CodeException error;
    IOrder order;
    OrderStateData orderStateData;
    IRequest request;
    Code code;
    final long timestamp = System.currentTimeMillis();

}
