package com.example.crudapp.infrastructure.web;

import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JavaType;
import java.lang.reflect.Type;

public class Jackson3Mapper implements JsonMapper {
    private final ObjectMapper objectMapper;

    public Jackson3Mapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @NotNull
    @Override
    public String toJsonString(@NotNull Object obj, @NotNull Type type) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON using Jackson 3", e);
        }
    }

    @NotNull
    @Override
    public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(targetType);
            return objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize from JSON using Jackson 3", e);
        }
    }
}
