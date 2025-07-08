package com.ob.api.dx.model.request.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.common.ZonedDateTimeDeserializer;
import com.ob.broker.common.client.ws.ChannelClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.event.EventTopic;
import com.ob.broker.common.event.EventType;
import com.ob.broker.common.event.GeneralErrorEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Slf4j
public class Reject implements ServerEvent<BaseDxApi> {
    //    @Setter
//    String type;
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime timestamp;
    Payload payload;

    @Override
    public void execute(BaseDxApi api, ChannelClient channelClient, BiConsumer<String, Object> callback) {
        var code = payload.errorCode;
        if("1".equals(code)) {
            GeneralErrorEvent errorEvent = GeneralErrorEvent.of(
                    api.getBrokerId(),
                    api.getAccountId(),
                    new CodeException(payload.description + ":" + code, Code.REQUEST_REJECTED),
                    EventType.SUBSCRIBE
            );
            errorEvent.setEventType(EventType.DISCONNECT);
            api.getEventProducer().eventConsumer(EventTopic.CONNECT).accept(api, errorEvent);
        }else if ("32".equals(code)){
            GeneralErrorEvent errorEvent = GeneralErrorEvent.of(
                    api.getBrokerId(),
                    api.getAccountId(),
                    new CodeException(payload.getDescription() + ":" + payload.getErrorCode(), Code.REQUEST_REJECTED),
                    EventType.SUBSCRIBE
            );
            api.getEventProducer().eventConsumer(EventTopic.SUBSCRIBE).accept(api, errorEvent);
        }else if("34".equals(code)) {
            GeneralErrorEvent errorEvent = GeneralErrorEvent.of(
                    api.getBrokerId(),
                    api.getAccountId(),
                    new CodeException(payload.getDescription() + ":" + payload.getErrorCode(), Code.REQUEST_REJECTED),
                    EventType.PRICE
            );
            api.getEventProducer().eventConsumer(EventTopic.SUBSCRIBE).accept(api, errorEvent);
            callback.accept("permission", null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class Payload {
        String errorCode;
        String description;
    }
}
/*
{"type":"Reject","timestamp":"2024-06-07T10:51:28.156Z"
,"payload":{"errorCode":"32","description":"Incorrect request parameters: <timestamp>. Possible values: 2020-01-01T00:00:00.00Z, 1577836800000."}}
 */