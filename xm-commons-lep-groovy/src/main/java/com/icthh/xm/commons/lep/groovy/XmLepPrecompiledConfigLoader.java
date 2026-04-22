package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.XmLepConstants;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.api.XmLepPrecompiledConfig;
import com.icthh.xm.commons.lep.spring.LepRefreshService;
import com.icthh.xm.commons.lep.utils.XmLepUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.icthh.xm.commons.tenant.YamlMapperUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public class XmLepPrecompiledConfigLoader implements RefreshableConfiguration {

    private static final String TENANT = "tenant";

    private final LepRefreshService lepRefreshService;

    private final String configPath;
    private final Path pathToWorkingDirectory;

    private final AtomicInteger version = new AtomicInteger(0);
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = YamlMapperUtils.yamlDefaultMapper();

    private final Map<String, XmLepPrecompiledConfig> precompiledConfigsByTenant = new ConcurrentHashMap<>();

    public XmLepPrecompiledConfigLoader(LepRefreshService lepRefreshService,
                                        String appName,
                                        String pathToWorkingDirectory) {
        this.lepRefreshService = lepRefreshService;
        this.configPath = "/config/tenants/{tenant}/" + appName + "/lep.yml";
        this.pathToWorkingDirectory = Path.of(pathToWorkingDirectory);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = extractTenant(updatedKey);

            XmLepPrecompiledConfig precompiledConfig = readConfig(config);
            if (precompiledConfig != null && StringUtils.isNotBlank(precompiledConfig.getPathToPrecompiledLep())) {
                precompiledConfigsByTenant.put(tenant, precompiledConfig);
            } else {
                precompiledConfigsByTenant.remove(tenant);
            }
        } catch (Exception e) {
            log.error("Error update configuration by key: {}", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        if (precompiledConfigsByTenant.isEmpty()) {
            return;
        }

        Map<String, List<String>> tenantsByZipPath = precompiledConfigsByTenant.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> e.getValue().getPathToPrecompiledLep(),
                LinkedHashMap::new,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())
            ));

        tenantsByZipPath.forEach(this::fetchZip);
    }

    private String extractTenant(String path) {
        Map<String, String> variables = matcher.extractUriTemplateVariables(configPath, path);
        return variables.get(TENANT);
    }

    @SneakyThrows
    private XmLepPrecompiledConfig readConfig(String config) {
        return ymlMapper.readValue(config, XmLepPrecompiledConfig.class);
    }

    protected void fetchZip(String zipPath, List<String> tenants) {
        String fileHash = hashFile(zipPath);
        Path tempDir = pathToWorkingDirectory.resolve(fileHash);

        try {
            unzip(zipPath, tempDir);

            long currentVersion = version.incrementAndGet();
            Path versionedDir = pathToWorkingDirectory.resolve("v" + currentVersion);

            moveToVersionDirectory(zipPath, tenants, versionedDir, tempDir);

            Map<String, Map<String, XmLepConfigFile>> sourcesByTenant = readSourceFiles(versionedDir, tenants);

            if (sourcesByTenant.isEmpty()) {
                log.warn("No source files found for tenants {} in version {}", tenants, currentVersion);
                return;
            }

            log.info("Refreshing LEP engines for tenants {} from version {}", sourcesByTenant.keySet(), currentVersion);
            lepRefreshService.initOrRefresh(sourcesByTenant.keySet(), sourcesByTenant, versionedDir.toAbsolutePath().toString());

        } catch (IOException e) {
            log.error("Failed to process zip [{}] for tenants {}", zipPath, tenants, e);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private static void moveToVersionDirectory(String zipPath, List<String> tenants, Path versionedDir, Path tempDir) throws IOException {
        if (Files.exists(versionedDir)) {
            deleteDirectory(versionedDir);
        }
        Files.createDirectories(versionedDir);

        for (String tenant : tenants) {
            Path tenantSource = tempDir.resolve(tenant);
            if (!Files.exists(tenantSource)) {
                log.warn("Tenant [{}] not found in zip [{}]", tenant, zipPath);
                continue;
            }

            Path tenantTarget = versionedDir.resolve(tenant);
            FileUtils.moveDirectory(tenantSource.toFile(), tenantTarget.toFile());
        }
    }

    protected void unzip(String zipPath, Path targetDir) throws IOException {
        if (Files.exists(targetDir)) {
            log.info("Zip already unzipped at {}, skipping", targetDir);
            return;
        }

        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("Zip entry outside target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                }
            }
        }
    }

    protected Map<String, Map<String, XmLepConfigFile>> readSourceFiles(Path versionedDir,
                                                                        List<String> tenants) throws IOException {
        Map<String, Map<String, XmLepConfigFile>> scriptsByTenant = new HashMap<>();

        for (String tenant : tenants) {
            Path sourcesDir = versionedDir.resolve(tenant).resolve(XmLepConstants.SCRIPT_SOURCES_DIR);
            if (!Files.exists(sourcesDir)) {
                log.warn("No sources directory for tenant [{}] in {}", tenant, versionedDir);
                continue;
            }

            Collection<File> files = FileUtils.listFiles(sourcesDir.toFile(), null, true);
            files.forEach(file -> addSourceScriptByTenant(tenant, file, sourcesDir, scriptsByTenant));
        }

        return scriptsByTenant;
    }

    @SneakyThrows
    private static void addSourceScriptByTenant(String tenant, File file, Path sourcesDir, Map<String, Map<String, XmLepConfigFile>> scriptsByTenant) {
        String path = "/" + sourcesDir.relativize(file.toPath()).toString().replace('\\', '/');
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        XmLepUtils.addToScriptsByTenant(tenant, scriptsByTenant, path, content);
    }

    @SneakyThrows
    protected String hashFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return DigestUtils.sha256Hex(fis);
        }
    }

    @SneakyThrows
    private static void deleteDirectory(Path dir) {
        FileUtils.deleteDirectory(dir.toFile());
    }
}
