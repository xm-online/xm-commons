package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata = new ConcurrentHashMap<>();

    public GroovyLepEngine(String tenant,
                           LepStorage leps,
                           LoggingWrapper loggingWrapper,
                           ClassLoader classLoader,
                           Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                           LepResourceConnector lepResourceConnector,
                           LepPathResolver lepPathResolver,
                           boolean isWarmupEnabled) {
        this.tenant = tenant;
        this.leps = leps;
        this.loggingWrapper = loggingWrapper;
        this.gse = buildGroovyEngine(classLoader, lepResourceConnector);
        this.lepMetadata.putAll(lepMetadata);
        this.lepPathResolver = lepPathResolver;
        this.tenantCommonsFolders = lepPathResolver.getLepCommonsPaths(tenant);
        if (isWarmupEnabled) {
            warmupScripts();
        } else {
            log.warn("Warmup lep script for tenant {} disabled", tenant);
        }
    }

    protected GroovyScriptEngine buildGroovyEngine(ClassLoader classLoader, LepResourceConnector lepResourceConnector) {
        var gse = new GroovyScriptEngine(lepResourceConnector, classLoader);
        CompilerConfiguration config = gse.getConfig();
        config.setRecompileGroovySource(true);
        config.setMinimumRecompilationInterval(50);
        gse.setConfig(config);
        gse.getGroovyClassLoader().setShouldRecompile(true);
        return gse;
    }

    private void warmupScripts() {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("Start warmup lep scripts");
        this.leps.forEach(lep -> {
            try {
                // skip classes that can't be are entry point, and will be compiled on import from other script
                if (!isCommonsClass(lep.getPath())) {
                    if (lepMetadata.containsKey(lep.metadataKey()) && lepMetadata.get(lep.metadataKey()).isScript()) {
                        StopWatch warmUpTime = StopWatch.createStarted();
                        log.trace("START | Warmup lep {}", lep.getPath());
                        Class<?> scriptClass = gse.loadScriptByName(LEP_PREFIX + lep.getPath());
                        InvokerHelper.getMetaClass(scriptClass); // build metaclass
                        log.trace("STOP | Warmup lep {}, time: {} ms", lep.getPath(), warmUpTime.getTime(MILLISECONDS));
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

}
