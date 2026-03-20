package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;

import static com.icthh.xm.commons.lep.groovy.storage.LepStorage.FILE_EXTENSION;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class GroovyLepEngine extends LepEngine {

    public static final String LEP_PREFIX = "lep://";
    public static final String COMMONS_SCRIPT = "/Commons$$";
    private final String tenant;
    private final LepStorage leps;
    private final GroovyScriptEngine gse;
    private final LoggingWrapper loggingWrapper;
    private final LepPathResolver lepPathResolver;
    private final List<String> tenantCommonsFolders;
    private final Boolean useDirectoryCompiledSources;

    private final Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata = new ConcurrentHashMap<>();

    public GroovyLepEngine(String tenant,
                           LepStorage leps,
                           LoggingWrapper loggingWrapper,
                           ClassLoader classLoader,
                           Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                           LepResourceConnector lepResourceConnector,
                           LepPathResolver lepPathResolver,
                           boolean isWarmupEnabled,
                           boolean useDirectoryCompiledSources,
                           String targetDirectoryPath) {
        this.tenant = tenant;
        this.leps = leps;
        this.loggingWrapper = loggingWrapper;
        this.useDirectoryCompiledSources = useDirectoryCompiledSources;
        this.gse = buildGroovyEngine(classLoader, lepResourceConnector, targetDirectoryPath);
        this.lepMetadata.putAll(lepMetadata);
        this.lepPathResolver = lepPathResolver;
        this.tenantCommonsFolders = lepPathResolver.getLepCommonsPaths(tenant);
        if (isWarmupEnabled) {
            warmupScripts();
        } else {
            log.warn("Warmup lep script for tenant {} disabled", tenant);
        }
    }

    protected GroovyScriptEngine buildGroovyEngine(ClassLoader classLoader,
                                                   LepResourceConnector lepResourceConnector,
                                                   String targetDirectoryPath) {
        GroovyScriptEngine gse;
        CompilerConfiguration config;
        if (useDirectoryCompiledSources) {
            File targetDir = new File(targetDirectoryPath);
            gse = new GroovyScriptEngine(lepResourceConnector, buildCachingClassLoader(classLoader, targetDir));
            config = gse.getConfig();
            config.setTargetDirectory(targetDir);
        } else {
            gse = new GroovyScriptEngine(lepResourceConnector, classLoader);
            config = gse.getConfig();
        }

        config.setRecompileGroovySource(true);
        config.setMinimumRecompilationInterval(50);
        gse.setConfig(config);
        gse.getGroovyClassLoader().setShouldRecompile(true);
        return gse;
    }

    private void warmupScripts() {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("Start warmup lep scripts");
        GroovyClassLoader groovyClassLoader = gse.getGroovyClassLoader();
        this.leps.forEach(lep -> {
            try {
                // skip classes that can't be are entry point, and will be compiled on import from other script
                if (!isCommonsClass(lep.getPath())) {
                    if (lepMetadata.containsKey(lep.metadataKey()) && lepMetadata.get(lep.metadataKey()).isScript()) {
                        StopWatch warmUpTime = StopWatch.createStarted();
                        log.info("START | Warmup lep {}", lep.getPath());

                        if (useDirectoryCompiledSources) {
                            String className = toGroovyClassName(lep);
                            try {
                                Class<?> loadClass = groovyClassLoader.loadClass(className, true, false);
                                InvokerHelper.getMetaClass(loadClass);
                                log.info("CACHE | Lep compiled sources {}, time {} ms",
                                    lep.getPath(),
                                    warmUpTime.getTime(MILLISECONDS));
                                return;
                            } catch (ClassNotFoundException ignored) {}
                        }

                        Class<?> scriptClass = gse.loadScriptByName(LEP_PREFIX + lep.getPath());
                        InvokerHelper.getMetaClass(scriptClass); // build metaclass
                        log.info("STOP | Warmup lep {}, time: {} ms", lep.getPath(), warmUpTime.getTime(MILLISECONDS));
                    }
                }
            } catch (Throwable e) {
                log.error("Error create script {}", lep.getPath(), e);
            }
        });
        log.info("Stop warm-up LEP scripts, time = {} ms, ", stopWatch.getTime(MILLISECONDS));
    }

    @Override
    public boolean isExists(LepKey lepKey) {
        List<String> before = getBeforeKeys(lepKey);
        List<String> main = getMainKeys(lepKey);
        return getExistingKey(before).isPresent() || getExistingKey(main).isPresent();
    }

    @Override
    @SneakyThrows
    public Object invoke(LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext) {
        List<String> beforeKeys = getBeforeKeys(lepKey);
        getExistingKey(beforeKeys).ifPresent(key -> executeLep(key, lepKey, lepMethod, lepContext));

        List<String> mainKeys = getMainKeys(lepKey);
        Optional<String> mainKey = getExistingKey(mainKeys);
        if (mainKey.isPresent()) {
            return executeLep(mainKey.get(), lepKey, lepMethod, lepContext);
        } else {
            return lepContext.lep.proceed();
        }
    }

    @SneakyThrows
    private Object executeLep(String key, LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext) {
        String scriptName = LEP_PREFIX + key + FILE_EXTENSION;
        if (useDirectoryCompiledSources) {
            String className = toGroovyClassName(leps.getByPath(key));
            return InvokerHelper.createScript(
                gse.getGroovyClassLoader().loadClass(className, true, false),  new Binding(new HashMap<>(Map.of("lepContext", lepContext)))
            ).run();
        }
        return loggingWrapper.doWithLogs(lepMethod, scriptName, lepKey, () ->
            // map HAVE TO be mutable!
            gse.run(LEP_PREFIX + key, new Binding(new HashMap<>(Map.of("lepContext", lepContext))))
        );
    }

    private List<String> getMainKeys(LepKey lepKey) {
        String lepPath = lepPathResolver.getLepPath(lepKey, tenant);
        String legacyLepPath = lepPathResolver.getLegacyLepPath(lepKey, tenant);
        return List.of(
            legacyLepPath + "$$tenant",
            legacyLepPath + "$$around",
            lepPath + "$$tenant",
            lepPath + "$$around",
            legacyLepPath,
            lepPath
        );
    }

    private List<String> getBeforeKeys(LepKey lepKey) {
        String lepPath = lepPathResolver.getLepPath(lepKey, tenant);
        String legacyLepPath = lepPathResolver.getLegacyLepPath(lepKey, tenant);
        return List.of(
            legacyLepPath + "$$before",
            lepPath + "$$before"
        );
    }

    private Optional<String> getExistingKey(List<String> keys) {
        return keys.stream().filter(leps::isExists).findFirst();
    }

    private boolean isCommonsClass(String path) {
        return !path.contains(COMMONS_SCRIPT) && tenantCommonsFolders.stream().anyMatch(path::startsWith);
    }

    private String toGroovyClassName(XmLepConfigFile lep) {
        String fileName = extractFileName(lep.getPath());
        String className = fileName.replace("$", "_");
        String packageName = extractPackageName(readString(lep));

        return packageName != null
            ? packageName + "." + className
            : className;
    }

    private String extractFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @SneakyThrows
    private URLClassLoader buildCachingClassLoader(ClassLoader parent, File targetDir) {
        return new URLClassLoader(new URL[]{targetDir.toURI().toURL()}, parent);
    }

    @SneakyThrows
    private String readString(XmLepConfigFile value) {
        return IOUtils.toString(value.getContentStream().getInputStream(), StandardCharsets.UTF_8);
    }

    private String extractPackageName(String text) {
        var matcher = Pattern.compile("^package\\s+([\\w.]+);?", Pattern.MULTILINE).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

}
