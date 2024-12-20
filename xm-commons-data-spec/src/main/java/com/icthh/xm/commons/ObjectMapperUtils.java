package com.icthh.xm.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility class to read/write objects using ObjectMapper
 */
@Slf4j
@UtilityClass
public class ObjectMapperUtils {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    public static Map<String, Object> deserializeToMap(String data) {
        try {
            return isBlank(data) ? new HashMap<>() : jsonMapper.readValue(data, new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("Error during String to JSON converting", e);
            return Collections.emptyMap();
        }
    }

    public static <S> Optional<S> readSpecYml(String tenant, String config, Class<S> specType) {
        try {
            return Optional.ofNullable(ymlMapper.readValue(config, specType));
        } catch (JsonProcessingException e) {
            log.debug("Could not read specification for tenant: {}", tenant, e);
            return Optional.empty();
        }
    }
}
