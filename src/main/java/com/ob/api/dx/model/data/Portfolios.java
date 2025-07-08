package com.ob.api.dx.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Portfolios {
    List<Portfolio> portfolios;
}
/*
{
  "portfolios": [
    {
      "account": "default:m_1056663",
      "version": 28,
      "balances": [
        {
          "account": "default:m_1056663",
          "version": 28,
          "value": 100003.87,
          "currency": "USD"
        }
      ],
      "positions": [
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
      ],
      "orders": [
        {
          "account": "default:m_1056663",
          "version": 28,
          "orderId": 703106968,
          "orderCode": "2020327:4482",
          "clientOrderId": "4482",
          "actionCode": "2020327:4482",
          "legCount": 1,
          "type": "LIMIT",
          "instrument": "BTC/USD",
          "status": "WORKING",
          "finalStatus": false,
          "legs": [
            {
              "instrument": "BTC/USD",
              "positionEffect": "OPEN",
              "positionCode": "703106968",
              "side": "BUY",
              "price": 67526.59,
              "legRatio": 1,
              "quantity": 0.01,
              "filledQuantity": 0,
              "remainingQuantity": 0.01,
              "averagePrice": 0
            }
          ],
          "side": "SELL",
          "tif": "GTC",
          "marginRate": 0.005,
          "issueTime": "2024-05-19T11:54:24.323Z",
          "transactionTime": "2024-05-19T11:54:24.323Z",
          "executions": [
            {
              "account": "default:m_1056663",
              "executionCode": "2045251:29015124",
              "orderCode": "2020327:4482",
              "updateOrderId": 703106968,
              "version": 28,
              "clientOrderId": "4482",
              "actionCode": "2020327:4482",
              "status": "WORKING",
              "finalStatus": false,
              "filledQuantity": 0,
              "lastQuantity": 0,
              "filledQuantityNotional": 0,
              "lastQuantityNotional": 0,
              "transactionTime": "2024-05-19T11:54:24.323Z",
              "marginRate": 0.005
            }
          ],
          "cashTransactions": []
        }
      ]
    }
  ]
}
 */