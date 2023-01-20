package com.icthh.xm.commons.domainevent.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

@Slf4j
@UtilityClass
public class JsonUtil {

    private static final String ID_FIELD_NAME = "id";
    private static final String TYPE_KEY_FIELD_NAME = "typeKey";

    private static final int ID = 0;
    private static final int TYPE_KEY = 1;

    public static Pair<String, String> extractIdAndTypeKey(JsonFactory jsonFactory, String json) {
        String[] values = new String[2];
        try (JsonParser jParser = jsonFactory.createParser(json)) {
            while (jParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jParser.getCurrentName();

                if (fieldName == null) {
                    continue;
                }

                if (ID_FIELD_NAME.equals(fieldName)) {
                    jParser.nextToken();
                    values[ID] = jParser.getText();
                    continue;
                }

                if (TYPE_KEY_FIELD_NAME.equals(fieldName)) {
                    jParser.nextToken();
                    values[TYPE_KEY] = jParser.getText();
                    continue;
                }

                if ("xmEntity".equals(fieldName)) {
                    extractFromInnerLevel(jParser, values);
                }

                if ("data".equals(fieldName)) {
                    extractFromInnerLevel(jParser, values);
                }
            }
        } catch (IOException e) {
            log.trace(e.getMessage(), e);
        }
        return Pair.of(values[ID], values[TYPE_KEY]);
    }

    @SneakyThrows
    private void extractFromInnerLevel(JsonParser jParser, String[] result) {
        jParser.nextToken();
        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String innerFieldName = jParser.getCurrentName();

            if (result[ID] == null && ID_FIELD_NAME.equals(innerFieldName)) {
                jParser.nextToken();
                result[ID] = jParser.getText();
                continue;
            }

            if (result[TYPE_KEY] == null && TYPE_KEY_FIELD_NAME.equals(innerFieldName)) {
                jParser.nextToken();
                result[TYPE_KEY] = jParser.getText();
            }
        }
    }
}
