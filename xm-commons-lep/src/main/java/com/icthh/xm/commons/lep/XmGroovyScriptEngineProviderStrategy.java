package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.xm.lep.groovy.LepResourceKeyURLConnection;
import com.icthh.xm.lep.groovy.LepScriptResourceConnector;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import lombok.SneakyThrows;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.beans.factory.BeanClassLoaderAware;

import java.lang.reflect.Field;
import java.net.URLConnection;
import java.util.Map;

/**
 * The {@link XmGroovyScriptEngineProviderStrategy} class.
 */
public class XmGroovyScriptEngineProviderStrategy extends LazyGroovyScriptEngineProviderStrategy
    implements BeanClassLoaderAware, CacheableLepEngine {

    private final ScriptNameLepResourceKeyMapper resourceKeyMapper;
    private final String appName;
    private final LepResourceService resourceService;
    private final TenantAliasService tenantAliasService;
    private volatile GroovyScriptEngine engine;

    private ClassLoader springClassLoader;

    public XmGroovyScriptEngineProviderStrategy(ScriptNameLepResourceKeyMapper resourceKeyMapper,
                                                String appName,
                                                LepResourceService resourceService,
                                                TenantAliasService tenantAliasService) {
        super(resourceKeyMapper);
        this.resourceKeyMapper = resourceKeyMapper;
        this.appName = appName;
        this.resourceService = resourceService;
        this.tenantAliasService = tenantAliasService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    protected ResourceConnector buildResourceConnector(LepManagerService managerService) {
        ClassNameLepResourceKeyMapper mapper = new ClassNameLepResourceKeyMapper(resourceKeyMapper, appName, managerService, resourceService, tenantAliasService);
        return new LepScriptResourceConnector(managerService, mapper) {
            @Override
            public URLConnection getResourceConnection(String scriptName) throws ResourceException {
                try {
                    return new XmLepResourceKeyURLConnection(mapper.map(scriptName),
                            managerService.getResourceService(),
                            managerService);
                } catch (Exception e) {
                    throw new ResourceException("Error while building "
                            + LepResourceKeyURLConnection.class.getSimpleName()
                            + ": " + e.getMessage(), e);
                }
            }
        };
    }

    @Override
    protected void initGroovyScriptEngine(GroovyScriptEngine engine, ContextsHolder contextsHolder) {
        super.initGroovyScriptEngine(engine, contextsHolder);
        this.engine = engine;
    }

    @Override
    @SneakyThrows
    public void clearCache() {
        GroovyScriptEngine engine = this.engine;
        if(engine != null) {
            GroovyClassLoader groovyClassLoader = engine.getGroovyClassLoader();
            groovyClassLoader.setShouldRecompile(true);
            groovyClassLoader.clearCache();
        }
    }
}
