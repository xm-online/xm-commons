package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.GroovyEngineCreationStrategy;
import com.icthh.xm.commons.lep.groovy.GroovyFileParser;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineFactory;
import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {LepCompilerConfiguration.class})
@ActiveProfiles("export")
@TestPropertySource(properties = {
    "spring.application.name=test"
})
public class GroovyLepEngineIntTest {

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

    private Path targetDir;
    private String appName;

    @Before
    public void setUp() throws IOException {
        targetDir = Files.createTempDirectory("lep-compiled-test");
        appName = applicationNameProvider.getAppName();
    }

    @After
    @SneakyThrows
    public void tearDown() {
        FileUtils.deleteDirectory(targetDir.toFile());
    }

    @Test
    public void shouldWriteCompiledClassesToTargetDirectory() {
        String className = "Save";
        createEngineForTenant("TEST", List.of(
            new XmLepConfigFile("/config/tenants/TEST/" + appName + "/lep/service/" + className + ".groovy", "return 'compiled'")
        ));

        assertTrue(matchCompiledClassByName(className));
    }

    @Test
    public void shouldCompileMultipleScripts() {
        String saveName = "Save";
        String deleteName = "Delete";
        createEngineForTenant("TEST", List.of(
            new XmLepConfigFile("/config/tenants/TEST/" + appName + "/lep/service/" + saveName + ".groovy", "return 'save'"),
            new XmLepConfigFile("/config/tenants/TEST/" + appName + "/lep/service/" + deleteName + ".groovy", "return 'delete'")
        ));

        assertTrue(matchCompiledClassByName(saveName));
        assertTrue(matchCompiledClassByName(deleteName));
    }

    private void createEngineForTenant(String tenant, List<XmLepConfigFile> leps) {
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
            targetDir.toAbsolutePath().toString()
        );
        factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
        factory.createLepEngine(tenant, leps);
    }

    private boolean matchCompiledClassByName(String name) {
        return FileUtils.listFiles(targetDir.toFile(), new String[]{"class"}, true)
            .stream()
            .anyMatch(path -> path.getAbsolutePath().endsWith(name + ".class"));
    }
}
