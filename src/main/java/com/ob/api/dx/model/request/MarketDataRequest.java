package com.ob.api.dx.model.request;

import lombok.Data;

import java.util.List;

@Data
public class MarketDataRequest {
    List<String> symbols;
    List<MarketDataEventType> eventTypes;
    String account;


    @Data
    public static class MarketDataEventType {
        final String type = "Quote";
        final String format = "COMPACT";
    }
}


/*
{
  "symbols": [
    "EUR/USD"
  ],
  "eventTypes": [
    {
      "type": "Quote",
      "format": "COMPACT"
    }
  ],
  "account": "string"
}
 */