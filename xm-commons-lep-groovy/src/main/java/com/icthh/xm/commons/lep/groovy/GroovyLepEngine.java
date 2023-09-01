package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Slf4j
public class GroovyLepEngine extends LepEngine {

    private final String appName;
    private final String tenant;
    private final LepStorage leps;
    private final GroovyScriptEngine gse;
    private final GroovyScriptEngine engineForCompile;

    public GroovyLepEngine(String appName, String tenant, LepStorage leps,
                           TenantAliasService tenantAliasService, ClassLoader classLoader) {
        this.appName = appName;
        this.tenant = tenant;
        this.leps = leps;
        LepResourceConnector lepResourceConnector = new LepResourceConnector(tenant, appName, tenantAliasService, leps);
        this.gse = new GroovyScriptEngine(lepResourceConnector, classLoader);
        this.engineForCompile = new GroovyScriptEngine(lepResourceConnector, classLoader);
        warmupScripts();
    }

    private void warmupScripts() {
        this.leps.forEach(lep -> {
            try {
                Class<?> scriptClass = engineForCompile.loadScriptByName("lep://" + lep.getPath());
                if (Script.class.isAssignableFrom(scriptClass)) {
                    gse.createScript("lep://" + lep.getPath(), new Binding());
                }
            } catch (Throwable e) {
                log.error("Error create script {}", lep.getPath(), e);
            }
        });
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
        getExistingKey(beforeKeys).ifPresent(key -> executeLep(key, lepContext));

        List<String> mainKeys = getMainKeys(lepKey);
        Optional<String> mainKey = getExistingKey(mainKeys);
        if (mainKey.isPresent()) {
            return executeLep(mainKey.get(), lepContext);
        } else {
            return lepContext.lep.proceed();
        }
    }

    @SneakyThrows
    private Object executeLep(String key, BaseLepContext lepContext) {
        try {
            return gse.run("lep://" + key, new Binding(Map.of("lepContext", lepContext)));
        } catch (Throwable e) {
            log.error("Error run lep {}", key, e);
            throw e;
        }
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
        lepPath = tenant + "/" + appName + "/lep/" + lepPath;
        return lepPath;
    }

    private static String translateToLepConvention(String xmEntitySpecKey) {
        return xmEntitySpecKey.replaceAll("-", "_").replaceAll("\\.", "\\$");
    }

}
