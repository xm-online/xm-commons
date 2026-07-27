package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroovyFileParserConfigIntTest {

    @SpringJUnitConfig(LepCompilerConfiguration.class)
    @ActiveProfiles("export")
    @TestPropertySource(properties = "spring.application.name=testApp")
    static class DefaultCacheSizeTest {

        @Autowired
        private GroovyFileParser groovyFileParser;

        @Test
        void metadataCacheSizeFallsBackToDefault() {
            assertEquals(GroovyFileParser.DEFAULT_METADATA_CACHE_MAX_SIZE, groovyFileParser.getMetadataCacheMaxSize());
        }
    }

    @SpringJUnitConfig(LepCompilerConfiguration.class)
    @ActiveProfiles("export")
    @TestPropertySource(properties = {
        "spring.application.name=testApp",
        "application.lep.file-metadata-cache-size=17"
    })
    static class ConfiguredCacheSizeTest {

        @Autowired
        private GroovyFileParser groovyFileParser;

        @Test
        void metadataCacheSizeIsTakenFromConfiguration() {
            assertEquals(17, groovyFileParser.getMetadataCacheMaxSize());
        }
    }
}
