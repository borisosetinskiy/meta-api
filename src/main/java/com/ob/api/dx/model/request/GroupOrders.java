package com.ob.api.dx.model.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.util.List;

@Data
public class GroupOrders {
    List<PlaceOrder> orders;
    @JsonSerialize(using = ContingencyTypeSerializer.class)
    ContingencyType contingencyType = ContingencyType.IF_THEN;

    public enum ContingencyType {
        IF_THEN("IF-THEN");
        final String value;

        ContingencyType(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}
