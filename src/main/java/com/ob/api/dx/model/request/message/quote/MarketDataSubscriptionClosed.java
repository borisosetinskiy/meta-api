package com.ob.api.dx.model.request.message.quote;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.common.ZonedDateTimeDeserializer;
import com.ob.api.dx.model.request.message.ServerEvent;
import com.ob.broker.common.client.ws.ChannelClient;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

@Data
public class MarketDataSubscriptionClosed implements ServerEvent<BaseDxApi> {
    //    String type;
    String inReplyTo;
    String session;
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime timestamp;

    @Override
    public void execute(BaseDxApi api, ChannelClient channelClient, BiConsumer<String, Object> callback) {
        callback.accept("MarketDataSubscriptionClosed", inReplyTo);
    }
}

/*
{"type":"MarketDataSubscriptionClosed","inReplyTo":"3","session":"1h9g8ggtn7bi1jp7g274a80els","timestamp":"2024-06-14T16:38:58.510Z"}
 */