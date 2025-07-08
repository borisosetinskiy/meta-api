package com.ob.broker.common.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;



@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupOrderRequest extends OrderRequest {
    List<Long> tickets;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupOrderRequest that = (GroupOrderRequest) o;
        return Objects.equals(brokerId, that.brokerId) && Objects.equals(accountId, that.accountId) && Objects.equals(TID, that.TID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brokerId, accountId, TID);
    }

}
