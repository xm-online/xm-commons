package com.icthh.xm.commons.tenant;

import java.util.Map;
import java.util.function.Consumer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public final class YamlMapperUtils {

    public static ObjectMapper yamlDefaultMapper() {
        return buildYamlMapper(builder -> builder.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
    }

    public static ObjectMapper yamlDeserializationMapper(Map<DeserializationFeature, Boolean> features) {
        return buildYamlMapper(builder -> {
            if (features != null) {
                features.forEach((feature, enabled) ->
                        builder.configure(feature, Boolean.TRUE.equals(enabled))
                );
            }
        });
    }

    public static ObjectMapper buildYamlMapper(Consumer<YAMLMapper.Builder> customizer) {
        YAMLMapper.Builder builder = YAMLMapper.builder();
        customizer.accept(builder);
        return builder.build();
    }
}
