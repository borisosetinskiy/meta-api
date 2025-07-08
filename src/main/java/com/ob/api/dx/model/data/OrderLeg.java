package com.ob.api.dx.model.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderLeg {
    String instrument;
    String positionEffect;
    String positionCode;
    Types.Side side;
    BigDecimal price;
    BigDecimal legRatio;
    BigDecimal quantity;
    BigDecimal filledQuantity;
    BigDecimal remainingQuantity;
    BigDecimal averagePrice;
}
