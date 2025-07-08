package com.ob.api.dx.util;

import com.ob.api.dx.model.data.Types;
import com.ob.broker.common.model.OrderTypeData;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TradeUtil {
    public static Types.Side side(OrderTypeData orderTypeData){
        return switch (orderTypeData) {
            case BuyStop, BuyLimit, Buy -> Types.Side.BUY;
            case SellStop, SellLimit, Sell -> Types.Side.SELL;
            default -> null;
        };

    }

    public static Types.Type type(OrderTypeData orderTypeData){
        return switch (orderTypeData) {
            case BuyStop, SellStop -> Types.Type.STOP;
            case BuyLimit, SellLimit -> Types.Type.LIMIT;
            case Buy, Sell -> Types.Type.MARKET;
            default -> null;
        };
    }

    public static Types.PositionEffect positionEffect(OrderTypeData orderTypeData){
        return switch (orderTypeData) {
            case  BuyStop, BuyLimit, SellStop, SellLimit  -> Types.PositionEffect.OPEN;
            case Sell, Buy-> Types.PositionEffect.CLOSE;
            default -> null;
        };
    }
}