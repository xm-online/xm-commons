package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.spring.LepRefreshService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmLepPrecompiledConfigLoaderUnitTest {

    private static final String CONFIG_KEY = "/config/tenants/TEST/testApp/lep.yml";
    private static final String SCRIPT_PATH = "TEST/sources/config/tenants/TEST/testApp/lep/service/Script.groovy";

    @TempDir
    Path tempDir;

    private final AtomicInteger refreshCount = new AtomicInteger();

    private final LepRefreshService refreshService = new LepRefreshService() {
        @Override
        public Future<?> refreshEngines(Set<String> tenantsToUpdate,
                                        Map<String, Map<String, XmLepConfigFile>> scriptsByTenant,
                                        boolean isInit) {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public void initOrRefresh(Set<String> tenantsToUpdate,
                                  Map<String, Map<String, XmLepConfigFile>> scriptsByTenant,
                                  String pathToPrecompiledLep) {
            refreshCount.incrementAndGet();
        }
    };

    @Test
    void skipRefreshWhenZipContentUnchanged() throws Exception {
        Path zip = tempDir.resolve("compiled-lep.zip");
        writeZip(zip, Map.of(SCRIPT_PATH, "return 'ok'"));

        Path workDir = tempDir.resolve("work");
        XmLepPrecompiledConfigLoader loader =
            new XmLepPrecompiledConfigLoader(refreshService, "testApp", workDir.toString());

        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"" + zip + "\"\n# 1");
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(1, refreshCount.get());

        // lep.yml touched (comment changed) but zip content is the same -> engines must NOT be refreshed
        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"" + zip + "\"\n# 2");
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(1, refreshCount.get());

        // zip content changed -> engines must be refreshed
        writeZip(zip, Map.of(SCRIPT_PATH, "return 'changed'"));
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(2, refreshCount.get());
    }

    @Test
    void refreshWhenSamePathHasDifferentZipContent() throws Exception {
        Path zip = tempDir.resolve("compiled-lep.zip");
        Path workDir = tempDir.resolve("work");
        XmLepPrecompiledConfigLoader loader =
            new XmLepPrecompiledConfigLoader(refreshService, "testApp", workDir.toString());
        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"" + zip + "\"");

        // the zip PATH never changes in this test, only the bytes inside it do
        writeZip(zip, Map.of(SCRIPT_PATH, "return 'v1'"));
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(1, refreshCount.get());

        writeZip(zip, Map.of(SCRIPT_PATH, "return 'v2'"));
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(2, refreshCount.get());

        writeZip(zip, Map.of(SCRIPT_PATH, "return 'v3'"));
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(3, refreshCount.get());

        // unchanged bytes -> skipped
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(3, refreshCount.get());
    }

    @Test
    void refreshWhenZipPathSwitchesBackWithSameContent() throws Exception {
        Path zipA = tempDir.resolve("a.zip");
        writeZip(zipA, Map.of(SCRIPT_PATH, "return 'a'"));
        Path zipB = tempDir.resolve("b.zip");
        writeZip(zipB, Map.of(SCRIPT_PATH, "return 'b'"));

        Path workDir = tempDir.resolve("work");
        XmLepPrecompiledConfigLoader loader =
            new XmLepPrecompiledConfigLoader(refreshService, "testApp", workDir.toString());

        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"" + zipA + "\"");
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(1, refreshCount.get());

        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"" + zipB + "\"");
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(2, refreshCount.get());

        // switch back to zip A: content of A is unchanged, but engines were built from B,
        // so the refresh must NOT be skipped
        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"" + zipA + "\"");
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(3, refreshCount.get());

        // stable state again -> skipped
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(3, refreshCount.get());
    }

    @Test
    void refreshWhenTenantConfigRemovedAndReadded() throws Exception {
        Path zip = tempDir.resolve("compiled-lep.zip");
        writeZip(zip, Map.of(SCRIPT_PATH, "return 'ok'"));

        Path workDir = tempDir.resolve("work");
        XmLepPrecompiledConfigLoader loader =
            new XmLepPrecompiledConfigLoader(refreshService, "testApp", workDir.toString());

        String config = "pathToPrecompiledLep: \"" + zip + "\"";
        loader.onRefresh(CONFIG_KEY, config);
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(1, refreshCount.get());

        // tenant precompiled config removed -> applied state must be forgotten
        loader.onRefresh(CONFIG_KEY, "pathToPrecompiledLep: \"\"");
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(1, refreshCount.get());

        // re-added with the same zip -> engines must be refreshed
        loader.onRefresh(CONFIG_KEY, config);
        loader.refreshFinished(List.of(CONFIG_KEY));
        assertEquals(2, refreshCount.get());
    }

    private static void writeZip(Path zip, Map<String, String> files) throws Exception {
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(file.getKey()));
                zos.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }
}
