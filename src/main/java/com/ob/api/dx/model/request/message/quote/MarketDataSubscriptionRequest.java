package com.ob.api.dx.model.request.message.quote;

import lombok.Data;

import java.util.List;

@Data
public class MarketDataSubscriptionRequest {
    final String type = "MarketDataSubscriptionRequest";
    String requestId;
    String session;
    Payload payload;

    @Data
    static
    public class Payload {
        String account;
        List<String> symbols;
        final List<EventTypes> eventTypes = List.of(new EventTypes());
    }

    @Data
    static
    public class EventTypes {
        final String type = "Quote";
        final String format = "COMPACT";
    }
}
/*

{
    "type": "MarketDataCloseSubscriptionRequest",
    "requestId": "2",
    "refRequestId": "1",
    "session": "2gkbiva152dov44d968sf9s0eh"
}
{
    "type": "MarketDataSubscriptionRequest",
    "requestId": "0010",
    "session": "63aqj1feplja8qpb033ii129pu",
    "payload": {
        "account": "default:smarttrader_api_md",
        "symbols": ["EUR/USD","USD/JPY"],
        "eventTypes":[
            {
                "type":"Quote",
                "format":"COMPACT"
            }
        ]
    }
}

{
    "type": "MarketData",
    "inReplyTo": "0010",
    "session": "63aqj1feplja8qpb033ii129pu",
    "timestamp": "2024-06-14T00:33:27.116Z",
    "payload": {
        "events": [
            {
                "symbol": "USD/JPY",
                "type": "Quote",
                "ask": 157.203,
                "bid": 157.173,
                "time": "2024-06-14T00:33:27Z"
            }
        ]
    }
}
 */