package com.ob.broker.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JsonService {
    public static JsonService JSON = new JsonService();
    final ObjectMapper objectMapper = new ObjectMapper();

    {
        var javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new MillisOrLocalDateTimeDeserializer());
        objectMapper.registerModule(javaTimeModule);
    }

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new CodeException("Error converting object to JSON", e, Code.PARSE_ERROR);
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new CodeException("Error converting JSON to object", e, Code.PARSE_ERROR);
        }
    }
}
