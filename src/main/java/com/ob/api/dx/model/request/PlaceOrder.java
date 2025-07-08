package com.ob.api.dx.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.api.dx.model.data.Types;
import lombok.Data;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PlaceOrder {
    String account;
    String orderCode;
    Types.Type type;
    String instrument;
    String positionCode;
    int quantity;
    Types.PositionEffect positionEffect;
    BigDecimal limitPrice;
    BigDecimal stopPrice;
    BigDecimal priceOffset;
    Types.PriceLink priceLink;
    Types.Side side;
    Types.Tif tif;

}
/*
{
  "account": "default:m_1056663",
  "orderCode": "smt1",
  "type": "MARKET",
  "instrument": "EUR/USD",
  "quantity": 10000,
  "positionEffect": "OPEN",
  "side": "BUY",
  "tif": "GTC"
}
 */