package com.icthh.xm.commons.domainevent.config;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MappingOperationConfigurationUnitTest {

    public static final String TENANT = "TEST";
    public static final String ENTITY_APP_NAME = "entity";
    public static final String CONFIG_PATH_DEFAULT = "/config/tenants/" + TENANT + "/mapping-operation-config.yml";

    private final MappingOperationConfiguration mappingOperationConfiguration= new MappingOperationConfiguration();

    @Before
    public void init() {
        mappingOperationConfiguration.onRefresh(CONFIG_PATH_DEFAULT, readConfigFile());
    }

    @Test
    public void testGetConfigValue() {
        String url = ENTITY_APP_NAME + "/api/xm-entities/123/states/FINISH";
        String operationMapping = mappingOperationConfiguration.getOperationMapping(TENANT, ENTITY_APP_NAME, "PUT", url);
        assertNotNull(operationMapping);
        assertEquals("statechange id: 123 to: FINISH", operationMapping);

    }

    @Test
    public void testGetDefaultValue() {
        String url = ENTITY_APP_NAME + "/api/xm-entities/123/states/FINISH";
        String operationMapping = mappingOperationConfiguration.getOperationMapping(TENANT, ENTITY_APP_NAME, "DELETE", url);
        assertNotNull(operationMapping);
        assertEquals("deleted", operationMapping);
    }

    @SneakyThrows
    private String readConfigFile() {
        InputStream cfgInputStream = new ClassPathResource("/config/tenants/TEST/mapping-operation-config.yml").getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }
}
