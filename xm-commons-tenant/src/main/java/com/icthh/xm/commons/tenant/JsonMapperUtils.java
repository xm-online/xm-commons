package com.icthh.xm.commons.tenant;

import java.util.function.Consumer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonMapperUtils {

    public static ObjectMapper getJsonMapperWithIgnore() {
        return buildJsonMapper(builder -> builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }


    public static ObjectMapper getDefaultJsonMapper() {
        return JsonMapper.builder().build();
    }

    public static ObjectMapper buildJsonMapper(Consumer<JsonMapper.Builder> customizer) {
        JsonMapper.Builder builder = JsonMapper.builder();
        customizer.accept(builder);
        return builder.build();
    }
}
