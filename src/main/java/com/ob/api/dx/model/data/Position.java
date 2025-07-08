package com.ob.api.dx.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Position {
    String account;
    int version;
    String positionCode;
    String symbol;
    BigDecimal quantity;
    Types.Side side;
    Integer quantityNotional;
    ZonedDateTime openTime;
    BigDecimal openPrice;
    ZonedDateTime lastUpdateTime;
    BigDecimal marginRate;
}
/*
{
          "account": "default:m_1056663",
          "version": 16,
          "positionCode": "698221930",
          "symbol": "AUD/USD",
          "quantity": 1000,
          "side": "SELL",
          "quantityNotional": 1000,
          "openTime": "2024-05-13T09:05:20.172Z",
          "openPrice": 0.66091,
          "lastUpdateTime": "2024-05-13T09:05:20.172Z",
          "marginRate": 0.005
        }
 */