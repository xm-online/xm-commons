package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code application.lep.clear-map-constructor-type-annotations} kill switch, end to end through
 * the engine factory. Kept apart from the ordinary engine tests because asserting on it means reaching
 * into groovy's internals.
 */
@SpringJUnitConfig(LepCompilerConfiguration.class)
@ActiveProfiles("export")
@TestPropertySource(properties = "spring.application.name=test")
public class GroovyMapConstructorTypeAnnotationsIntTest {

    private static final String TENANT = "TEST";
    private static final String MODEL_CLASS = "MapConstructorModel";

    @Autowired
    private ApplicationNameProvider applicationNameProvider;
    @Autowired
    private LepStorageFactory lepStorageFactory;
    @Autowired
    private GroovyEngineCreationStrategy groovyEngineCreationStrategy;
    @Autowired
    private LoggingWrapper loggingWrapper;
    @Autowired
    private LepPathResolver lepPathResolver;
    @Autowired
    private GroovyFileParser groovyFileParser;

    @TempDir
    Path targetDir;

    @AfterEach
    void leaveTheSharedNodesEmpty() {
        GroovyMapConstructorTypeAnnotations.clear();
    }

    @Test
    void clearsWhatTheCompilationLeftBehindByDefault() {
        GroovyMapConstructorTypeAnnotations.clear();

        createEngine(true);

        assertTrue(compiledModelExists(), "the lep under test was never compiled");
        assertEquals(0, GroovyMapConstructorTypeAnnotations.accumulatedCount(),
            "engine creation must not leave anything on the shared groovy node");
    }

    @Test
    void leavesTheAccumulationAloneWhenTheKillSwitchIsOff() {
        GroovyMapConstructorTypeAnnotations.clear();

        createEngine(false);

        assertTrue(compiledModelExists(), "the lep under test was never compiled");
        assertTrue(GroovyMapConstructorTypeAnnotations.accumulatedCount() > 0,
            "with clear-map-constructor-type-annotations=false groovy's accumulation is left alone");
    }

    private void createEngine(boolean clearMapConstructorTypeAnnotations) {
        String appName = applicationNameProvider.getAppName();
        GroovyLepEngineFactory factory = new GroovyLepEngineFactory(
            appName,
            lepStorageFactory,
            groovyEngineCreationStrategy,
            loggingWrapper,
            lepPathResolver,
            groovyFileParser,
            Set.of(),
            true,
            true,
            targetDir.toAbsolutePath().toString(),
            clearMapConstructorTypeAnnotations
        );
        factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
        factory.createLepEngine(TENANT, List.of(mapConstructorLep(appName)));
    }

    /** the class name has to differ from the lep file name: a script cannot also declare its own class */
    private static XmLepConfigFile mapConstructorLep(String appName) {
        return new XmLepConfigFile(
            "/config/tenants/" + TENANT + "/" + appName + "/lep/service/MapConstructorLep.groovy",
            """
                import groovy.transform.MapConstructor
                @MapConstructor
                class %s {
                    Integer repeatCount = null
                    String  interval    = null
                }
                return new %s(repeatCount: 1)
                """.formatted(MODEL_CLASS, MODEL_CLASS));
    }

    private boolean compiledModelExists() {
        return FileUtils.listFiles(targetDir.toFile(), new String[]{"class"}, true)
            .stream()
            .anyMatch(path -> path.getAbsolutePath().endsWith(MODEL_CLASS + ".class"));
    }
}
