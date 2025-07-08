package com.ob.api.dx.model.request.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ob.api.dx.BaseDxApi;
import com.ob.broker.common.client.ws.ChannelClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Slf4j
public class SessionClosed implements ServerEvent<BaseDxApi> {
//    String type;
    String session;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    ZonedDateTime timestamp;

    @Override
    public void execute(BaseDxApi tradeDxApi, ChannelClient channelClient, BiConsumer<String, Object> callback) {
        log.warn("Session closed {}", this);
    }
}
