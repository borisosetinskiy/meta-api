package com.ob.api.dx.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.broker.common.model.ContractType;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class InstrumentResponse {
    List<Instrument> instruments;

    public static ContractType toContractType(String type) {
        if (type == null || type.isEmpty()) {
            return ContractType.FOREX;
        }
        return switch (type) {
            case "FOREX" -> ContractType.FOREX;
            case "STOCK" -> ContractType.STOCK;
            case "INDEX" -> ContractType.CFD_INDEX;
            case "COMMODITY" -> ContractType.CFD;
            case "CURRENCY" -> ContractType.CRYPTO;
            default -> ContractType.UNKNOWN;
        };
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static
    class Instrument {
        String type;
        String symbol;
        int version;
        String description;
        double priceIncrement;
        double pipSize;
        String currency;
        double lotSize;
        double multiplier;
        String underlying;
        String product;
        OptionDetails optionDetails;
        ExpirationDetails expirationDetails;
        String settlementType;
        String firstCurrency;
        List<TradingHours> tradingHours;
        String currencyType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static
    class OptionDetails {
        double strike;
        String optionSide;
        String optionStyle;
        double sharesPerContract;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static
    class ExpirationDetails {
        String maturityDate;
        String lastTradeDate;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static
    class TradingHours {
        String weekDay;
        String eventType;
    }

}

/*
{
  "instruments": [
    {
      "type": "CURRENCY",
      "symbol": "string",
      "version": 0,
      "description": "string",
      "priceIncrement": 0,
      "pipSize": 0,
      "currency": "string",
      "lotSize": 0,
      "multiplier": 0,
      "underlying": "string",
      "product": "string",
      "optionDetails": {
        "strike": 0,
        "optionSide": "CALL",
        "optionStyle": "AMERICAN",
        "sharesPerContract": 0
      },
      "expirationDetails": {
        "maturityDate": "string",
        "lastTradeDate": "string"
      },
      "settlementType": "CASH",
      "firstCurrency": "string",
      "tradingHours": [
        {
          "weekDay": "string",
          "eventType": "SESSION_OPEN"
        }
      ],
      "currencyType": "FIAT"
    }
  ]
}
 */