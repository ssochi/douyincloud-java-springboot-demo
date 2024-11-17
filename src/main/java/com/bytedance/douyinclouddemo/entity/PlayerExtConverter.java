package com.bytedance.douyinclouddemo.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class PlayerExtConverter implements AttributeConverter<PlayerExt, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PlayerExt playerExt) {
        try {
            return objectMapper.writeValueAsString(playerExt);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public PlayerExt convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, PlayerExt.class);
        } catch (JsonProcessingException e) {
            return new PlayerExt();
        }
    }
}