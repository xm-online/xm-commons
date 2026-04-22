package com.icthh.xm.commons.lep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.lep.groovy.LepCompiler;
import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {LepCompilerConfiguration.class})
@ActiveProfiles("export")
public class LepCompilerIntTest {

    @Autowired
    private ApplicationNameProvider applicationNameProvider;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path outputDir;
    private String appName;

    private LepCompiler lepCompiler;

    @Before
    public void setUp() throws IOException {
        outputDir = tempFolder.newFolder("output").toPath();
        appName = applicationNameProvider.getAppName();
        lepCompiler = new LepCompiler();
    }

    @Test
    public void testCompileSimpleScript() throws IOException {
        Path inputZip = createTestZip(Map.of(
            "config/tenants/TEST/" + appName + "/lep/service/Save.groovy",
            "return 'compiled'"
        ));

        lepCompiler.execute(new String[]{inputZip.toString(), outputDir.toString()});

        Path expectedZip = outputDir.resolve(appName + "-compiled-lep.zip");
        assertTrue(Files.exists(expectedZip));

        Map<String, byte[]> zipContents = readZipBytes(expectedZip);

        assertTrue(zipContents.keySet().stream().anyMatch(k -> k.contains("/sources/") && k.endsWith("Save.groovy")));
        assertTrue(zipContents.keySet().stream().anyMatch(k -> k.contains("/compiled/") && k.endsWith(".class")));
    }

    @Test
    public void testCompileMultipleTenants() throws IOException {
        Path inputZip = createTestZip(Map.of(
            "config/tenants/TENANT_A/" + appName + "/lep/service/ScriptA.groovy", "return 'A'",
            "config/tenants/TENANT_B/" + appName + "/lep/service/ScriptB.groovy", "return 'B'"
        ));

        lepCompiler.execute(new String[]{inputZip.toString(), outputDir.toString()});

        Path expectedZip = outputDir.resolve(appName + "-compiled-lep.zip");
        Map<String, byte[]> zipContents = readZipBytes(expectedZip);

        assertTrue(zipContents.keySet().stream().anyMatch(k -> k.startsWith("TENANT_A/sources/") && k.endsWith("ScriptA.groovy")));
        assertTrue(zipContents.keySet().stream().anyMatch(k -> k.startsWith("TENANT_B/sources/") && k.endsWith("ScriptB.groovy")));
        assertTrue(zipContents.keySet().stream().anyMatch(k -> k.startsWith("TENANT_A/compiled/") && k.endsWith(".class")));
        assertTrue(zipContents.keySet().stream().anyMatch(k -> k.startsWith("TENANT_B/compiled/") && k.endsWith(".class")));
    }

    @Test
    public void testCleanupAfterZip() throws IOException {
        Path inputZip = createTestZip(Map.of(
            "config/tenants/TEST/" + appName + "/lep/service/Save.groovy", "return 'ok'"
        ));

        lepCompiler.execute(new String[]{inputZip.toString(), outputDir.toString()});

        assertFalse(Files.exists(outputDir.resolve("TEST")));
        assertTrue(Files.exists(outputDir.resolve(appName + "-compiled-lep.zip")));
    }

    @Test
    public void testOutputZipNotIncludedInsideItself() throws IOException {
        Path inputZip = createTestZip(Map.of(
            "config/tenants/TEST/" + appName + "/lep/service/Save.groovy", "return 'ok'"
        ));

        lepCompiler.execute(new String[]{inputZip.toString(), outputDir.toString()});

        Map<String, byte[]> zipContents = readZipBytes(outputDir.resolve(appName + "-compiled-lep.zip"));

        assertTrue(zipContents.keySet().stream().noneMatch(k -> k.endsWith(".zip")));
    }

    @Test
    public void testScriptWithCommonsImport() throws IOException {
        String commonsClass = """
            class Utils {
                static String test { return test }
            }""";

        String mainScript = "return text";

        Path inputZip = createTestZip(Map.of(
            "config/tenants/TEST/" + appName + "/lep/commons/Utils.groovy", commonsClass,
            "config/tenants/TEST/" + appName + "/lep/service/TestClass.groovy", mainScript
        ));

        lepCompiler.execute(new String[]{inputZip.toString(), outputDir.toString()});

        Map<String, byte[]> zipContents = readZipBytes(outputDir.resolve(appName + "-compiled-lep.zip"));

        long classCount = zipContents.keySet().stream()
            .filter(k -> k.startsWith("TEST/compiled/") && k.endsWith(".class"))
            .count();
        assertTrue(classCount >= 2);
    }

    private Path createTestZip(Map<String, String> entries) throws IOException {
        Path zipPath = tempFolder.newFile("input-" + System.nanoTime() + ".zip").toPath();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    private static Map<String, byte[]> readZipBytes(Path zipPath) throws IOException {
        Map<String, byte[]> contents = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    contents.put(entry.getName(), zis.readAllBytes());
                }
            }
        }
        return contents;
    }
}
