package com.icthh.xm.commons.domainevent.outbox.domain.converter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    public MapToStringConverter() {
        ;
    }

    @Override
    public String convertToDatabaseColumn(Map<String, Object> data) {
        try {
            return mapper.writeValueAsString(data != null ? data : Map.of());
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot convert map to JSON string", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String data) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        try {
            return mapper.readValue(StringUtils.isNoneBlank(data) ? data : "{}", typeRef);
        } catch (JacksonException e) {
            log.warn("Error during String to JSON converting", e);
            return Collections.emptyMap();
        }
    }

}
