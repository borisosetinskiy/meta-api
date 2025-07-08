package com.ob.api.dx.model.request.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.ZonedDateTime;


@Data
public class PingClientMessage {
    final String type = "Ping";
    String inReplyTo;
    String session;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
    ZonedDateTime timestamp;
}
/*

 */