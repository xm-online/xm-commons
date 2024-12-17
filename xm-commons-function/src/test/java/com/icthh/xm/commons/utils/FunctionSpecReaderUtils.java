package com.icthh.xm.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
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
public class FunctionSpecReaderUtils {

    @SneakyThrows
    public static FunctionApiSpecs loadFunctionApiSpecByFile(String name) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(loadFile("config/functions/" + name + ".yml"), FunctionApiSpecs.class);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }
}
