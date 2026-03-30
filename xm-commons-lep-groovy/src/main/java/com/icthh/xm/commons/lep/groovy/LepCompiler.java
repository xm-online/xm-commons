package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.config.LepCompilerConfiguration;
import com.icthh.xm.commons.lep.groovy.storage.LepStorageFactory;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import com.icthh.xm.commons.lep.utils.XmLepUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessorApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.AntPathMatcher;

import static com.icthh.xm.commons.lep.LepPathResolver.ENV_COMMONS;

@Slf4j
public class LepCompiler {

    private static final String LEP_PATH_PATTERN = "/config/tenants/*/%s/lep/**";
    private static final String ENV_LEP_PATH_PATTERN = "/config/tenants/" + ENV_COMMONS + "/lep/**";

    public static void main(String[] args) {
        new LepCompiler().execute(args);
    }

    protected void execute(String[] args) {
        if (args.length != 2) {
            log.error("Usage: java LepCompiler <path-to-config-export.zip> <path-to-output>");
            System.exit(1);
            return;
        }

        String inputZipPath = args[0];
        String outputPath = args[1];

        try (ConfigurableApplicationContext context = createContext()) {
            String appName = context.getBean(ApplicationNameProvider.class).getAppName();
            TenantAliasService tenantAliasService = context.getBean(TenantAliasService.class);

            String resolvedOutput = resolveOutputPath(outputPath, appName);
            log.info("Processing LEPs for [{}] from [{}] to [{}]", appName, outputPath, resolvedOutput);

            Map<String, Map<String, XmLepConfigFile>> lepsByTenant = readLepFilesFromZip(tenantAliasService, inputZipPath, appName);
            log.info("Found LEP files for {} tenants", lepsByTenant.size());

            Map<String, List<XmLepConfigFile>> preparedConfigs = XmLepUtils.prepareConfigs(lepsByTenant.keySet(), lepsByTenant);

            Path workDir = Path.of(outputPath);

            writeSources(preparedConfigs, workDir);

            preCompileAllTenants(appName, context, preparedConfigs, workDir);

            zipDirectory(workDir, resolvedOutput);

            log.info("LEP compilation completed: {}", resolvedOutput);
        } catch (Exception e) {
            log.error("LEP compilation failed", e);
            System.exit(1);
        }
    }

    protected static Map<String, Map<String, XmLepConfigFile>> readLepFilesFromZip(TenantAliasService tenantAliasService,
                                                                                   String zipPath,
                                                                                   String msName) throws IOException {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        String tenantPattern = String.format(LEP_PATH_PATTERN, msName);

        Map<String, Map<String, XmLepConfigFile>> result = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            processLepFiles(tenantAliasService, zis, pathMatcher, tenantPattern, result);
        }

        return result;
    }

    private static void processLepFiles(TenantAliasService tenantAliasService, ZipInputStream zis, AntPathMatcher pathMatcher, String tenantPattern, Map<String, Map<String, XmLepConfigFile>> result) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }

            String path = "/" + entry.getName();
            if (TenantAliasService.TENANT_ALIAS_CONFIG.equals(path)) {
                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                tenantAliasService.onRefresh(content);
            }

            if (pathMatcher.match(tenantPattern, path) || pathMatcher.match(ENV_LEP_PATH_PATTERN, path)) {
                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                String tenant = extractTenant(path);
                XmLepUtils.addToScriptsByTenant(tenant, result, path, content);
            }
        }
    }

    protected static String extractTenant(String path) {
        String[] segments = path.split("/");
        if (segments.length < 4) {
            throw new IllegalArgumentException("Cannot extract tenant from path: " + path);
        }
        return segments[3];
    }

    protected static String resolveOutputPath(String path, String msName) {
        File file = new File(path);
        return file.isDirectory()
            ? new File(file, msName + "-compiled-lep.zip").getAbsolutePath()
            : path;
    }

    protected static void writeSources(Map<String, List<XmLepConfigFile>> prepared, Path workDir) throws IOException {
        for (Map.Entry<String, List<XmLepConfigFile>> entry : prepared.entrySet()) {
            String tenant = entry.getKey();
            for (XmLepConfigFile lep : entry.getValue()) {
                String lepPath = lep.getPath().startsWith("/")
                    ? lep.getPath().substring(1)
                    : lep.getPath();
                Path target = workDir.resolve(tenant).resolve("sources").resolve(lepPath);
                Files.createDirectories(target.getParent());
                Files.writeString(target, lep.readContent(), StandardCharsets.UTF_8);
            }
        }
    }

    protected ConfigurableApplicationContext createContext() {
        SpringApplication app = new SpringApplication(LepCompilerConfiguration.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.setAdditionalProfiles("export");
        app.setListeners(List.of(new EnvironmentPostProcessorApplicationListener()));
        return app.run();
    }

    protected void preCompileAllTenants(String appName,
                                        ConfigurableApplicationContext ctx,
                                        Map<String, List<XmLepConfigFile>> prepared,
                                        Path workDir) throws IOException {
        LepStorageFactory storageFactory = ctx.getBean(LepStorageFactory.class);
        GroovyEngineCreationStrategy strategy = ctx.getBean(GroovyEngineCreationStrategy.class);
        LoggingWrapper loggingWrapper = ctx.getBean(LoggingWrapper.class);
        LepPathResolver pathResolver = ctx.getBean(LepPathResolver.class);
        GroovyFileParser fileParser = ctx.getBean(GroovyFileParser.class);

        for (Map.Entry<String, List<XmLepConfigFile>> entry : prepared.entrySet()) {
            String tenant = entry.getKey();
            Path compiledDir = workDir.resolve(tenant).resolve("compiled");
            Files.createDirectories(compiledDir);

            log.info("Compiling LEPs for tenant [{}] → {}", tenant, compiledDir);

            GroovyLepEngineFactory factory = new GroovyLepEngineFactory(
                appName,
                storageFactory,
                strategy,
                loggingWrapper,
                pathResolver,
                fileParser,
                Set.of(),
                true,
                true,
                compiledDir.toAbsolutePath().toString()
            );

            factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

            factory.createLepEngine(tenant, entry.getValue());
        }
    }

    protected static void zipDirectory(Path sourceDir, String outputPath) throws IOException {
        Path outputFile = Paths.get(outputPath).toAbsolutePath().normalize();

        try (
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath));
            Stream<Path> paths = Files.walk(sourceDir);
            Stream<Path> entries = Files.list(sourceDir)
        ) {
            paths.forEach((file) -> createZipEntry(sourceDir, file, outputFile, zos));

            entries.filter(Files::isDirectory).forEach(LepCompiler::deleteDirectory);
        }
    }

    private static void createZipEntry(Path sourceDir, Path file, Path outputFile, ZipOutputStream zos) {
        try {
            if (file.toAbsolutePath().normalize().equals(outputFile)) {
                return;
            }

            String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
            if (entryName.isEmpty() || entryName.endsWith(".zip")) {
                return;
            }

            if (Files.isDirectory(file)) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();
            } else if (Files.isRegularFile(file)) {
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(Files.readAllBytes(file));
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteDirectory(Path dir) {
        try {
            FileUtils.deleteDirectory(dir.toFile());
        } catch (IOException e) {
            log.warn("Failed to delete directory: {}", dir, e);
        }
    }
}
