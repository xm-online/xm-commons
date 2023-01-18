package com.icthh.xm.commons.domainevent.utils;

import com.fasterxml.jackson.core.JsonFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonUtilUnitTest {

    private final JsonFactory jsonFactory = new JsonFactory();

    @Test
    public void shouldReturnIdAndTypeKey() {
        String json = "{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\"}";
        Pair<String, String> stringStringPair = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(stringStringPair);
    }

    @Test
    public void shouldReturnIdAndTypeKey_fromXmEntity() {
        String json = "{\"xmEntity\":{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\"}}";
        Pair<String, String> stringStringPair = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(stringStringPair);
    }

    @Test
    public void shouldReturnIdAndTypeKey_fromData() {
        String json = "{\"data\":{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\"}}";
        Pair<String, String> stringStringPair = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(stringStringPair);
    }

    @Test
    public void shouldReturnIdAndTypeKey_fromUpperLevel() {
        String json = "{\"xmEntity\":{\"id\":\"456\",\"typeKey\":\"XM_ENTITY_TYPE_KEY\"},\"data\":{\"id\":\"789\",\"typeKey\":\"DATA_TYPE_KEY\"},\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\"}";
        Pair<String, String> stringStringPair = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(stringStringPair);
    }

    private void assertResult(Pair<String, String> stringStringPair) {
        assertNotNull(stringStringPair);
        assertEquals("123", stringStringPair.getKey());
        assertEquals("TEST_TYPE_KEY", stringStringPair.getValue());
    }

}
