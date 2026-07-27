package com.icthh.xm.commons.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * The {@link JsonMapperUtilsUnitTest} class.
 */
public class JsonMapperUtilsUnitTest {

    public static class Dto {
        public String name;
        public String description;
    }

    @Test
    public void changeDefaultPropertyInclusionSkipsNulls() {
        ObjectMapper mapper = JsonMapperUtils.buildJsonMapper(builder -> builder
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)));

        Dto dto = new Dto();
        dto.name = "tenant";

        assertEquals("{\"name\":\"tenant\"}", mapper.writeValueAsString(dto));
    }

    @Test
    public void disableFailOnUnknownProperties() {
        ObjectMapper mapper = JsonMapperUtils.buildJsonMapper(builder -> builder
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        Dto dto = mapper.readValue("{\"name\":\"tenant\",\"unknown\":\"ignored\"}", Dto.class);

        assertEquals("tenant", dto.name);
        assertNull(dto.description);
    }

    @Test
    public void enableIndentOutput() {
        ObjectMapper mapper = JsonMapperUtils.buildJsonMapper(builder -> builder
            .enable(SerializationFeature.INDENT_OUTPUT));

        Dto dto = new Dto();
        dto.name = "tenant";

        assertTrue(mapper.writeValueAsString(dto).contains("\n"));
    }

    @Test
    public void combinesInclusionEnableAndDisable() {
        ObjectMapper mapper = JsonMapperUtils.buildJsonMapper(builder -> builder
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT));

        Dto dto = mapper.readValue("{\"name\":\"tenant\",\"unknown\":\"ignored\"}", Dto.class);
        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"name\" : \"tenant\""));
        assertFalse(json.contains("description"));
    }

    @Test
    public void noCustomizationBuildsDefaultMapper() {
        ObjectMapper mapper = JsonMapperUtils.buildJsonMapper(builder -> { });

        Dto dto = new Dto();
        dto.name = "tenant";

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\"name\":\"tenant\""));
        assertTrue(json.contains("\"description\":null"));
    }

}
