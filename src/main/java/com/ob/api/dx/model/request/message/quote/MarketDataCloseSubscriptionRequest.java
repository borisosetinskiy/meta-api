package com.ob.api.dx.model.request.message.quote;

import lombok.Data;

@Data
public class MarketDataCloseSubscriptionRequest {
    final String type = "MarketDataCloseSubscriptionRequest";
    String requestId;
    String refRequestId;
    String session;
}
