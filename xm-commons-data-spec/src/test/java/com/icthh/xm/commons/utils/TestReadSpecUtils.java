package com.icthh.xm.commons.utils;

import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.TestBaseSpecification;
import com.icthh.xm.commons.tenant.YamlMapperUtils;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class to read test specifications
 */
@UtilityClass
public class TestReadSpecUtils {

    @SneakyThrows
    public static TestBaseSpecification loadBaseSpecByFileName(String name) {
        ObjectMapper mapper = YamlMapperUtils.yamlDefaultMapper();
        return mapper.readValue(loadBaseSpecFileByName(name), TestBaseSpecification.class);
    }

    @SneakyThrows
    public static String loadBaseSpecFileByName(String name) {
        return loadFile("config/spec/" + name + ".yml");
    }

    @SneakyThrows
    public static String loadJsonSpecFileByName(String name) {
        return loadFile("config/spec/" + name + ".json");
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }
}
