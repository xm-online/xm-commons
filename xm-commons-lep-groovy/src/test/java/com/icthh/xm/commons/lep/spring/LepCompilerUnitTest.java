//package com.icthh.xm.commons.lep.groovy;
//
//import com.icthh.xm.commons.config.client.service.TenantAliasService;
//import com.icthh.xm.commons.lep.api.XmLepConfigFile;
//import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Path;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//import java.util.zip.ZipOutputStream;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.api.io.TempDir;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.context.ConfigurableApplicationContext;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class LepCompilerUnitTest {
//
//    private static final String APP_NAME = "testapp";
//
//    @Mock
//    private ConfigurableApplicationContext applicationContext;
//
//    @Mock
//    private ApplicationNameProvider applicationNameProvider;
//
//    @Mock
//    private TenantAliasService tenantAliasService;
//
//    @TempDir
//    Path tempDir;
//
//    @Test
//    void execute_singleTenantLep_createsOutputZipWithSources() throws IOException {
//        String inputZip = createInputZip(Map.of(
//            "config/tenants/MAIN/testapp/lep/Script.groovy", "println 'hello'"
//        ));
//
//        Path workDir = tempDir.resolve("work");
//        workDir.toFile().mkdirs();
//        String outputZip = LepCompiler.resolveOutputPath(workDir.toString(), APP_NAME);
//
//        runExecute(inputZip, workDir.toString());
//
//        assertTrue(new File(outputZip).exists());
//        Map<String, String> entries = readZipEntries(outputZip);
//        assertFalse(entries.isEmpty());
//    }
//
//    @Test
//    void execute_multipleTenants_allProcessed() throws IOException {
//        String inputZip = createInputZip(Map.of(
//            "config/tenants/TENANT1/testapp/lep/A.groovy", "println 'a'",
//            "config/tenants/TENANT2/testapp/lep/B.groovy", "println 'b'"
//        ));
//
//        Path workDir = tempDir.resolve("work");
//        workDir.toFile().mkdirs();
//        String outputZip = LepCompiler.resolveOutputPath(workDir.toString(), APP_NAME);
//
//        runExecute(inputZip, workDir.toString());
//
//        assertTrue(new File(outputZip).exists());
//        Map<String, String> entries = readZipEntries(outputZip);
//        assertFalse(entries.isEmpty());
//    }
//
//    @Test
//    void execute_aliasConfigInZip_callsOnRefresh() throws IOException {
//        String aliasPath = TenantAliasService.TENANT_ALIAS_CONFIG;
//        String aliasPathInZip = aliasPath.startsWith("/") ? aliasPath.substring(1) : aliasPath;
//        String aliasContent = "tenantAliasTree: []";
//
//        String inputZip = createInputZip(Map.of(
//            aliasPathInZip, aliasContent,
//            "config/tenants/MAIN/testapp/lep/Script.groovy", "println 'hello'"
//        ));
//
//        Path workDir = tempDir.resolve("work");
//        workDir.toFile().mkdirs();
//
//        runExecute(inputZip, workDir.toString());
//
//        verify(tenantAliasService).onRefresh(aliasContent);
//    }
//
//    @Test
//    void execute_noAliasConfig_onRefreshNotCalled() throws IOException {
//        String inputZip = createInputZip(Map.of(
//            "config/tenants/MAIN/testapp/lep/Script.groovy", "println 'hello'"
//        ));
//
//        Path workDir = tempDir.resolve("work");
//        workDir.toFile().mkdirs();
//
//        runExecute(inputZip, workDir.toString());
//
//        verify(tenantAliasService, never()).onRefresh(anyString());
//    }
//
//    @Test
//    void execute_closesApplicationContext() throws IOException {
//        String inputZip = createInputZip(Map.of(
//            "config/tenants/MAIN/testapp/lep/Script.groovy", "println 'hello'"
//        ));
//
//        Path workDir = tempDir.resolve("work");
//        workDir.toFile().mkdirs();
//
//        runExecute(inputZip, workDir.toString());
//
//        verify(applicationContext).close();
//    }
//
//    // --- extractTenant tests ---
//
//    @Test
//    void extractTenant_validPath_returnsTenant() {
//        assertEquals("MAIN", LepCompiler.extractTenant("/config/tenants/MAIN/app/lep/Script.groovy"));
//    }
//
//    @Test
//    void extractTenant_differentTenant_returnsTenant() {
//        assertEquals("XM", LepCompiler.extractTenant("/config/tenants/XM/app/lep/Script.groovy"));
//    }
//
//    @Test
//    void extractTenant_shortPath_throwsException() {
//        assertThrows(IllegalArgumentException.class, () -> LepCompiler.extractTenant("/config/tenants"));
//    }
//
//    // --- resolveOutputPath tests ---
//
//    @Test
//    void resolveOutputPath_directory_appendsDefaultFilename() {
//        String dirPath = tempDir.toFile().getAbsolutePath();
//        String result = LepCompiler.resolveOutputPath(dirPath, APP_NAME);
//
//        assertTrue(result.endsWith(APP_NAME + "-compiled-lep.zip"));
//        assertTrue(result.startsWith(dirPath));
//    }
//
//    @Test
//    void resolveOutputPath_filePath_returnsAsIs() {
//        String filePath = tempDir.resolve("custom-output.zip").toString();
//        assertEquals(filePath, LepCompiler.resolveOutputPath(filePath, APP_NAME));
//    }
//
//    // --- helpers ---
//
//    private void runExecute(String inputZipPath, String outputPath) {
//        when(applicationContext.getBean(ApplicationNameProvider.class)).thenReturn(applicationNameProvider);
//        when(applicationContext.getBean(TenantAliasService.class)).thenReturn(tenantAliasService);
//        when(applicationNameProvider.getAppName()).thenReturn(APP_NAME);
//
//        new LepCompiler() {
//            @Override
//            protected ConfigurableApplicationContext createContext() {
//                return applicationContext;
//            }
//
//            @Override
//            protected void preCompileAllTenants(String appName,
//                                                ConfigurableApplicationContext ctx,
//                                                Map<String, List<XmLepConfigFile>> prepared,
//                                                Path workDir) {
//                // skip compilation in unit test
//            }
//        }.execute(new String[]{inputZipPath, outputPath});
//    }
//
//    private String createInputZip(Map<String, String> entries) throws IOException {
//        String zipPath = tempDir.resolve("input.zip").toString();
//        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
//            for (Map.Entry<String, String> entry : entries.entrySet()) {
//                zos.putNextEntry(new ZipEntry(entry.getKey()));
//                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
//                zos.closeEntry();
//            }
//        }
//        return zipPath;
//    }
//
//    private Map<String, String> readZipEntries(String zipPath) throws IOException {
//        Map<String, String> result = new LinkedHashMap<>();
//        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
//            ZipEntry entry;
//            while ((entry = zis.getNextEntry()) != null) {
//                if (!entry.isDirectory()) {
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    byte[] buffer = new byte[1024];
//                    int len;
//                    while ((len = zis.read(buffer)) > 0) {
//                        baos.write(buffer, 0, len);
//                    }
//                    result.put(entry.getName(), baos.toString(StandardCharsets.UTF_8));
//                }
//                zis.closeEntry();
//            }
//        }
//        return result;
//    }
//}
