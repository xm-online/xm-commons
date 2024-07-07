package com.icthh.xm.commons.flow.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class YamlConverter {
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()
        .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
    )
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule());

    @SneakyThrows
    public String writeConfig(Object config) {
        return objectMapper.writeValueAsString(config);
    }
}
