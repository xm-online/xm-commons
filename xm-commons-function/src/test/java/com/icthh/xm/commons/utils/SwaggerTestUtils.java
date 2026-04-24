package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.Map;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFile;
import static tools.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

@UtilityClass
public class SwaggerTestUtils {

    @SneakyThrows
    public static String toYml(Object swagger) {
        ObjectMapper mapper = YAMLMapper.builder()
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(NON_EMPTY)
                                .withContentInclusion(NON_EMPTY)
                )
                .enable(SORT_PROPERTIES_ALPHABETICALLY)
                .enable(ORDER_MAP_ENTRIES_BY_KEYS)
                .build();

        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.convertValue(swagger, Map.class));
    }

    @SneakyThrows
    public static SwaggerModel readExpected(String path) {
        return YAMLMapper.builder().build().readValue(loadFile(path), SwaggerModel.class);
    }
}
