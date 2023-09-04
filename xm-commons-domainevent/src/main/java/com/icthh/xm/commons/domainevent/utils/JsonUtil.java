package com.icthh.xm.commons.domainevent.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@UtilityClass
public class JsonUtil {

    private static final String ID_FIELD_NAME = "id";
    private static final String TYPE_KEY_FIELD_NAME = "typeKey";
    private static final String NAME_FIELD_NAME = "name";

    private static final int FIELD_COUNT = 3;

    public static final int ID = 0;
    public static final int TYPE_KEY = 1;
    public static final int NAME = 2;

    public static String[] extractIdAndTypeKey(JsonFactory jsonFactory, String json) {
        String[] values = new String[FIELD_COUNT];
        if (json == null) {
            return values;
        }
        try (JsonParser jParser = jsonFactory.createParser(json)) {
            while (jParser.nextToken() != JsonToken.END_OBJECT && !jParser.isClosed()) {
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

                if (NAME_FIELD_NAME.equals(fieldName)) {
                    jParser.nextToken();
                    values[NAME] = jParser.getText();
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
        return values;
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
                continue;
            }

            if (result[NAME] == null && NAME_FIELD_NAME.equals(innerFieldName)) {
                jParser.nextToken();
                result[NAME] = jParser.getText();
            }
        }
    }

    public static final class AggregateMapper {
        public static String getTypeKey(String[] values) {
            return values[TYPE_KEY];
        }

        public static String getId(String[] values) {
            return values[ID];
        }

        public static String getName(String[] values) {
            return values[NAME];
        }
    }
}
