package com.icthh.xm.commons.lep.groovy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(LepCompilerConfiguration.class)
@ActiveProfiles("export")
@TestPropertySource(properties = "spring.application.name=testApp")
public class GroovyPrecompiledWarmupIntTest {

    private static final String TENANT = "WARMTEST";
    private static final String COMMONS_PATH = "/config/tenants/WARMTEST/testApp/lep/commons/WarmService.groovy";
    private static final String SCRIPT_PATH = "/config/tenants/WARMTEST/testApp/lep/service/Do$$around.groovy";

    @Autowired
    private ApplicationNameProvider applicationNameProvider;
    @Autowired
    private LepStorageFactory lepStorageFactory;
    @Autowired
    private LoggingWrapper loggingWrapper;
    @Autowired
    private LepPathResolver lepPathResolver;
    @Autowired
    private GroovyFileParser groovyFileParser;

    @TempDir
    Path tempDir;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(GroovyLepEngine.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(GroovyLepEngine.class)).detachAppender(logAppender);
    }

    @Test
    void warmupLoadsPrecompiledCommonsClasses() {
        List<XmLepConfigFile> leps = List.of(
            new XmLepConfigFile(COMMONS_PATH,
                "package WARMTEST.testApp.lep.commons\nclass WarmService { def hello() { return 'ok' } }\n"),
            new XmLepConfigFile(SCRIPT_PATH,
                "import WARMTEST.testApp.lep.commons.WarmService\nreturn new WarmService().hello()\n")
        );

        String compiledDir = tempDir.resolve("compiled").toAbsolutePath().toString();

        // phase A: precompile - the same way LepCompiler produces the zip content
        createFactory(compiledDir).createLepEngine(TENANT, leps);

        logAppender.list.clear();

        // phase B: fresh engine over the precompiled directory - warmup must load commons classes precompiled
        createFactory(compiledDir).createLepEngine(TENANT, leps);

        assertTrue(hasPrecompiledLog(SCRIPT_PATH),
            "script class expected to be warmed up from precompiled sources");
        assertTrue(hasPrecompiledLog(COMMONS_PATH),
            "commons class expected to be warmed up from precompiled sources");
    }

    private boolean hasPrecompiledLog(String configPath) {
        String lepPath = configPath.replace("/config/tenants/", "").replace(".groovy", "");
        return logAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains("PRECOMPILED") && message.contains(lepPath));
    }

    private GroovyLepEngineFactory createFactory(String compiledDir) {
        GroovyLepEngineFactory factory = new GroovyLepEngineFactory(
            applicationNameProvider.getAppName(),
            lepStorageFactory,
            new RecreateGroovyLepEngineOnRefresh(),
            loggingWrapper,
            lepPathResolver,
            groovyFileParser,
            Set.of(),
            true,
            true,
            compiledDir
        );
        factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
        return factory;
    }
}
