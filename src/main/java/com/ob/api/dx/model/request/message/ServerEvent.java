package com.ob.api.dx.model.request.message;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.model.request.message.quote.MarketData;
import com.ob.api.dx.model.request.message.quote.MarketDataSubscriptionClosed;
import com.ob.broker.common.client.ws.ChannelClient;

import java.util.function.BiConsumer;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PingRequest.class, name = "PingRequest"),
        @JsonSubTypes.Type(value = SessionClosed.class, name = "SessionClosed"),
        @JsonSubTypes.Type(value = Reject.class, name = "Reject"),
        @JsonSubTypes.Type(value = AccountPortfolios.class, name = "AccountPortfolios"),
        @JsonSubTypes.Type(value = MarketData.class, name = "MarketData"),
        @JsonSubTypes.Type(value = MarketDataSubscriptionClosed.class, name = "MarketDataSubscriptionClosed")
})
public interface ServerEvent<T extends BaseDxApi> {
//    String getType();

    void execute(T api, ChannelClient channelClient, BiConsumer<String, Object> callback);
}
