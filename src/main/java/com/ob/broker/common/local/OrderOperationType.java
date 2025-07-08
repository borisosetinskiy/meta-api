package com.ob.broker.common.local;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.OrderTypeData;
import lombok.Getter;

public enum OrderOperationType {
    BUY_LIMIT(2, -1, 0), SELL_LIMIT(3, 1, 1), BUY_STOP(4, 1, 0), SELL_STOP(5, -1, 1);

    private final int value;
    @Getter
    private final int direction;
    @Getter
    private final int side;


    OrderOperationType(int value, int direction, int side) {
        this.value = value;
        this.direction = direction;
        this.side = side;
    }

    public static OrderOperationType toOrderOperationType(OrderTypeData orderTypeData) {
        if (OrderTypeData.BuyLimitLocal.equals(orderTypeData)) {
            return BUY_LIMIT;
        } else if (OrderTypeData.BuyStopLocal.equals(orderTypeData)) {
            return BUY_STOP;
        } else if (OrderTypeData.SellLimitLocal.equals(orderTypeData)) {
            return SELL_LIMIT;
        } else if (OrderTypeData.SellStopLocal.equals(orderTypeData)) {
            return SELL_STOP;
        }
        throw new CodeException(Code.INVALID_PARAM);
    }

    public int value() {
        return value;
    }

    public OrderTypeData toOrderTypeData() {
        if (BUY_LIMIT.equals(this)) {
            return OrderTypeData.BuyLimitLocal;
        } else if (BUY_STOP.equals(this)) {
            return OrderTypeData.BuyStopLocal;
        } else if (SELL_STOP.equals(this)) {
            return OrderTypeData.SellStopLocal;
        }
        return OrderTypeData.SellLimitLocal;
    }
}
