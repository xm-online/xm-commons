package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.xm.lep.groovy.LepScriptResourceConnector;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.beans.factory.BeanClassLoaderAware;

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
        return new LepScriptResourceConnector(managerService,
                new ClassNameLepResourceKeyMapper(resourceKeyMapper, appName, managerService, resourceService, tenantAliasService));
    }

    @Override
    protected void initGroovyScriptEngine(GroovyScriptEngine engine, ContextsHolder contextsHolder) {
        super.initGroovyScriptEngine(engine, contextsHolder);
        this.engine = engine;
    }

    @Override
    public void clearCache() {
        GroovyScriptEngine engine = this.engine;
        if(engine != null) {
            GroovyClassLoader groovyClassLoader = engine.getGroovyClassLoader();
            groovyClassLoader.setShouldRecompile(true);
            groovyClassLoader.clearCache();
        }
    }
}
