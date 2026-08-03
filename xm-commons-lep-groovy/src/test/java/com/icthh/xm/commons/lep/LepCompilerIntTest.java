package com.icthh.xm.commons.lep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.icthh.xm.commons.lep.groovy.LepCompiler;
import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig(LepCompilerConfiguration.class)
@ActiveProfiles("export")
public class LepCompilerIntTest {

    @Autowired
    private ApplicationNameProvider applicationNameProvider;

    @TempDir
    public Path tempFolder;

    private Path outputDir;
    private String appName;

    private LepCompiler lepCompiler;

    @BeforeEach
    public void setUp() throws IOException {
        outputDir = Files.createDirectories(tempFolder.resolve("output"));
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
        assertTrue(hasCompiledClasses(zipContents, "TEST"));
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
        assertTrue(hasCompiledClasses(zipContents, "TENANT_A"));
        assertTrue(hasCompiledClasses(zipContents, "TENANT_B"));
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

        long classCount = compiledClasses(zipContents, "TEST").stream()
            .filter(k -> k.endsWith(".class"))
            .count();
        assertTrue(classCount >= 2);
    }

    private Path createTestZip(Map<String, String> entries) throws IOException {
        Path zipPath = tempFolder.resolve("input-" + System.nanoTime() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    /** compiled classes are packed per tenant into a nested compiled.jar, see LepCompilerJarLayoutIntTest */
    private static boolean hasCompiledClasses(Map<String, byte[]> zipContents, String tenant) throws IOException {
        return compiledClasses(zipContents, tenant).stream().anyMatch(k -> k.endsWith(".class"));
    }

    private static Set<String> compiledClasses(Map<String, byte[]> zipContents, String tenant) throws IOException {
        byte[] jar = zipContents.get(tenant + "/compiled.jar");
        return jar == null ? Set.of() : readZipBytes(new ZipInputStream(new ByteArrayInputStream(jar))).keySet();
    }

    private static Map<String, byte[]> readZipBytes(Path zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            return readZipBytes(zis);
        }
    }

    private static Map<String, byte[]> readZipBytes(ZipInputStream zis) throws IOException {
        Map<String, byte[]> contents = new HashMap<>();
        try (zis) {
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
