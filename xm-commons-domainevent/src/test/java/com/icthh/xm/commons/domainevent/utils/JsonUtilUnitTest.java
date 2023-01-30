package com.icthh.xm.commons.domainevent.utils;

import com.fasterxml.jackson.core.JsonFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonUtilUnitTest {

    private final JsonFactory jsonFactory = new JsonFactory();

    @Test
    public void shouldReturnIdAndTypeKey() {
        String json = "{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\",\"name\":\"TEST_NAME\"}";
        String[] values = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(values);
    }

    @Test
    public void shouldReturnIdAndTypeKey_fromXmEntity() {
        String json = "{\"xmEntity\":{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\",\"name\":\"TEST_NAME\"}}";
        String[] values = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(values);
    }

    @Test
    public void shouldReturnIdAndTypeKey_fromData() {
        String json = "{\"data\":{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\",\"name\":\"TEST_NAME\"}}";
        String[] values = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(values);
    }

    @Test
    public void shouldReturnIdAndTypeKey_fromUpperLevel() {
        String json = "{\"xmEntity\":{\"id\":\"456\",\"typeKey\":\"XM_ENTITY_TYPE_KEY\",\"name\":\"XM_ENTITY_NAME\"},\"name\":\"TEST_NAME\",\"data\":{\"id\":\"789\",\"typeKey\":\"DATA_TYPE_KEY\",\"name\":\"DATA_NAME\"},\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\"}";
        String[] values = JsonUtil.extractIdAndTypeKey(jsonFactory, json);

        assertResult(values);
    }

    private void assertResult(String[]  values) {
        assertNotNull(values);
        assertEquals("123", values[JsonUtil.ID]);
        assertEquals("TEST_TYPE_KEY", values[JsonUtil.TYPE_KEY]);
        assertEquals("TEST_NAME", values[JsonUtil.NAME]);
    }

}
