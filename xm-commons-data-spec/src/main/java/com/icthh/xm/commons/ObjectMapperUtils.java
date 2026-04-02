package com.icthh.xm.commons;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
    private static final ObjectMapper ymlMapper = YAMLMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    public static Map<String, Object> deserializeToMap(String data) {
        try {
            return isBlank(data) ? new HashMap<>() : jsonMapper.readValue(data, new TypeReference<>() {});
        } catch (JacksonException e) {
            log.warn("Error during String to JSON converting", e);
            return Collections.emptyMap();
        }
    }

    public static <S> Optional<S> readSpecYml(String tenant, String config, Class<S> specType) {
        try {
            return Optional.ofNullable(ymlMapper.readValue(config, specType));
        } catch (JacksonException e) {
            log.debug("Could not read specification for tenant: {}", tenant, e);
            return Optional.empty();
        }
    }
}
