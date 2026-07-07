package com.icthh.xm.commons.lep.groovy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LepCompilerJarLayoutIntTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // LepCompiler boots its own Spring context which reads the app name from the environment
        System.setProperty("spring.application.name", "testApp");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("spring.application.name");
    }

    @Test
    void compiledClassesArePackedIntoJarPerTenant() throws IOException {
        Path inputZip = tempDir.resolve("config-export.zip");
        writeZip(inputZip, Map.of(
            "config/tenants/TEST/testApp/lep/service/Save$$around.groovy", "return 'compiled'"
        ));
        Path outputDir = Files.createDirectories(tempDir.resolve("output"));

        new LepCompiler().execute(new String[]{inputZip.toString(), outputDir.toString()});

        Path outputZip = outputDir.resolve("testApp-compiled-lep.zip");
        assertTrue(Files.exists(outputZip), "compiled lep zip expected");

        Map<String, byte[]> entries = readZip(outputZip);

        assertTrue(entries.containsKey("TEST/compiled.jar"),
            "compiled classes expected to be packed as TEST/compiled.jar");
        assertFalse(entries.keySet().stream().anyMatch(k -> k.startsWith("TEST/compiled/")),
            "no unpacked class tree expected under TEST/compiled/");
        assertTrue(entries.keySet().stream()
                .anyMatch(k -> k.startsWith("TEST/sources/") && k.endsWith("Save$$around.groovy")),
            "sources expected to stay unpacked");

        Map<String, byte[]> jarEntries = readZip(entries.get("TEST/compiled.jar"));
        assertTrue(jarEntries.keySet().stream().anyMatch(k -> k.endsWith(".class")),
            "compiled.jar expected to contain class files");
    }

    @Test
    void recompilationIntoDirtyOutputDirKeepsClassesInJar() throws IOException {
        Path inputZip = tempDir.resolve("config-export.zip");
        writeZip(inputZip, Map.of(
            "config/tenants/TEST/testApp/lep/service/Save$$around.groovy", "return 'compiled'"
        ));
        Path outputDir = Files.createDirectories(tempDir.resolve("output"));

        new LepCompiler().execute(new String[]{inputZip.toString(), outputDir.toString()});

        // simulate an interrupted previous run: compiled.jar left on disk from the first pass
        Map<String, byte[]> firstRun = readZip(outputDir.resolve("testApp-compiled-lep.zip"));
        Path leftoverJar = outputDir.resolve("TEST").resolve("compiled.jar");
        Files.createDirectories(leftoverJar.getParent());
        Files.write(leftoverJar, firstRun.get("TEST/compiled.jar"));

        // second run into the dirty output dir must not lose compiled classes
        new LepCompiler().execute(new String[]{inputZip.toString(), outputDir.toString()});

        Map<String, byte[]> entries = readZip(outputDir.resolve("testApp-compiled-lep.zip"));
        Map<String, byte[]> jarEntries = readZip(entries.get("TEST/compiled.jar"));
        assertTrue(jarEntries.keySet().stream().anyMatch(k -> k.contains("Save") && k.endsWith(".class")),
            "compiled.jar expected to still contain the lep script class after recompilation, but has: "
                + jarEntries.keySet());
    }

    private static void writeZip(Path zip, Map<String, String> files) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(file.getKey()));
                zos.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }

    private static Map<String, byte[]> readZip(Path zip) throws IOException {
        return readZip(new ZipInputStream(Files.newInputStream(zip)));
    }

    private static Map<String, byte[]> readZip(byte[] content) throws IOException {
        return readZip(new ZipInputStream(new ByteArrayInputStream(content)));
    }

    private static Map<String, byte[]> readZip(ZipInputStream zis) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (zis) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zis.readAllBytes());
                }
            }
        }
        return entries;
    }
}
