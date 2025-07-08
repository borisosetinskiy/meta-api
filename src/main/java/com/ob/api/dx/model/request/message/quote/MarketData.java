package com.ob.api.dx.model.request.message.quote;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.common.ZonedDateTimeDeserializer;
import com.ob.api.dx.model.request.message.ServerEvent;
import com.ob.broker.common.client.ws.ChannelClient;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.model.QuoteData;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiConsumer;

@Data
public class MarketData implements ServerEvent<BaseDxApi> {
    String inReplyTo;
    String session;
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime timestamp;
    Payload payload;

    @Override
    public void execute(BaseDxApi api, ChannelClient channelClient, BiConsumer<String, Object> callback) {
        getPayload().getEvents().forEach(event -> {
            final var quoteData = new QuoteData((short) 0
                    , api.getBrokerId()
                    , api.getAccountId()
                    , event.getSymbol()
                    , event.getBid()
                    , event.getAsk()
                    , event.getTime().toEpochSecond() * 1000, true);
            callback.accept("MarketData", quoteData);
            api.getEventProducer()
                    .eventConsumer(EventTopic.PRICE)
                    .accept(api, quoteData);
        });
    }

    @Data
    public static class Payload {
        List<Event> events;
    }

    @Data
    public static class Event {
        String symbol;
        String type;
        BigDecimal ask;
        BigDecimal bid;
        @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
        ZonedDateTime time;
    }
}
/*

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