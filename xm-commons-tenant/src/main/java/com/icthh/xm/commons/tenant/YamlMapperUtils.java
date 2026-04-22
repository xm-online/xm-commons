package com.icthh.xm.commons.tenant;

import java.util.Map;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public final class YamlMapperUtils {

    public static ObjectMapper yamlDefaultMapper() {
        return YAMLMapper.builder()
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }

    public static ObjectMapper yamlDeserializationMapper(Map<DeserializationFeature, Boolean> features) {
        YAMLMapper.Builder builder = YAMLMapper.builder();
        if (features != null) {
            features.forEach((feature, enabled) ->
                    builder.configure(feature, Boolean.TRUE.equals(enabled))
            );
        }

        return builder.build();
    }


}
