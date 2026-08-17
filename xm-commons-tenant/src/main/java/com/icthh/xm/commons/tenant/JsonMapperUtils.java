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
        return buildJsonMapper(builder -> {});
    }

    public static ObjectMapper buildJsonMapper(Consumer<JsonMapper.Builder> customizer) {
        JsonMapper.Builder builder = JsonMapper.builder()
            // Jackson 2 parity: do not fail on content after the first JSON value (Jackson 3 enables this check by default)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        customizer.accept(builder);
        return builder.build();
    }
}
