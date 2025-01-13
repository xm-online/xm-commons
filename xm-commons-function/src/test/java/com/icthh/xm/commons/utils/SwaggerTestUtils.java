package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFile;

@UtilityClass
public class SwaggerTestUtils {

    @SneakyThrows
    public static String toYml(Object swagger) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(NON_NULL)
            .setSerializationInclusion(NON_EMPTY)
            .enable(SORT_PROPERTIES_ALPHABETICALLY)
            .enable(ORDER_MAP_ENTRIES_BY_KEYS);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.convertValue(swagger, Map.class));
    }

    @SneakyThrows
    public static SwaggerModel readExpected(String path) {
        return new ObjectMapper(new YAMLFactory()).readValue(loadFile(path), SwaggerModel.class);
    }
}
