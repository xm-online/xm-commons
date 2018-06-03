package com.icthh.xm.commons.permission.service.translator;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.JsonPath.using;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.apache.commons.lang3.StringUtils.replaceAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SpelObjectTranslator {

    private final String variableName;
    private final ObjectMapper objectMapper;
    private final Pattern VARIABLE_REGEXP;

    public SpelObjectTranslator(String variableName, ObjectMapper objectMapper) {
        this.variableName = variableName;
        this.objectMapper = objectMapper;
        VARIABLE_REGEXP = Pattern.compile("#(" + variableName + "[\\.\\w\\[\\]]+)");
    }



    @SneakyThrows
    public String translate(String spel, Supplier<Object> objectRepository) {
        if (!spel.contains("#" + variableName + ".")) {
            return spel;
        }

        Matcher m = VARIABLE_REGEXP.matcher(spel);
        while (m.find()) {
            String json = objectMapper.writeValueAsString(objectRepository.get());
            DocumentContext document = using(defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS)).parse(json);
            String variable = m.group(1);
            Object value = document.read("$" + variable);
            spel = replaceWithQuotes(spel, "#" + variable, value);
        }

        return null;
    }

    private String replaceWithQuotes(String jpql, String search, Object replace) {
        String replaceStr = toString(replace);
        if (StringUtils.isNotBlank(replaceStr)) {
            return replaceAll(replaceAll(jpql, "'" + search + "'", replaceStr), search, replaceStr);
        }
        return jpql;
    }

    private String toString(Object obj) {
        String string = String.valueOf(obj);
        return obj instanceof Number ? string : "'" + string + "'" ;
    }

}
