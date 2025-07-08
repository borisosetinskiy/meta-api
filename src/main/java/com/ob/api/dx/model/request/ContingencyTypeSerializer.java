package com.ob.api.dx.model.request;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ContingencyTypeSerializer extends JsonSerializer<GroupOrders.ContingencyType> {
    @Override
    public void serialize(GroupOrders.ContingencyType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}