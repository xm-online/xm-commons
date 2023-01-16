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

    public static Pair<String, String> extractIdAndTypeKey(JsonFactory jsonFactory, String json) {
        String id = null;
        String typeKey = null;

        try (JsonParser jParser = jsonFactory.createParser(json)) {
            while (jParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jParser.getCurrentName();
                if (ID_FIELD_NAME.equals(fieldName)) {
                    jParser.nextToken();
                    id = jParser.getText();
                }

                if (TYPE_KEY_FIELD_NAME.equals(fieldName)) {
                    jParser.nextToken();
                    typeKey = jParser.getText();
                }

                if ("xmEntity".equals(fieldName)) {
                    Pair<String, String> innerPair = extractFromInnerLevel(jParser, id, typeKey);
                    id = innerPair.getKey();
                    typeKey = innerPair.getValue();
                }

                if ("data".equals(fieldName)) {
                    Pair<String, String> innerPair = extractFromInnerLevel(jParser, id, typeKey);
                    id = innerPair.getKey();
                    typeKey = innerPair.getValue();
                }
            }
        } catch (IOException e) {
            log.trace(e.getMessage(), e);
        }
        return Pair.of(id, typeKey);
    }

    @SneakyThrows
    private Pair<String, String> extractFromInnerLevel(JsonParser jParser, String currentId, String currentTypeKey) {
        jParser.nextToken();
        while (jParser.nextToken() != JsonToken.END_OBJECT) {
            String innerFieldName = jParser.getCurrentName();
            if (currentId == null && "id".equals(innerFieldName)) {
                jParser.nextToken();
                currentId = jParser.getText();
            }

            if (currentTypeKey == null && "typeKey".equals(innerFieldName)) {
                jParser.nextToken();
                currentTypeKey = jParser.getText();
            }
        }

        return Pair.of(currentId, currentTypeKey);
    }
}
