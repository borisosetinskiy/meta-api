package com.ob.broker.common.request;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderRequest.class, name = "0"),
        @JsonSubTypes.Type(value = GroupOrderRequest.class, name = "1")
})
public interface IRequest {
    Object getTID();
    void setTID(Object tid);
    default String getRID(){return null;};
    default String getFeUID(){return null;};
    default void setFeUID(String feUID){};
    default Long getGroupTransactionId(){return null;};
    default void setGroupTransactionId(Long groupTransactionId){};
    default String getGroupId(){return null;};
    default void setGroupId(String groupId){};
    Object getAccountId();

    Long getBrokerId();

    Long getTime();

    long getTimestamp();

    RequestType getRequestType();

    default Long getCreated() {
        return System.currentTimeMillis();
    }
}
