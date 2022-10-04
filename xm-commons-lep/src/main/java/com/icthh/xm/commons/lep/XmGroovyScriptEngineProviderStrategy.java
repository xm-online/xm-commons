package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.storage.TenantScriptPathResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.groovy.GroovyScriptEngineProviderStrategy;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import groovy.util.GroovyScriptEngine;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.AntPathMatcher;

import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_ENVIRONMENT;
import static com.icthh.xm.commons.lep.TenantScriptStorage.URL_PREFIX_COMMONS_TENANT;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.commonsLepScriptsAntPathPattern;
import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.environmentLepScriptsAntPathPattern;

/**
 * The {@link XmGroovyScriptEngineProviderStrategy} class.
 */
@Slf4j
@RequiredArgsConstructor
public class XmGroovyScriptEngineProviderStrategy implements GroovyScriptEngineProviderStrategy,
        BeanClassLoaderAware, CacheableLepEngine {

    private final ScriptNameLepResourceKeyMapper resourceKeyMapper;
    private final String appName;
    private final TenantAliasService tenantAliasService;
    private final TenantScriptPathResolver tenantScriptPathResolver;
    private final TenantListRepository tenantListRepository;
    private volatile GroovyScriptEngine engine;
    private volatile LepManagerService managerService;

    private ClassLoader springClassLoader;

    /**
     * {@inheritDoc}
     */
    protected ClassLoader getParentClassLoader() {
        return springClassLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.springClassLoader = classLoader;
    }

    @Override
    public GroovyScriptEngine getEngine(LepManagerService managerService) {
        GroovyScriptEngine engine = this.engine;
        if (engine == null) {
            synchronized (this) {
                engine = this.engine;
                if (engine == null) {
                    this.engine = engine = buildGroovyScriptEngine(managerService, Map.of());
                    this.managerService = managerService;
                }
            }
        }
        return engine;
    }

    private GroovyScriptEngine buildGroovyScriptEngine(LepManagerService managerService, Map<String, XmLepScriptResource> leps) {
        final ClassLoader parentClassLoader = getParentClassLoader();
        var warmupManagerService = new WarmupLepManagerService(appName, tenantScriptPathResolver, managerService, leps);
        var mapper = buildMapper(warmupManagerService);
        var rc = new XmLepScriptResourceConnector(warmupManagerService, mapper);

        GroovyScriptEngine engine = (parentClassLoader == null)
                ? new GroovyScriptEngine(rc)
                : new GroovyScriptEngine(rc, parentClassLoader);

        leps.keySet().stream()
                .flatMap(it -> pathToLepPath(it).stream())
                .forEach(scriptName -> loadScriptByName(engine, scriptName));

        warmupManagerService.warmupFinished();
        return engine;
    }

    private ClassNameLepResourceKeyMapper buildMapper(LepManagerService managerService) {
        return new ClassNameLepResourceKeyMapper(resourceKeyMapper, appName, managerService, tenantAliasService);
    }

    @Override
    public void clearCache(Map<String, XmLepScriptResource> leps) {
        LepManagerService managerService = this.managerService;
        if (managerService != null) {
            this.engine = buildGroovyScriptEngine(managerService, leps);
        } else {
            log.warn("GroovyScriptEngine was not inited");
        }
    }

    private static void loadScriptByName(GroovyScriptEngine engine, String scriptName) {
        try {
            engine.loadScriptByName("lep://" + scriptName);
        } catch (Throwable e) {
            log.error("Error during load script {}", scriptName, e);
        }
    }


    public List<String> pathToLepPath(String path) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String pattern = "/config/tenants/{tenantKey}/" + appName + "/lep/**";

        if (antPathMatcher.match(environmentLepScriptsAntPathPattern, path)) {
            String lepPath = antPathMatcher.extractPathWithinPattern(environmentLepScriptsAntPathPattern, path);
            String suffix = URL_PREFIX_COMMONS_ENVIRONMENT + "/" + lepPath;
            return tenantListRepository.getTenants().stream().map(tenant -> tenant.toUpperCase() + suffix).toList();

        } else if (antPathMatcher.match(commonsLepScriptsAntPathPattern, path)) {
            String lepPath = antPathMatcher.extractPathWithinPattern(commonsLepScriptsAntPathPattern, path);
            Map<String, String> variables = antPathMatcher.extractUriTemplateVariables(commonsLepScriptsAntPathPattern, path);
            return List.of(variables.get("tenantKey") + URL_PREFIX_COMMONS_TENANT + "/" + lepPath);

        } else if (antPathMatcher.match(pattern, path)) {
            String lepPath = antPathMatcher.extractPathWithinPattern(pattern, path);
            Map<String, String> variables = antPathMatcher.extractUriTemplateVariables(pattern, path);
            return List.of(variables.get("tenantKey") + "/" + lepPath);
        }

        return List.of();
    }

}
