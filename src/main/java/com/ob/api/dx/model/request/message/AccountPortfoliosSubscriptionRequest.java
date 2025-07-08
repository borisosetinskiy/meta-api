package com.ob.api.dx.model.request.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class AccountPortfoliosSubscriptionRequest {
    final String type = "AccountPortfoliosSubscriptionRequest";
    String requestId;
    String session;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
    ZonedDateTime timestamp;
    Payload payload;

    @Data
    static
    public class Payload {
        final String requestType = "LIST";
        List<String> accounts;
    }
}


/*
{
    "type": "AccountPortfoliosSubscriptionRequest",
    "requestId": "1",
    "timestamp": "2024-06-06T06:48:48.30Z",
    "session": "5ondoflnmh6qcta7bapf9fn9ll",
    "payload": {
        "requestType": "LIST",
        "accounts": ["default:m_1056663"]
    }
}
 */