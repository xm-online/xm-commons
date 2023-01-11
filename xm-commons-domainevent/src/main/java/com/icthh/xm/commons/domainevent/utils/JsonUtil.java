package com.icthh.xm.commons.domainevent.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

@Slf4j
@UtilityClass
public class JsonUtil {

    public static Pair<String, String> extractIdAndTypeKey(JsonFactory jsonFactory, String json) {
        String id = null;
        String typeKey = null;

        try (JsonParser jParser = jsonFactory.createParser(json)) {
            while (jParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jParser.getCurrentName();
                if ("id".equals(fieldName)) {
                    jParser.nextToken();
                    id = jParser.getText();
                }

                if ("typeKey".equals(fieldName)) {
                    jParser.nextToken();
                    typeKey = jParser.getText();
                }

                if ("xmEntity".equals(fieldName)) {
                    jParser.nextToken();
                    while (jParser.nextToken() != JsonToken.END_OBJECT) {
                        String innerFieldName = jParser.getCurrentName();
                        if (id == null && "id".equals(innerFieldName)) {
                            jParser.nextToken();
                            id = jParser.getText();
                        }

                        if (typeKey == null && "typeKey".equals(innerFieldName)) {
                            jParser.nextToken();
                            typeKey = jParser.getText();
                        }
                    }
                }

                if ("data".equals(fieldName)) {
                    jParser.nextToken();
                    while (jParser.nextToken() != JsonToken.END_OBJECT) {
                        String innerFieldName = jParser.getCurrentName();
                        if (id == null && "id".equals(innerFieldName)) {
                            jParser.nextToken();
                            id = jParser.getText();
                        }

                        if (typeKey == null && "typeKey".equals(innerFieldName)) {
                            jParser.nextToken();
                            typeKey = jParser.getText();
                        }
                    }
                }
            }

        } catch (IOException e) {
            log.trace("");
        }
        return Pair.of(id, typeKey);
    }
}
