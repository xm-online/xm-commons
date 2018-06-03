package com.icthh.xm.commons.permission.service.translator;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SpelObjectTranslator {

    private final String variableName;
    private final Object object;
    private final ObjectMapper objectMapper;


    private final Pattern VARIABLE_REGEXP = Pattern.compile("#(varname[\\.\\w\\[\\]]+)");



    @SneakyThrows
    public String translate(String spel, Supplier<Object> objectRepository) {
        if (!spel.contains("#" + variableName + ".")) {
            return spel;
        }

        Matcher m = VARIABLE_REGEXP.matcher(spel);
        while (m.find()) {
            String json = objectMapper.writeValueAsString(objectRepository.get());
            DocumentContext document = JsonPath.using(defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS)).parse(json);
            document.read("$" + m.group(1));
        }​​​​​​​​​​​​​​​​​​​​​​

        return null;
    }
}
