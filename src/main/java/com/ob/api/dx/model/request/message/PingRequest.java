package com.ob.api.dx.model.request.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.common.ZonedDateTimeDeserializer;
import com.ob.api.dx.util.TimeUtil;
import com.ob.broker.common.JsonService;
import com.ob.broker.common.client.ws.ChannelClient;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PingRequest implements ServerEvent<BaseDxApi> {
//    String type;
    String requestId;
    String session;
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime timestamp;

    @Override
    public void execute(BaseDxApi baseDxApi, ChannelClient channelClient, BiConsumer<String, Object> callback) {
        PingClientMessage pingClientMessage = new PingClientMessage();
        pingClientMessage.setInReplyTo(getRequestId());
        pingClientMessage.setSession(baseDxApi.getUserSession().getToken());
        pingClientMessage.setTimestamp(TimeUtil.now());
        var pong = JsonService.JSON.toJson(pingClientMessage);
        channelClient.send(pong);
    }
}
