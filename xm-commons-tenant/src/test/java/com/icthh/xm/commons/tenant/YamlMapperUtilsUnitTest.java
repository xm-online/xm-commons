package com.icthh.xm.commons.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;

/**
 * The {@link YamlMapperUtilsUnitTest} class.
 */
public class YamlMapperUtilsUnitTest {

    public static class Dto {
        public String name;
        public String description;
    }

    @Test
    public void changeDefaultPropertyInclusionSkipsNulls() {
        ObjectMapper mapper = YamlMapperUtils.buildYamlMapper(builder -> builder
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)));

        Dto dto = new Dto();
        dto.name = "tenant";

        String yaml = mapper.writeValueAsString(dto);

        assertTrue(yaml.contains("name: \"tenant\""));
        assertFalse(yaml.contains("description"));
    }

    @Test
    public void disableFailOnUnknownProperties() {
        ObjectMapper mapper = YamlMapperUtils.buildYamlMapper(builder -> builder
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        Dto dto = mapper.readValue("name: tenant\nunknown: ignored\n", Dto.class);

        assertEquals("tenant", dto.name);
        assertNull(dto.description);
    }

    @Test
    public void disableSortPropertiesAlphabetically() {
        ObjectMapper mapper = YamlMapperUtils.buildYamlMapper(builder -> builder
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));

        Dto dto = new Dto();
        dto.name = "tenant";
        dto.description = "desc";

        String yaml = mapper.writeValueAsString(dto);

        assertTrue(yaml.indexOf("name:") < yaml.indexOf("description:"));
    }

    @Test
    public void combinesInclusionAndDisable() {
        ObjectMapper mapper = YamlMapperUtils.buildYamlMapper(builder -> builder
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        Dto dto = mapper.readValue("name: tenant\nunknown: ignored\n", Dto.class);
        String yaml = mapper.writeValueAsString(dto);

        assertTrue(yaml.contains("name: \"tenant\""));
        assertFalse(yaml.contains("description"));
    }

    @Test
    public void noCustomizationBuildsDefaultMapper() {
        ObjectMapper mapper = YamlMapperUtils.buildYamlMapper(builder -> { });

        Dto dto = new Dto();
        dto.name = "tenant";

        String yaml = mapper.writeValueAsString(dto);

        assertTrue(yaml.contains("name: \"tenant\""));
        assertTrue(yaml.contains("description: null"));
    }

}
