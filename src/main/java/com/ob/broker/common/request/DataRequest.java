package com.ob.broker.common.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataRequest implements IRequest {
    Object TID;
    String RID;
    String feUID;
    Object accountId;
    String groupId;
    Long groupTransactionId;
    Long brokerId;
    Long time;
    RequestType requestType;
    final long timestamp = System.currentTimeMillis();
}
