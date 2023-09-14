package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.icthh.xm.commons.lep.groovy.LepResourceConnector.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.groovy.LepResourceConnector.URL_PREFIX_COMMONS_TENANT;
import static com.icthh.xm.commons.lep.groovy.storage.LepStorage.FILE_EXTENSION;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Slf4j
public class GroovyLepEngine extends LepEngine {

    public static final String LEP_PREFIX = "lep://";
    private final String appName;
    private final String tenant;
    private final LepStorage leps;
    private final GroovyScriptEngine gse;
    private final GroovyScriptEngine engineForCompile;
    private final LoggingWrapper loggingWrapper;

    public GroovyLepEngine(String appName,
                           String tenant,
                           LepStorage leps,
                           LoggingWrapper loggingWrapper,
                           ClassLoader classLoader,
                           LepResourceConnector lepResourceConnector,
                           boolean isWarmupEnabled) {
        this.appName = appName;
        this.tenant = tenant;
        this.leps = leps;
        this.loggingWrapper = loggingWrapper;
        this.gse = buildGroovyEngine(classLoader, lepResourceConnector);
        this.engineForCompile = new GroovyScriptEngine(lepResourceConnector, classLoader);
        if (isWarmupEnabled) {
            warmupScripts();
        } else {
            log.warn("Warmup lep script disabled");
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
                Class<?> scriptClass = engineForCompile.loadScriptByName(LEP_PREFIX + lep.getPath());
                if (Script.class.isAssignableFrom(scriptClass)) {
                    engineForCompile.loadScriptByName(LEP_PREFIX + lep.getPath());
                }
            } catch (Throwable e) {
                log.error("Error create script {}", lep.getPath(), e);
            }
        });
        log.info("End warmup lep scripts | time {}ms", stopWatch.getTime(MILLISECONDS));
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
            gse.run(LEP_PREFIX + key, new Binding(Map.of("lepContext", lepContext)))
        );
    }

    private List<String> getMainKeys(LepKey lepKey) {
        String lepPath = getLepPath(lepKey);
        String legacyLepPath = getLegacyLepPath(lepKey);
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
        String lepPath = getLepPath(lepKey);
        String legacyLepPath = getLegacyLepPath(lepKey);
        return List.of(
            legacyLepPath + "$$before",
            lepPath + "$$before"
        );
    }

    private Optional<String> getExistingKey(List<String> keys) {
        return keys.stream().filter(leps::isExists).findFirst();
    }

    private String getLepPath(LepKey lepKey) {
        return buildLepPath(lepKey, identity());
    }

    private String getLegacyLepPath(LepKey lepKey) {
        return buildLepPath(lepKey, GroovyLepEngine::translateToLepConvention);
    }

    private String buildLepPath(LepKey lepKey, Function<String, String> segmentMapper) {
        String lepPath = lepKey.getBaseKey();
        List<String> segments = lepKey.getSegments();
        if (StringUtils.isNotBlank(lepKey.getGroup())) {
            lepPath = lepKey.getGroup().replace(".", "/") + "/" + lepKey.getBaseKey();
        }
        if (isNotEmpty(segments)) {
            segments = segments.stream().map(segmentMapper).collect(toList());
            lepPath = lepPath + "$$" + StringUtils.join(segments, "$$");
        }

        if (lepPath.startsWith(URL_PREFIX_COMMONS_ENVIRONMENT)) {
            lepPath = "commons/lep" + lepPath.substring(URL_PREFIX_COMMONS_ENVIRONMENT.length());
        } else if (lepPath.startsWith(URL_PREFIX_COMMONS_TENANT)) {
            lepPath = tenant + "/commons/lep" + lepPath.substring(URL_PREFIX_COMMONS_TENANT.length());
        } else {
            lepPath = tenant + "/" + appName + "/lep/" + lepPath;
        }
        return lepPath;
    }

    private static String translateToLepConvention(String xmEntitySpecKey) {
        return xmEntitySpecKey.replaceAll("-", "_").replaceAll("\\.", "\\$");
    }

}
