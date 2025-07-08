package com.ob.api.mtx.util;

import com.ob.broker.common.event.EventType;
import com.ob.broker.common.model.OrderStateData;
import com.ob.broker.common.model.OrderTypeData;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BrokerUtil {

    public static EventType eventType(OrderStateData orderStateData, OrderTypeData orderTypeData) {
        return switch (orderStateData) {
            case Opened -> EventType.OPEN_ORDER;
            case Closed, PartialClosed -> EventType.CLOSE_ORDER;
            case Modified -> {
                if (orderTypeData == null || OrderTypeData.Buy.equals(orderTypeData) || OrderTypeData.Sell.equals(orderTypeData))
                    yield EventType.UPDATE_ORDER;
                yield EventType.UPDATE_PENDING_ORDER;
            }
            case Filled -> EventType.FILL_PENDING_ORDER;
            case Rejected -> EventType.REJECT_ORDER;
            case Placed -> EventType.PENDING_ORDER;
            case Cancelled -> EventType.CANCEL_ORDER;
            default -> EventType.INFO;
        };
    }
    public static String key(Long brokerId, Object accountId) {
        return brokerId + "_" + accountId;
    }
}
