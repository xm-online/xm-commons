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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
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

    @Test
    void warmupLoadsClassesFromCompiledJar() throws Exception {
        List<XmLepConfigFile> leps = List.of(
            new XmLepConfigFile(COMMONS_PATH,
                "package WARMTEST.testApp.lep.commons\nclass WarmService { def hello() { return 'ok' } }\n"),
            new XmLepConfigFile(SCRIPT_PATH,
                "import WARMTEST.testApp.lep.commons.WarmService\nreturn new WarmService().hello()\n")
        );

        Path compiledDir = tempDir.resolve("compiled");

        // phase A: precompile into the directory, then pack it into compiled.jar like LepCompiler does
        createFactory(compiledDir.toAbsolutePath().toString()).createLepEngine(TENANT, leps);
        packToJar(compiledDir, tempDir.resolve("compiled.jar"));
        FileUtils.deleteDirectory(compiledDir.toFile());

        logAppender.list.clear();

        // phase B: no class tree on disk - warmup must load precompiled classes from compiled.jar
        createFactory(compiledDir.toAbsolutePath().toString()).createLepEngine(TENANT, leps);

        assertTrue(hasPrecompiledLog(SCRIPT_PATH),
            "script class expected to be loaded from compiled.jar");
        assertTrue(hasPrecompiledLog(COMMONS_PATH),
            "commons class expected to be loaded from compiled.jar");
    }

    private static void packToJar(Path dir, Path jar) throws Exception {
        try (
            ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jar));
            Stream<Path> files = Files.walk(dir)
        ) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    zos.putNextEntry(new ZipEntry(dir.relativize(file).toString().replace('\\', '/')));
                    zos.write(Files.readAllBytes(file));
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
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
