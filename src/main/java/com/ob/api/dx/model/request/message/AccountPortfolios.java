package com.ob.api.dx.model.request.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ob.api.dx.BaseDxApi;
import com.ob.api.dx.common.ZonedDateTimeDeserializer;
import com.ob.api.dx.model.data.Portfolios;
import com.ob.broker.common.client.ws.ChannelClient;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class AccountPortfolios implements ServerEvent<BaseDxApi> {
    //    String type;
    String inReplyTo;
    String session;
    Portfolios payload;
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime timestamp;

    @Override
    public void execute(BaseDxApi tradeDxApi, ChannelClient channelClient, BiConsumer<String, Object> callback) {
        callback.accept("AccountPortfolios", payload);
    }
}